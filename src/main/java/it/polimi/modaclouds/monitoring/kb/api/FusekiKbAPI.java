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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;

public class FusekiKbAPI extends KbAPI {

	private Logger logger = LoggerFactory.getLogger(FusekiKbAPI.class);

	DatasetAccessor datasetAccessor;

	private String knowledgeBaseURL;

	public FusekiKbAPI(String knowledgeBaseURL) {
		this.knowledgeBaseURL = knowledgeBaseURL;
		datasetAccessor = DatasetAccessorFactory
				.createHTTP(getKnowledgeBaseDataURL());
	}

	public void addMany(Iterable<?> entities, String idPropertyName,
			String graphName) throws SerializationException {
		String queryString = sqf.addMany(entities, idPropertyName, graphName);
		logger.debug("Update query:\n{}", queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	@Override
	public void add(Object entity, String idPropertyName, String graphName)
			throws SerializationException {
		String queryString = sqf.add(entity, idPropertyName, graphName);
		logger.debug("Update query:\n{}", queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	@Override
	public void add(Object entity, String idPropertyName)
			throws SerializationException, IOException {
		add(entity, idPropertyName, "default");
	}

	public void deleteAll(Set<?> entities, String idPropertyName)
			throws SerializationException {
		deleteAll(entities, idPropertyName, "default");
	}

	public void deleteAll(Set<?> entities, String idPropertyName,
			String graphName) throws SerializationException {
		Set<Object> ids = new HashSet<Object>();
		for (Object entity : entities) {
			ids.add(sqf.getEntityId(entity, idPropertyName));
		}
		deleteEntitiesByPropertyValues(ids, idPropertyName, graphName);
	}

	public void deleteEntitiesByPropertyValues(Set<Object> propertyValues,
			String propertyName) throws SerializationException {
		deleteEntitiesByPropertyValues(propertyValues, propertyName, "default");
	}

	@Override
	public void deleteEntitiesByPropertyValues(Set<?> propertyValues,
			String propertyName, String graphName) {
		String queryString = sqf.deleteEntitiesByPropertyValue(propertyValues,
				propertyName, graphName);
		logger.info("Prepared delete Query:\n" + queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	/**
	 * 
	 * @param propertyValue
	 * @param propertyName
	 */
	public void deleteEntitiesByPropertyValue(Object propertyValue,
			String propertyName) {
		deleteEntitiesByPropertyValue(propertyValue, propertyName, "default");
	}

	/**
	 * 
	 * @param propertyValue
	 * @param propertyName
	 * @param graphName
	 */
	public void deleteEntitiesByPropertyValue(Object propertyValue,
			String propertyName, String graphName) {
		String queryString = sqf.deleteEntitiesByPropertyValue(propertyValue,
				propertyName, graphName);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	public String getGraphURL(String kbURL, String graphName) {
		try {
			return kbURL + "/data?graph="
					+ URLEncoder.encode(graphName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> Set<T> getAll(Class<T> entitiesClass)
			throws DeserializationException {
		return getAll(entitiesClass, "default");
	}

	@Override
	public Set<?> getAll(String graphName) throws DeserializationException {
		return getAll(null, graphName);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Set<T> getAll(Class<T> entitiesClass, String graphName)
			throws DeserializationException {
		Set<T> entities = new HashSet<T>();
		Model model = datasetAccessor.getModel(graphName);
		if (model == null) {
			return entities;
		}
		StmtIterator iter = model.listStatements(null, JAVA_CLASS_PROPERTY,
				entitiesClass != null ? entitiesClass.getName() : null);
		while (iter.hasNext()) {
			entities.add((T) toJava(iter.next().getSubject(), model));
		}
		return entities;
	}

	public Set<?> getEntitiesByPropertyValue(String property,
			String propertyName) throws DeserializationException {
		return getEntitiesByPropertyValue(property, propertyName, "default");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Set<?> getEntitiesByPropertyValue(String property,
			String propertyName, String graphName)
			throws DeserializationException {
		Preconditions.checkNotNull(property);
		Preconditions.checkNotNull(propertyName);
		Set entities = new HashSet();
		Model model = datasetAccessor.getModel(graphName);
		if (model == null) {
			logger.warn("Graph {} does not exist yet", graphName);
			return entities;
		}
		Iterable<Resource> resources = getRDFResourcesByPropertyValue(
				new PropertyImpl(KB_NAME_SPACE, propertyName), property, model);
		if (resources != null) {
			for (Resource resource : resources) {
				entities.add(toJava(resource, model));
			}
		}
		return entities;
	}

	@Override
	public Object getEntityById(String id, String idPropertyName)
			throws DeserializationException {
		return getEntityById(id, idPropertyName, "default");
	}

	@Override
	public Object getEntityById(String id, String idPropertyName,
			String graphName) throws DeserializationException {
		Preconditions.checkNotNull(idPropertyName);
		Preconditions.checkNotNull(id);
		Model model = datasetAccessor.getModel(graphName);
		if (model == null) {
			logger.info("Graph {} does not exist yet", graphName);
			return null;
		}
		Resource resource = getRDFResourceByPropertyValue(new PropertyImpl(
				KB_NAME_SPACE, idPropertyName), id, model);
		if (resource == null) {
			logger.info("No entity found with {} {} on the KB", idPropertyName,
					id);
			return null;
		}
		Object entity = toJava(resource, model);
		logger.debug("Retrieved entity: {}", entity.toString());
		return entity;
	}

	public Set<String> getIds(Class<?> entitiesClass, String idPropertyName) {
		return getIds(entitiesClass, idPropertyName, "default");
	}

	public Set<String> getIds(Class<?> entitiesClass, String idPropertyName,
			String graphName) {
		Set<String> ids = new HashSet<String>();
		Model model = datasetAccessor.getModel(graphName);
		if (model == null) {
			logger.warn("Graph {} does not exist yet", graphName);
			return ids;
		}
		StmtIterator iter = model.listStatements(null, JAVA_CLASS_PROPERTY,
				entitiesClass.getName());
		while (iter.hasNext()) {
			ids.add(iter
					.nextStatement()
					.getSubject()
					.getProperty(
							new PropertyImpl(KB_NAME_SPACE, idPropertyName))
					.getObject().asLiteral().toString());
		}
		return ids;
	}

	public String getKnowledgeBaseDataURL() {
		return knowledgeBaseURL + "/data";
	}

	public String getKnowledgeBaseQueryURL() {
		return knowledgeBaseURL + "/query";
	}

	public String getKnowledgeBaseUpdateURL() {
		return knowledgeBaseURL + "/update";
	}

	public String getKnowledgeBaseURL() {
		return knowledgeBaseURL;
	}

	private Set<Resource> getRDFResourcesByPropertyValue(Property property,
			String value, Model model) {
		ResIterator iterator = model.listSubjectsWithProperty(property, value);
		return Sets.newHashSet(iterator);
	}

	@Override
	public void clearGraph(String graphName) {
		UpdateRequest query = UpdateFactory.create(sqf.clearGraph(graphName),
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	@Override
	public void clearAll() {
		UpdateRequest query = UpdateFactory.create(sqf.clearAll(),
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	@Override
	public void putModel(OntModel model, String graphName) throws IOException {
		datasetAccessor.add(graphName, model);
	}

}
