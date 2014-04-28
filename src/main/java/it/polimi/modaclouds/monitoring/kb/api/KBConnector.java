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

import it.polimi.modaclouds.qos_models.monitoring_ontology.KBEntity;
import it.polimi.modaclouds.qos_models.monitoring_ontology.MO;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.NotImplementedException;
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

public class KBConnector {

	private static final int DELETE_DATA_INDEX = 0;
	private static final int INSERT_DATA_INDEX = 1;
	private static final int DELETE_INDEX = 2;
	private static final int INSERT_INDEX = 3;
	private static final int WHERE_DELETE_INDEX = 4;
	private static final int WHERE_INSERT_INDEX = 5;

	private String[] getEmptyQueryBody() {
		String[] queryBody = { "", "", "", "", "", "" };
		return queryBody;
	}

	private static KBConnector _instance = null;
	private Logger logger = LoggerFactory
			.getLogger(KBConnector.class.getName());
	private DatasetAccessor da;

	private URL kbURL;

	public static KBConnector getInstance() throws MalformedURLException,
			FileNotFoundException {
		if (_instance == null) {
			_instance = new KBConnector();
		}
		return _instance;
	}

	public URL getKbURL() {
		return kbURL;
	}

	public void setKbURL(URL kbURL) {
		this.kbURL = kbURL;
	}
	
	public KBEntity get(String uri) {
		Model model = da.getModel();
		Resource r = ResourceFactory.createResource(uri);
		KBEntity entity = KBMapping.toJava(r, model);
		return entity;
	}

