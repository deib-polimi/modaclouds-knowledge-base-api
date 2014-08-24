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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.DatasetAccessor;
import com.hp.hpl.jena.query.DatasetAccessorFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.vocabulary.RDF;

public class FusekiKBAPI {

	private class SkipPropertiesSelector extends SimpleSelector {

		private Property[] skipProperties;

		public SkipPropertiesSelector(Resource subject,
				Property... skipProperties) {
			super();
			this.subject = subject;
			this.skipProperties = skipProperties;
		}

		public boolean selects(Statement s) {
			if (!s.getSubject().equals(subject))
				return false;
			for (Property skipProp : skipProperties) {
				if (s.getPredicate().equals(skipProp))
					return false;
			}
			return true;
		}

	}

	private Logger logger = LoggerFactory.getLogger(FusekiKBAPI.class);
	private static final int DELETE_DATA_INDEX = 0;
	private static final int INSERT_DATA_INDEX = 1;
	private static final int INSERT_INDEX = 2;
	private static final int DELETE_WHERE_INDEX = 3;
	private static final int WHERE_4_INSERT_INDEX = 4;
	private static final int WHERE_4_DELETE_INDEX = 5;
	private static final int DELETE_INDEX = 6;

	/*
	 * Serialization: - Set -> Bag (using the rdf:_1, rdf:_2... properties,
	 * these are rdf:li subproperties) - List -> Seq (using the rdf:_1,
	 * rdf:_2... properties, these are rdf:li subproperties) - Map -> custom
	 * construct, same format of the containers with custom type (map) -> entity
	 * predicate map. map element [id, value] . map element [id, value] .
	 */

	private DatasetAccessor dataAccessor;

	private String knowledgeBaseURL;

	public FusekiKBAPI(String knowledgeBaseURL) {
		this.knowledgeBaseURL = knowledgeBaseURL;
		dataAccessor = DatasetAccessorFactory
				.createHTTP(getKnowledgeBaseDataURL());
	}

	public void add(Iterable<?> entities, String idPropertyName)
			throws SerializationException, DeserializationException {
		// TODO only one query should be done
		for (Object e : entities) {
			add(e, idPropertyName);
		}
	}

	/**
	 * Adds the entity to the KB. If an entity with the same property
	 * {@code idPropertyName} already exists, the old entity will be
	 * overwritten.
	 * 
	 * @param entity
	 *            the entity to be persisted
	 * @param idPropertyName
	 *            the parameter name identifying the unique identifier of the
	 *            entity
	 * @throws SerializationException
	 * @throws DeserializationException
	 */
	public void add(Object entity, String idPropertyName)
			throws SerializationException, DeserializationException {
		Preconditions.checkNotNull(idPropertyName);
		Preconditions.checkNotNull(entity);
		String entityId = getEntityId(entity, idPropertyName);
		logger.info("Entity with {} {} received", idPropertyName, entityId);
		String queryString = prepareAddQuery(entity, idPropertyName, entityId);
		logger.info("Update query:\n{}", queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	private void addNewEntity(String entityId, Object entity,
			Map<String, Object> properties, String[] queryBody)
			throws SerializationException {

		String javaClassName = entity.getClass().getName();
		String javaClassSimpleName = entity.getClass().getSimpleName();
		String subjectUri = getShortUriFromLocalName(entityId);

		queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
				RDF.type.toString(),
				getShortUriFromLocalName(javaClassSimpleName));
		queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subjectUri,
				KBConfig.javaClassRDFProperty.toString(), javaClassName);

		for (String property : properties.keySet()) {
			Object value = properties.get(property);
			if (value != null) {
				addNewProperty(subjectUri, property, value, queryBody);
			}
		}
	}

