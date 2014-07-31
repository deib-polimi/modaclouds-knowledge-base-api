/**
 * Copyright 2014 deib-polimi
 * Contact: deib-polimi <marco.miglierina@polimi.it>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package it.polimi.modaclouds.monitoring.kb.api;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

public class FusekiKBAPI {

	public enum VariableTypes {
		SET, LIST, MAP;
	}

	private Logger logger = LoggerFactory.getLogger(FusekiKBAPI.class);

	private static final int DELETE_DATA_INDEX = 0;
	private static final int INSERT_DATA_INDEX = 1;
	private static final int DELETE_INDEX = 2;
	private static final int INSERT_INDEX = 3;
	private static final int WHERE_DELETE_INDEX = 4;
	private static final int WHERE_INSERT_INDEX = 5;

	/*Serialization: 
	- Set -> Bag (using the rdf:_1, rdf:_2... properties, these are rdf:li subproperties)
	- List -> Seq (using the rdf:_1, rdf:_2... properties, these are rdf:li subproperties)
	- Map -> custom construct, same format of the containers with custom type (map) 
				-> entity predicate map. 
				   map element [id, value] .  
				   map element [id, value] .
	 */

	/**
	 * Example url: http://localhost:3030/modaclouds/kb
	 * 
	 * @param knowledgeBaseURL
	 */
	public FusekiKBAPI(String knowledgeBaseURL) {
		this.knowledgeBaseURL = knowledgeBaseURL;
		dataAccessor = DatasetAccessorFactory
				.createHTTP(getKnowledgeBaseDataURL());
	}

	private String[] getEmptyQueryBody() {
		String[] queryBody = { "", "", "", "", "", "" };
		return queryBody;
	}

	private DatasetAccessor dataAccessor;

	private String knowledgeBaseURL;

	public String getKnowledgeBaseURL() {
		return knowledgeBaseURL;
	}

	public String getKnowledgeBaseDataURL() {
		return knowledgeBaseURL + "/data";
	}

	public String getKnowledgeBaseUpdateURL() {
		return knowledgeBaseURL + "/update";
	}

	public String getKnowledgeBaseQueryURL() {
		return knowledgeBaseURL + "/query";
	}

	public KBEntity getEntityByURI(URI uri) {
		// TODO avoid downloading the entire model
		Model model = dataAccessor.getModel();
		Resource r = ResourceFactory.createResource(uri.toString());
		KBEntity entity = EntityManager.toJava(r, model);
		return entity;
	}

	/**
	 * Adds the entity in the KB. If the entity already exists in the KB, the
	 * entity will be updated.
	 * 
	 * @param entity
	 */
	public void add(KBEntity entity) {
		String queryString = prepareAddQuery(entity);
		logger.info("Prepared add Query:\n" + queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	/**
	 * Adds the entities to the KB. If the entity already exists in the KB, the
	 * entity will be updated.
	 * 
	 * @param entities
	 */
	public void add(Set<? extends KBEntity> entities) {
		// TODO only one query should be done
		for (KBEntity e : entities) {
			add(e);
		}
	}

	public void deleteEntityByURI(URI uri) {
		// TODO delete all triples, not only this. Probably "uri ?p ?o" should
		// be ok
		String queryString = "DELETE WHERE { <" + uri + "> <"
				+ RDF.type.getURI() + "> ?o . }";
		logger.info("Prepared delete Query:\n" + queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	public void deleteAll(Set<? extends KBEntity> entities) {
		// TODO only one query should be done
		for (KBEntity entity : entities) {
			deleteEntityByURI(entity.getUri());
		}
	}

	public void addAll(Set<? extends KBEntity> entities) {
		// TODO only one query should be done
		for (KBEntity entity : entities) {
			add(entity);
		}
	}

	public <T extends KBEntity> Set<String> getURIs(Class<T> entityClass) {
		Set<String> uris = new HashSet<String>();
		// TODO avoid downloading the entire model
		Model model = dataAccessor.getModel();
		StmtIterator iter = model.listStatements(null, RDF.type,
				ResourceFactory.createResource(EntityManager
						.getKBClassURI(entityClass)));
		while (iter.hasNext()) {
			uris.add(iter.nextStatement().getSubject().getURI());
		}
		return uris;
	}

	private String prepareAddQuery(KBEntity entity) {
		Set<KBEntity> explored = new HashSet<KBEntity>();
		VariableGenerator varGen = new VariableGenerator();
		String[] queryBody = prepareAddQueryBody(entity, explored, varGen);
		String queryString = "PREFIX " + KBEntity.uriPrefix + ":<"
				+ KBEntity.uriBase + "> " + "DELETE DATA { "
				+ queryBody[DELETE_DATA_INDEX] + " } ; INSERT DATA { "
				+ queryBody[INSERT_DATA_INDEX] + " } ; DELETE { "
				+ queryBody[DELETE_INDEX] + " } WHERE { "
				+ queryBody[WHERE_DELETE_INDEX] + " } ; INSERT { "
				+ queryBody[INSERT_INDEX] + " } WHERE { "
				+ queryBody[WHERE_INSERT_INDEX] + " } ";
		return queryString;
	}

	private String[] prepareAddQueryBody(KBEntity entity,
			Set<KBEntity> explored, VariableGenerator varGen) {
		// explored was used to avoid loops when recursively exploring related
		// entities,
		// not useful anymore since we are not gonna recurevely add entities
		// anymore (using URIs)
		String[] queryBody = getEmptyQueryBody();
		if (!explored.contains(entity)) {
			explored.add(entity);
			Map<String, Object> properties = getProperties(entity);
			KBEntity entityFromKB = getEntityByURI(entity.getUri());
			if (entityFromKB == null) { // The entity does not exist in the KB, creating new instance...
				addNewEntity(entity, queryBody);
				for (String property : properties.keySet()) {
					Object value = properties.get(property);
					if (value != null) {
						addNewProperty(entity, property, value, queryBody);
					}
				}
			} else { // The entity already exists in the KB, updating...
				Map<String, Object> entityFromKBProperties = getProperties(entityFromKB);
				for (String property : properties.keySet()) {
					Object entityFromKBValue = entityFromKBProperties
							.get(property);
					Object value = properties.get(property);
					if (entityFromKBValue == null && value == null) {
						break;
					} else if (entityFromKBValue == null) {
						addNewProperty(entity, property, value, queryBody);
					} else if (value == null) {
						deleteProperty(entity, property, value, queryBody);
					} else if (!entityFromKBValue.equals(value)) { //add new control for List and Map and change the Set serialization
						deleteProperty(entity, property, entityFromKBValue,	queryBody);
						addNewProperty(entity, property, value, queryBody);
					}
				}
			}
		}
		return queryBody;
	}

	private void deleteProperty(KBEntity entity, String property, Object value, String[] queryBody) { // this should be
		if (value instanceof Set<?>) { 
			String[] temp = prepareDeletePropertyQueryBody(entity, property, value, VariableTypes.SET);
			concatBodies(queryBody, temp);
		} else if(value instanceof List<?>) { 
			String[] temp = prepareDeletePropertyQueryBody(entity, property, value, VariableTypes.LIST);
			concatBodies(queryBody, temp);
		} else if(value instanceof Map<?,?>) { 
			String[] temp = prepareDeletePropertyQueryBody(entity, property, value, VariableTypes.MAP);
			concatBodies(queryBody, temp);
		} else {
			String[] temp = prepareDeletePropertyQueryBody(entity, property, value, null);
			concatBodies(queryBody, temp);
		}
	}

	private String[] prepareDeletePropertyQueryBody(KBEntity subject, String property, Object object, VariableTypes varType) {

		String anonIdUri = getByPropertyValue(property, null).iterator().next().getUri().toString();

		String[] queryBody = getEmptyQueryBody();
		int i = 0;
		switch (varType) {
		case SET:
			Set<?> setObjects = (Set<?>) object;
			queryBody[DELETE_INDEX] += prepareLiteralTriple(subject.getUri().toString(), EntityManager.getKBPropertyURI(property).toString(), anonIdUri);
			queryBody[DELETE_INDEX] += prepareLiteralTriple(anonIdUri, RDF.type.toString(), RDF.Bag.toString());
			i = 0;
			for(Object obj : setObjects){
				queryBody[DELETE_INDEX] += prepareLiteralTriple(anonIdUri, RDF.li(i).toString(), obj.toString());
				i++;
			}		
			break;			
		case LIST:
			List<?> listObjects = (List<?>) object;
			queryBody[DELETE_INDEX] += prepareLiteralTriple(subject.getUri().toString(), EntityManager.getKBPropertyURI(property).toString(), anonIdUri);
			queryBody[DELETE_INDEX] += prepareLiteralTriple(anonIdUri, RDF.type.toString(), RDF.Seq.toString());
			i = 0;
			for(Object obj : listObjects){
				queryBody[DELETE_INDEX] += prepareLiteralTriple(anonIdUri, RDF.li(i).toString(), obj.toString());
				i++;
			}		
			break;		
		case MAP:
			Map<?,?> mapObjects = (Map<?,?>) object;
			queryBody[DELETE_INDEX] += prepareLiteralTriple(subject.getUri().toString(), EntityManager.getKBPropertyURI(property).toString(), anonIdUri);
			queryBody[DELETE_INDEX] += prepareLiteralTriple(anonIdUri, RDF.type.toString(), KBEntity.uriBase + "Map");
			i = 0;
			Set<?> keys = mapObjects.keySet();
			for(Object key : keys){
				queryBody[DELETE_INDEX] += prepareLiteralTriple(anonIdUri, RDF.li(i).toString(), mapObjects.get(key).toString());
				i++;
			}	
			break;
		default:
			queryBody[DELETE_INDEX] += prepareLiteralTriple(subject,property, object.toString());
			break;
		}
		return queryBody;
	}

	private void addNewProperty(KBEntity entity, String property, Object value, String[] queryBody) {
		if (value instanceof Set<?>) { 
			String[] temp = prepareAddPropertyQueryBody(entity, property, value, VariableTypes.SET);
			concatBodies(queryBody, temp);
		} else if(value instanceof List<?>) { 
			String[] temp = prepareAddPropertyQueryBody(entity, property, value, VariableTypes.LIST);
			concatBodies(queryBody, temp);
		} else if(value instanceof Map<?,?>) { 
			String[] temp = prepareAddPropertyQueryBody(entity, property, value, VariableTypes.MAP);
			concatBodies(queryBody, temp);
		} else {
			String[] temp = prepareAddPropertyQueryBody(entity, property, value, null);
			concatBodies(queryBody, temp);
		}
	}

	private void addNewEntity(KBEntity entity, String[] queryBody) {
		queryBody[INSERT_DATA_INDEX] += prepareAboutTriple(entity);
	}

	private String[] prepareAddPropertyQueryBody(KBEntity subject, String property, Object object, VariableTypes varType) {
		String[] queryBody = getEmptyQueryBody();
		int i = 0;
		if(varType == null){
			queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subject.getUri().toString(), property, object.toString());
		} else {
			switch (varType) {
			case SET:
				Set<?> setObjects = (Set<?>) object;
				String bagAnonId = new AnonId(UUID.randomUUID().toString()).toString();
				bagAnonId = KBEntity.uriBase + bagAnonId;
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subject.getUri().toString(), EntityManager.getKBPropertyURI(property).toString(), bagAnonId);
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(bagAnonId, RDF.type.toString(), RDF.Bag.toString());
				i = 0;
				for(Object obj : setObjects){
					queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(bagAnonId, RDF.li(i).toString(), obj.toString());
					i++;
				}			
				break;
			case LIST:
				List<?> listObjects = (List<?>) object;
				String seqAnonId = new AnonId(UUID.randomUUID().toString()).toString();
				seqAnonId = KBEntity.uriBase + seqAnonId;
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subject.getUri().toString(), EntityManager.getKBPropertyURI(property).toString(), seqAnonId);
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(seqAnonId, RDF.type.toString(), RDF.Seq.toString());
				i = 0;
				for(Object obj : listObjects){
					queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(seqAnonId, RDF.li(i).toString(), obj.toString());
					i++;
				}			
				break;
			case MAP:
				Map<?,?> mapObjects = (Map<?,?>) object;
				String mapAnonId = new AnonId(UUID.randomUUID().toString()).toString();
				mapAnonId = KBEntity.uriBase + mapAnonId;
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subject.getUri().toString(), EntityManager.getKBPropertyURI(property).toString(), mapAnonId);
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(mapAnonId, RDF.type.toString(), KBEntity.uriBase + "Map");
				i = 0;
				Set<?> keys = mapObjects.keySet();
				for(Object key : keys){
					queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(mapAnonId, RDF.li(i).toString(), mapObjects.get(key).toString());
					i++;
				}			
				break;
			default:
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subject.getUri().toString(), property, object.toString());
				break;
			}
		}
		return queryBody;
	}

	private String prepareLiteralTriple(KBEntity entity, String property, String literal) {
		return entity.getShortURI() + " "
				+ EntityManager.getKBPropertyURI(property) + " \"" + literal
				+ "\" . ";
	}

	private static String prepareLiteralTriple(String subject, String property, String object) {

		if(subject.contains("http://")){
			subject = "<" + subject + ">";
		} if(property.contains("http://")){
			property = "<" + property + ">";
		} else {
			property = "<" + EntityManager.getKBPropertyURI(property) + ">";
		} if(object.contains("http://")){
			object = "<" + object + ">";
		} else if(object.contains(":")){
			object = "" + object + "";
		} else {
			object = "\"" + object + "\"";
		}

		return subject + " " + property + " " + object + " . ";
	}

	private String prepareAboutTriple(KBEntity entity) {
		return entity.getShortURI() + " a " + entity.getShortClassURI() + " . ";
	}

	/**
	 * get all properties except for those in KBEntity class, which is meta data
	 * and not needed to be persisted as RDF triples
	 * 
	 * @param object
	 * @return
	 */
	private Map<String, Object> getProperties(Object object) {
		Map<String, Object> properties = new HashMap<String, Object>();
		try {
			properties = PropertyUtils.describe(object);
			PropertyDescriptor[] stopProperties = Introspector.getBeanInfo(
					KBEntity.class).getPropertyDescriptors();
			for (PropertyDescriptor stopProperty : stopProperties) {
				properties.remove(stopProperty.getName());
			}
		} catch (IllegalAccessException | InvocationTargetException
				| NoSuchMethodException | IntrospectionException e) {
			logger.error("Error while reading entity properties", e);
		}
		return properties;
	}

	// concatenate strings
	private void concatBodies(String[] body1, String[] body2) {
		assert body1.length == body2.length;
		for (int i = 0; i < body1.length; i++) {
			body1[i] += body2[i];
		}
	}

	public Set<KBEntity> getByPropertyValue(String property, String value) {
		Set<KBEntity> entities = new HashSet<KBEntity>();
		// TODO avoid downloading entire model
		Model model = dataAccessor.getModel();
		if(value != null) {
			StmtIterator iter = model.listStatements(null, ResourceFactory
					.createProperty(EntityManager.getKBPropertyURI(property)),
					value);
			while (iter.hasNext()) {
				Resource r = iter.nextStatement().getSubject();
				entities.add(EntityManager.toJava(r, model));
			}
		} else {
			StmtIterator iter = model.listStatements(null, ResourceFactory
					.createProperty(EntityManager.getKBPropertyURI(property)),
					(RDFNode) null);
			while (iter.hasNext()) {
				Resource r = iter.nextStatement().getSubject();
				entities.add(EntityManager.toJava(r, model));
			}
		}
		return entities;
	}

	@SuppressWarnings("unchecked")
	public <T extends KBEntity> Set<T> getAll(Class<T> entityClass) {
		Set<T> entities = new HashSet<T>();
		// TODO avoid downloading entire model
		Model model = dataAccessor.getModel();
		StmtIterator iter = model.listStatements(null, RDF.type,
				ResourceFactory.createResource(EntityManager
						.getKBClassURI(entityClass)));
		while (iter.hasNext()) {
			Resource r = iter.nextStatement().getSubject();
			entities.add((T) EntityManager.toJava(r, model));
		}
		return entities;
	}

	public <T extends KBEntity> Set<T> getAll(Class<T> subjectEntityClass,
			String property, String object) {
		// TODO TEMPORARY STUPID SOLUTION, not filtering by property object
		return getAll(subjectEntityClass);
	}

}