	/**
	 * Adds the entity in the KB and, recursively, all related properties. If
	 * the entity or any child entity already exists in the KB, the entity will
	 * be updated.
	 * 
	 * @param entity
	 */
	public void add(KBEntity entity) {
		String queryString = prepareAddQuery(entity);
		logger.info("Prepared add Query:\n" + queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				MO.getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	public void delete(String uri) {
		throw new NotImplementedException();
	}

	public void addAll(Set<? extends KBEntity> entities) {
		for (KBEntity entity : entities) {
			add(entity);
		}
	}

	public Set<KBEntity> getAll(Class<? extends KBEntity> entityClass) {
		Set<KBEntity> entities = new HashSet<KBEntity>();
		Model model = da.getModel();
		StmtIterator iter = model.listStatements(
				null,
				RDF.type,
				ResourceFactory.createResource(KBMapping.toKB(
						entityClass.getSimpleName(), false)));
		while (iter.hasNext()) {
			Resource r = iter.nextStatement().getSubject();
			entities.add(KBMapping.toJava(r, model));
		}
		return entities;
	}

	public <T extends KBEntity> Set<String> getURIs(Class<T> entityClass) {
		Set<String> uris = new HashSet<String>();
		Model model = da.getModel();
		StmtIterator iter = model.listStatements(
				null,
				RDF.type,
				ResourceFactory.createResource(KBMapping.toKB(
						entityClass.getSimpleName(), false)));
		while (iter.hasNext()) {
			uris.add(iter.nextStatement().getSubject().getURI());
		}
		return uris;
	}

	private KBConnector() throws MalformedURLException, FileNotFoundException {
		loadConfig();
		da = DatasetAccessorFactory.createHTTP(MO
				.getKnowledgeBaseDataURL());
	}

	private void loadConfig() throws MalformedURLException,
			FileNotFoundException {
		Config config = Config.getInstance();
		String kbAddress = config.getKBServerAddress();
		int ddaPort = config.getKBServerPort();
		kbAddress = cleanAddress(kbAddress);
		kbURL = new URL("http://" + kbAddress + ":" + ddaPort);
		MO.setKnowledgeBaseURL(kbURL);
	}

	private static String cleanAddress(String address) {
		if (address.indexOf("://") != -1)
			address = address.substring(address.indexOf("://") + 3);
		if (address.endsWith("/"))
			address = address.substring(0, address.length() - 1);
		return address;
	}

	private String prepareAddQuery(KBEntity entity) {
		Set<KBEntity> explored = new HashSet<KBEntity>();
		VariableGenerator varGen = new VariableGenerator();
		String[] queryBody = prepareAddQueryBody(entity, explored, varGen);
		String queryString = "PREFIX " + MO.prefix + ":<" + MO.URI + "> "
				+ "DELETE DATA { " + queryBody[DELETE_DATA_INDEX]
				+ " } ; INSERT DATA { " + queryBody[INSERT_DATA_INDEX]
				+ " } ; DELETE { " + queryBody[DELETE_INDEX] + " } WHERE { "
				+ queryBody[WHERE_DELETE_INDEX] + " } ; INSERT { "
				+ queryBody[INSERT_INDEX] + " } WHERE { "
				+ queryBody[WHERE_INSERT_INDEX] + " } ";
		return queryString;
	}

	@SuppressWarnings("unchecked")
	private String[] prepareAddQueryBody(KBEntity entity,
			Set<KBEntity> explored, VariableGenerator varGen) {
		String[] queryBody = getEmptyQueryBody();
		if (!explored.contains(entity)) {
			explored.add(entity);
			Map<String, Object> properties = getProperties(entity);
			KBEntity entityFromKB = get(entity.getUri());
			if (entityFromKB == null) { // entity does not exist
				addNewEntity(entity, queryBody);
				for (String property : properties.keySet()) {
					Object value = properties.get(property);
					if (value != null) {
						addNewProperty(entity, property, value, explored,
								queryBody, varGen);
					}
				}
			} else { // entity already exists
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
							for (Object o: (Set<Object>)entityFromKBValue) {
								logger.info(o.toString());
							}
							for (Object o: (Set<Object>)value) {
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
				String[] temp = prepareAddQueryBody(entity, property, object,
						explored, varGen);
				concatBodies(queryBody, temp);
			}
		} else {
			String[] temp = prepareAddQueryBody(entity, property, value,
					explored, varGen);
			concatBodies(queryBody, temp);
		}
	}

	private void addNewEntity(KBEntity entity, String[] queryBody) {
		queryBody[INSERT_DATA_INDEX] += prepareAboutTriple(entity);
	}

	private String[] prepareAddQueryBody(KBEntity subject, String property,
			Object object, Set<KBEntity> explored, VariableGenerator varGen) {
		String[] queryBody = getEmptyQueryBody();
		if (object instanceof KBEntity) {
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
		return entity.getShortURI() + " " + KBMapping.toKB(property, true)
				+ " \"" + literal + "\" . ";
	}

	private String prepareTriple(KBEntity entity, String property,
			KBEntity value) {
		return entity.getShortURI() + " " + KBMapping.toKB(property, true)
				+ " " + value.getShortURI() + " . ";
	}

	private String prepareAboutTriple(KBEntity entity) {
		return entity.getShortURI() + " a " + entity.getShortClassURI() + " . ";
	}

	private String prepareVarObjectTriple(KBEntity entity, String property,
			String oVar) {
		return entity.getShortURI() + " " + KBMapping.toKB(property, true)
				+ " " + oVar + " . ";
	}

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

	private void concatBodies(String[] body1, String[] body2) {
		assert body1.length == body2.length;
		for (int i = 0; i < body1.length; i++) {
			body1[i] += body2[i];
		}
	}

	public Set<KBEntity> getByPropertyValue(String property, String value) {
		Set<KBEntity> entities = new HashSet<KBEntity>();
		Model model = da.getModel();
		StmtIterator iter = model.listStatements(
				null,
				ResourceFactory.createProperty(KBMapping.toKB(property, false)),
				value);
		while (iter.hasNext()) {
			Resource r = iter.nextStatement().getSubject();
			entities.add(KBMapping.toJava(r, model));
		}
		return entities;
	}
}