	/**
	 * Only strings and sets/lists/maps of strings are persisted
	 * 
	 * @param subjectUri
	 * @param property
	 * @param object
	 * @param queryBody
	 * @throws SerializationException
	 */
	private void addNewProperty(String subjectUri, String property,
			Object object, String[] queryBody) throws SerializationException {
		if (object instanceof Set<?>) {
			String anonId = new AnonId(UUID.randomUUID().toString()).toString();
			String anonUri = KBConfig.namespace + anonId;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
					getShortUriFromLocalName(property), anonUri);
			Set<?> setObjects = (Set<?>) object;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri,
					RDF.type.toString(), RDF.Bag.toString());
			int i = 0;
			for (Object obj : setObjects) {
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(anonUri,
						RDF.li(i).toString(), obj.toString());
				i++;
			}
		} else if (object instanceof List<?>) {
			String anonId = new AnonId(UUID.randomUUID().toString()).toString();
			String anonUri = KBConfig.namespace + anonId;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
					getShortUriFromLocalName(property), anonUri);
			List<?> listObjects = (List<?>) object;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri,
					RDF.type.toString(), RDF.Seq.toString());
			int i = 0;
			for (Object obj : listObjects) {
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(anonUri,
						RDF.li(i).toString(), obj.toString());
				i++;
			}
		} else if (object instanceof Map<?, ?>) {
			String anonId = new AnonId(UUID.randomUUID().toString()).toString();
			String anonUri = KBConfig.namespace + anonId;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
					getShortUriFromLocalName(property), anonUri);
			Map<?, ?> mapObjects = (Map<?, ?>) object;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri,
					RDF.type.toString(), KBConfig.MapRDFResource.toString());
			int i = 0;
			Set<?> keys = mapObjects.keySet();
			for (Object key : keys) {
				String internalAnonId = new AnonId(UUID.randomUUID().toString())
						.toString();
				String internalAnonUri = KBConfig.namespace + internalAnonId;
				queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri, RDF
						.li(i).toString(), internalAnonUri);
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(
						internalAnonUri, KBConfig.keyRDFProperty.toString(),
						key.toString());
				queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(
						internalAnonUri, KBConfig.valueRDFProperty.toString(),
						mapObjects.get(key).toString());
				i++;
			}
		} else {
			queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subjectUri,
					getShortUriFromLocalName(property), object.toString());
		}
	}

	public void deleteEntitiesById(Iterable<String> ids, String idPropertyName)
			throws SerializationException {
		for (String id : ids) {
			deleteEntityById(id, idPropertyName);
		}
	}

	public void deleteEntityById(String id, String idPropertyName)
			throws SerializationException {
		String queryString = "PREFIX rdf:<"
				+ RDF.getURI()
				+ "> "
				+ "PREFIX "
				+ KBConfig.uriPrefix
				+ ":<"
				+ KBConfig.namespace
				+ "> "
				+ "DELETE { "
				+ prepareTriple("?s", "?p", "?o")
				+ prepareLiteralTriple("?s",
						getShortUriFromLocalName(idPropertyName), id)
				+ prepareTriple("?o", "?p1", "?o1")
				+ prepareTriple("?o", "?p2", "?o2")
				+ " } WHERE { "
				+ prepareLiteralTriple("?s",
						getShortUriFromLocalName(idPropertyName), id)
				+ prepareTriple("?s", "?p", "?o")
				+ "OPTIONAL { "
				+ prepareTriple("?o", RDF.type.toString(), RDF.Seq.toString())
				+ prepareTriple("?o", "?p1", "?o1")
				+ "} "
				+ "OPTIONAL { "
				+ prepareTriple("?o", RDF.type.toString(), RDF.Bag.toString())
				+ prepareTriple("?o", "?p1", "?o1")
				+ "} "
				+ "OPTIONAL { "
				+ prepareTriple("?o", RDF.type.toString(),
						KBConfig.MapRDFResource.toString())
				+ prepareTriple("?o", "?p1", "?o1")
				+ prepareTriple("?o", "?p2", "?o2") + "}  }";
		logger.info("Prepared delete Query:\n" + queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	/**
	 * 
	 * @param subjectUri
	 * @param property
	 * @param object
	 * @param queryBody
	 * @throws SerializationException
	 */
	private void deleteProperty(String subjectUri, String property,
			Object object, String[] queryBody) throws SerializationException {
		String o = VariableGenerator.getNew();
		String p = VariableGenerator.getNew();
		String o1 = VariableGenerator.getNew();
		String o2 = VariableGenerator.getNew();
		String o3 = VariableGenerator.getNew();
		if (object instanceof Set<?>) {
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(subjectUri,
					getShortUriFromLocalName(property), o);
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(o,
					RDF.type.toString(), RDF.Bag.toString());
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(o, p, o1);
		} else if (object instanceof List<?>) {
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(subjectUri,
					getShortUriFromLocalName(property), o);
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(o,
					RDF.type.toString(), RDF.Seq.toString());
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(o, p, o1);
		} else if (object instanceof Map<?, ?>) {
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(subjectUri,
					getShortUriFromLocalName(property), o);
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(o,
					RDF.type.toString(), KBConfig.MapRDFResource.toString());
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(o, p, o1);
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(o1,
					KBConfig.keyRDFProperty.toString(), o2);
			queryBody[DELETE_WHERE_INDEX] += prepareTriple(o1,
					KBConfig.valueRDFProperty.toString(), o3);
		} else {
			queryBody[DELETE_DATA_INDEX] += prepareLiteralTriple(subjectUri,
					getShortUriFromLocalName(property), object.toString());
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Set<T> getAll(Class<T> entitiesClass)
			throws DeserializationException {
		Set<T> entities = new HashSet<T>();
		Model model = dataAccessor.getModel();
		StmtIterator iter = model.listStatements(null,
				KBConfig.javaClassRDFProperty, entitiesClass.getName());
		while (iter.hasNext()) {
			entities.add((T) toJava(iter.next().getSubject(), model));
		}
		return entities;
	}

	private String[] getEmptyQueryBody() {
		String[] queryBody = { "", "", "", "", "", "", "" };
		return queryBody;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Set<?> getEntitiesByPropertyValue(String property,
			String propertyName) throws DeserializationException {
		Preconditions.checkNotNull(property);
		Preconditions.checkNotNull(propertyName);
		Set entities = new HashSet();
		Model model = dataAccessor.getModel();
		Iterable<Resource> resources = getRDFResourcesByPropertyValue(
				new PropertyImpl(KBConfig.namespace, propertyName), property,
				model);
		if (resources != null) {
			for (Resource resource : resources) {
				entities.add(toJava(resource, model));
			}
		}
		return entities;
	}

	public Object getEntityById(String id, String idPropertyName)
			throws DeserializationException {
		Preconditions.checkNotNull(idPropertyName);
		Preconditions.checkNotNull(id);
		Model model = dataAccessor.getModel();
		Resource resource = getRDFResourceByPropertyValue(new PropertyImpl(
				KBConfig.namespace, idPropertyName), id, model);
		if (resource == null)
			return null;
		return toJava(resource, model);
	}

	private String getEntityId(Object entity, String idPropertyName)
			throws SerializationException {
		String entityId;
		try {
			Object entityIdObj = PropertyUtils.getProperty(entity,
					idPropertyName);
			if (entityIdObj == null || entityIdObj.toString().isEmpty())
				throw new SerializationException(
						"idPropertyName field cannot be null or empty");
			entityId = entityIdObj.toString();
		} catch (Exception e) {
			throw new SerializationException(e);
		}
		return entityId;
	}

	public Set<String> getIds(Class<?> entitiesClass, String idPropertyName) {
		Set<String> ids = new HashSet<String>();
		Model model = dataAccessor.getModel();
		StmtIterator iter = model.listStatements(null,
				KBConfig.javaClassRDFProperty, entitiesClass.getName());
		while (iter.hasNext()) {
			ids.add(iter
					.nextStatement()
					.getSubject()
					.getProperty(
							new PropertyImpl(KBConfig.namespace, idPropertyName))
					.getObject().asLiteral().toString());
		}
		return ids;
	}

	private Class<?> getJavaClass(Resource resource, Model model)
			throws ClassNotFoundException {
		RDFNode rdfValue = getRDFPropertyValue(resource,
				KBConfig.javaClassRDFProperty, model);
		String javaClass = rdfValue.asLiteral().getString();
		return Class.forName(javaClass);
	}

	/**
	 * get all object properties except for the class
	 * 
	 * @param object
	 * @return
	 */
	private Map<String, Object> getJavaProperties(Object object) {
		Map<String, Object> properties = new HashMap<String, Object>();
		try {
			properties = PropertyUtils.describe(object);
			properties.remove("class");
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Cannot retrieve object properties for serialization", e);
		}
		return properties;
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

	private RDFNode getRDFPropertyValue(Resource resource, Property property,
			Model model) {
		NodeIterator iterator = model.listObjectsOfProperty(resource, property);
		return iterator.hasNext() ? iterator.next() : null;
	}

	private Resource getRDFResourceByPropertyValue(Property property,
			String value, Model model) {
		ResIterator iterator = model.listSubjectsWithProperty(property, value);
		return iterator.hasNext() ? iterator.next() : null;
	}

	private Set<Resource> getRDFResourcesByPropertyValue(Property property,
			String value, Model model) {
		ResIterator iterator = model.listSubjectsWithProperty(property, value);
		return Sets.newHashSet(iterator);
	}

	private String getShortUriFromLocalName(String localname)
			throws SerializationException {
		try {
			return KBConfig.uriPrefix + ":"
					+ URLEncoder.encode(localname, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SerializationException(e);
		}
	}

	private String getUriFromLocalName(String localname)
			throws SerializationException {
		try {
			return KBConfig.namespace + URLEncoder.encode(localname, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SerializationException(e);
		}
	}

	private String prepareAddQuery(Object entity, String idPropertyName,
			String entityId) throws SerializationException,
			DeserializationException {
		String[] queryBody = prepareAddQueryBody(entity, idPropertyName,
				entityId);
		String queryString = "PREFIX " + KBConfig.uriPrefix + ":<"
				+ KBConfig.namespace + "> PREFIX rdf:<" + RDF.getURI() + "> "
				+ "DELETE DATA { " + queryBody[DELETE_DATA_INDEX]
				+ " } ; DELETE { " + queryBody[DELETE_INDEX] + " } WHERE { "
				+ queryBody[WHERE_4_DELETE_INDEX] + " } ; DELETE WHERE { "
				+ queryBody[DELETE_WHERE_INDEX] + " } ; INSERT DATA { "
				+ queryBody[INSERT_DATA_INDEX] + " } ; INSERT { "
				+ queryBody[INSERT_INDEX] + " } WHERE { "
				+ queryBody[WHERE_4_INSERT_INDEX] + " } ";
		return queryString;
	}

	private String[] prepareAddQueryBody(Object newEntity,
			String idPropertyName, String entityId)
			throws SerializationException, DeserializationException {
		String[] queryBody = getEmptyQueryBody();
		Map<String, Object> newProperties = getJavaProperties(newEntity);
		Object oldEntity = getEntityById(entityId, idPropertyName);
		if (oldEntity == null) {
			logger.info("Adding new entity with {} {}", idPropertyName,
					entityId);
			addNewEntity(entityId, newEntity, newProperties, queryBody);
		} else {
			logger.info("An entity with {} {} already exists, updating it",
					idPropertyName, entityId);
			Map<String, Object> oldEntityProperties = getJavaProperties(oldEntity);
			for (String propertyName : newProperties.keySet()) {
				Object oldEntityProperty = oldEntityProperties
						.get(propertyName);
				Object newEntityProperty = newProperties.get(propertyName);
				if (oldEntityProperty == null && newEntityProperty == null) {
					break;
				} else if (oldEntityProperty == null) {
					addNewProperty(getShortUriFromLocalName(entityId),
							propertyName, newEntityProperty, queryBody);
				} else if (newEntityProperty == null) {
					deleteProperty(getShortUriFromLocalName(entityId),
							propertyName, oldEntityProperty, queryBody);
				} else if (!oldEntityProperty.equals(newEntityProperty)) { // add
																			// new
					// control
					// for List
					// and Map
					// and
					// change
					// the Set
					// serialization
					deleteProperty(getShortUriFromLocalName(entityId),
							propertyName, oldEntityProperty, queryBody);
					addNewProperty(getShortUriFromLocalName(entityId),
							propertyName, newEntityProperty, queryBody);
				}
			}

		}
		return queryBody;
	}

	private String prepareLiteralTriple(String s, String p, String o) {
		if (s.contains("/"))
			s = "<" + s + ">";
		if (p.contains("/"))
			p = "<" + p + ">";
		if (o.contains("/"))
			o = "<" + o + ">";
		return s + " " + p + " \"" + o + "\" . ";
	}

	private String prepareTriple(String s, String p, String o) {
		if (s.contains("/"))
			s = "<" + s + ">";
		if (p.contains("/"))
			p = "<" + p + ">";
		if (o.contains("/"))
			o = "<" + o + ">";
		return s + " " + p + " " + o + " . ";
	}

	private boolean skipObjectDeserialization(RDFNode rdfObject) {
		return rdfObject.isResource()
				&& !rdfObject.asResource().hasProperty(RDF.type, RDF.Bag)
				&& !rdfObject.asResource().hasProperty(RDF.type, RDF.Seq)
				&& !rdfObject.asResource().hasProperty(RDF.type,
						KBConfig.MapRDFResource);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object toJava(RDFNode rdfNode, Model model)
			throws DeserializationException {
		Object entity = null;
		if (rdfNode.isResource()) {
			assert model.contains(rdfNode.asResource(), null, (RDFNode) null);
			StmtIterator propIterator = model
					.listStatements(new SkipPropertiesSelector(rdfNode
							.asResource(), RDF.type,
							KBConfig.javaClassRDFProperty));
			if (rdfNode.asResource().hasProperty(RDF.type, RDF.Bag)) {
				entity = new HashSet();
				while (propIterator.hasNext()) {
					((Set) entity).add(toJava(propIterator.next().getObject(),
							model));
				}
			} else if (rdfNode.asResource().hasProperty(RDF.type, RDF.Seq)) {
				entity = new ArrayList();
				while (propIterator.hasNext()) {
					((List) entity).add(toJava(propIterator.next().getObject(),
							model));
				}
			} else if (rdfNode.asResource().hasProperty(RDF.type,
					KBConfig.MapRDFResource)) {
				entity = new HashMap();
				while (propIterator.hasNext()) {
					StmtIterator mapIterator = model.listStatements(
							rdfNode.asResource(), null, (RDFNode) null);
					Object key = null;
					Object value = null;
					while (mapIterator.hasNext()) {
						Statement stm = mapIterator.next();
						if (stm.getPredicate().equals(KBConfig.keyRDFProperty))
							key = toJava(stm.getObject(), model);
						if (stm.getPredicate()
								.equals(KBConfig.valueRDFProperty))
							value = toJava(stm.getObject(), model);
					}
					((Map) entity).put(key, value);
				}
			} else {
				try {
					Class<?> entityClass = getJavaClass(rdfNode.asResource(),
							model);
					entity = entityClass.newInstance();
					Map<String, Object> properties = new HashMap<String, Object>();
					while (propIterator.hasNext()) {
						Statement stmt = propIterator.nextStatement();
						RDFNode rdfObject = stmt.getObject();
						if (!skipObjectDeserialization(rdfObject)) { // we are
																		// not
																		// deserializing
																		// all
																		// related
																		// entities
							String javaProperty = stmt.getPredicate()
									.getLocalName();
							Object value = toJava(rdfObject, model);
							properties.put(javaProperty, value);
						}
					}
					BeanUtils.populate(entity, properties);
				} catch (Exception e) {
					throw new DeserializationException(e);
				}
			}
		} else {
			entity = rdfNode.asLiteral().getValue();
		}
		return entity;
	}

	// concatenate strings
	// private void concatBodies(String[] body1, String[] body2) {
	// assert body1.length == body2.length;
	// for (int i = 0; i < body1.length; i++) {
	// body1[i] += body2[i];
	// }
	// }

	// public Set<KBEntity> getByPropertyValue(String property, String value) {
	// Set<KBEntity> entities = new HashSet<KBEntity>();
	// // TODO avoid downloading entire model
	// Model model = dataAccessor.getModel();
	// if (value != null) {
	// StmtIterator iter = model.listStatements(null, ResourceFactory
	// .createProperty(EntityManager.getKBPropertyURI(property)),
	// value);
	// while (iter.hasNext()) {
	// Resource r = iter.nextStatement().getSubject();
	// entities.add(EntityManager.toJava(r, model,
	// this.entitiesPackage));
	// }
	// } else {
	// StmtIterator iter = model.listStatements(null, ResourceFactory
	// .createProperty(EntityManager.getKBPropertyURI(property)),
	// (RDFNode) null);
	// while (iter.hasNext()) {
	// Resource r = iter.nextStatement().getSubject();
	// entities.add(EntityManager.toJava(r, model,
	// this.entitiesPackage));
	// }
	// }
	// return entities;
	// }

	public void uploadOntology(OntModel model) {
		dataAccessor.add(model);
	}

	// public <T extends KBEntity> Set<T> getAll(Class<T> subjectEntityClass,
	// String property, String object) { // THINK ABOUT SETS, LISTS, MAPS...
	// Set<T> entities = new HashSet<T>();
	// // TODO avoid downloading entire model
	// Model model = dataAccessor.getModel();
	// StmtIterator iter = model.listStatements(null,
	// ResourceFactory.createProperty(KBEntity.uriBase + property),
	// ResourceFactory.createPlainLiteral(object));
	//
	// while (iter.hasNext()) {
	// Resource r = iter.nextStatement().getSubject();
	// if (r.hasURI(EntityManager.getKBClassURI(subjectEntityClass)))
	// entities.add((T) EntityManager.toJava(r, model,
	// this.class_package));
	// }
	// return entities;
	// }
}
