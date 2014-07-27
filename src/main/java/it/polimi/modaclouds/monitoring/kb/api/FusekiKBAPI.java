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
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

public class FusekiKBAPI {

	private Logger logger = LoggerFactory.getLogger(FusekiKBAPI.class);

	private static final int DELETE_DATA_INDEX = 0;
	private static final int INSERT_DATA_INDEX = 1;
	private static final int DELETE_INDEX = 2;
	private static final int INSERT_INDEX = 3;
	private static final int WHERE_DELETE_INDEX = 4;
	private static final int WHERE_INSERT_INDEX = 5;

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

	@SuppressWarnings("unchecked")
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
						addNewProperty(entity, property, value, explored,
								queryBody, varGen);
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
						addNewProperty(entity, property, value, explored,
								queryBody, varGen);
					} else if (value == null) {
						deleteProperty(entity, property, value, queryBody,
								varGen);
					} else if (!entityFromKBValue.equals(value)) {
						if (entityFromKBValue instanceof Set) {
							for (Object o : (Set<Object>) entityFromKBValue) {
								logger.info(o.toString());
							}
							for (Object o : (Set<Object>) value) {
								logger.info(o.toString());
							}
						}
						deleteProperty(entity, property, entityFromKBValue,
								queryBody, varGen);
						addNewProperty(entity, property, value, explored,
								queryBody, varGen);
					}
				}
			}
		}
		return queryBody;
	}

	private void deleteProperty(KBEntity entity, String property, Object value,
			String[] queryBody, VariableGenerator varGen) { // this should be
															// recursive and
															// should consider
															// common children.
															// Just removes the
															// property for now.
		String oVar = varGen.getNew();
		queryBody[DELETE_INDEX] += prepareVarObjectTriple(entity, property,
				oVar);
	}

	private void addNewProperty(KBEntity entity, String property, Object value,
			Set<KBEntity> explored, String[] queryBody, VariableGenerator varGen) {
		if (value instanceof Set<?>) {
			Set<?> objects = (Set<?>) value;
			for (Object object : objects) {
				String[] temp = prepareAddPropertyQueryBody(entity, property, object,
						explored, varGen);
				concatBodies(queryBody, temp);
			}
		} else {
			String[] temp = prepareAddPropertyQueryBody(entity, property, value,
					explored, varGen);
			concatBodies(queryBody, temp);
		}
	}

	private void addNewEntity(KBEntity entity, String[] queryBody) {
		queryBody[INSERT_DATA_INDEX] += prepareAboutTriple(entity);
	}

	
	private String[] prepareAddPropertyQueryBody(KBEntity subject, String property,
			Object object, Set<KBEntity> explored, VariableGenerator varGen) {
		String[] queryBody = getEmptyQueryBody();
		if (object instanceof KBEntity) { //NOT HAPPENING ANYMORE WITH URIs
			KBEntity objectEntity = (KBEntity) object;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(subject, property,
					objectEntity);
			String[] temp = prepareAddQueryBody(objectEntity, explored, varGen);
			concatBodies(queryBody, temp);
		} else {
			queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subject,
					property, object.toString());
		}
		return queryBody;
	}

	private String prepareLiteralTriple(KBEntity entity, String property,
			String literal) {
		return entity.getShortURI() + " "
				+ EntityManager.getKBPropertyURI(property) + " \"" + literal
				+ "\" . ";
	}

	// not useful anymore, using URIs as objects
	private String prepareTriple(KBEntity entity, String property,
			KBEntity value) {
		return entity.getShortURI() + " "
				+ EntityManager.getKBPropertyURI(property) + " "
				+ value.getShortURI() + " . ";
	}

	private String prepareAboutTriple(KBEntity entity) {
		return entity.getShortURI() + " a " + entity.getShortClassURI() + " . ";
	}

	private String prepareVarObjectTriple(KBEntity entity, String property,
			String oVar) {
		return entity.getShortURI() + " "
				+ EntityManager.getKBPropertyURI(property) + " " + oVar + " . ";
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
		StmtIterator iter = model.listStatements(null, ResourceFactory
				.createProperty(EntityManager.getKBPropertyURI(property)),
				value);
		while (iter.hasNext()) {
			Resource r = iter.nextStatement().getSubject();
			entities.add(EntityManager.toJava(r, model));
		}
		return entities;
	}

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
