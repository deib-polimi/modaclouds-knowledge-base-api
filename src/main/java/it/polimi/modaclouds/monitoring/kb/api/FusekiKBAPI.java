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
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
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
import com.hp.hpl.jena.vocabulary.XSD;

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
	private static final int DELETE_INDEX = 1;
	private static final int DELETE_WHERE_INDEX = 2;
	private static final int WHERE_4_DELETE_INDEX = 3;
	private static final int INSERT_DATA_INDEX = 4;
	private static final int INSERT_INDEX = 5;
	private static final int WHERE_4_INSERT_INDEX = 6;

	DatasetAccessor datasetAccessor;

	private String knowledgeBaseURL;

	public FusekiKBAPI(String knowledgeBaseURL) {
		this.knowledgeBaseURL = knowledgeBaseURL;
		datasetAccessor = DatasetAccessorFactory
				.createHTTP(getKnowledgeBaseDataURL());
	}

	public void add(Iterable<?> entities, String idPropertyName,
			String graphName) throws SerializationException,
			DeserializationException {
		// TODO only one query should be done
		for (Object e : entities) {
			add(e, idPropertyName, graphName);
		}
	}

	public static String getGraphURI(String graphName) {
		if (graphName.equals("default"))
			return graphName;
		return Config.graphsNamespace + Util.urlEncode(graphName);
	}

	public static String getGraphURL(String kbURL, String graphName) {
		return kbURL + "/data?graph=" + Util.urlEncode(getGraphURI(graphName));
	}

	/**
	 * Adds the entity to the default graph of the KB. If an entity with the
	 * same property {@code idPropertyName} already exists in the default graph,
	 * the old entity will be overwritten.
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
		add(entity, idPropertyName, "default");
	}

	/**
	 * Adds the entity to a specific graph in the KB. If an entity with the same
	 * property {@code idPropertyName} already exists in the graph, the old
	 * entity will be overwritten.
	 * 
	 * @param entity
	 *            the entity to be persisted
	 * @param idPropertyName
	 *            the parameter name identifying the unique identifier of the
	 *            entity
	 * @param graphName
	 *            the name of the graph where the entity should be persisted
	 * @throws SerializationException
	 * @throws DeserializationException
	 */
	public void add(Object entity, String idPropertyName, String graphName)
			throws SerializationException, DeserializationException {
		Preconditions.checkNotNull(idPropertyName);
		Preconditions.checkNotNull(entity);
		String entityId = getEntityId(entity, idPropertyName);
		logger.info("Adding entity with {} {} to graph {}", idPropertyName,
				entityId, graphName);
		String[] queryBody = getEmptyQueryBody();
		prepareDeleteByPropertyValueQueryBody(entityId, idPropertyName,
				queryBody);
		prepareAddQueryBody(entity, idPropertyName, entityId, queryBody);
		String queryString = prepareQueryFromBody(graphName, queryBody);
		logger.info("Update query:\n{}", queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	private void addNewProperty(String subjectUri, String propertyUri,
			Object object, String idPropertyName, String[] queryBody)
			throws SerializationException {
		if (object instanceof Set<?>) {
			String anonId = new AnonId(UUID.randomUUID().toString()).toString();
			String anonUri = Config.entitiesNamespace + anonId;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
					propertyUri, anonUri);
			Set<?> setObjects = (Set<?>) object;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri,
					RDF.type.toString(), RDF.Bag.toString());
			int i = 0;
			for (Object obj : setObjects) {
				addNewProperty(anonUri, RDF.li(i).toString(), obj,
						idPropertyName, queryBody);
				i++;
			}
		} else if (object instanceof List<?>) {
			String anonId = new AnonId(UUID.randomUUID().toString()).toString();
			String anonUri = Config.entitiesNamespace + anonId;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
					propertyUri, anonUri);
			List<?> listObjects = (List<?>) object;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri,
					RDF.type.toString(), RDF.Seq.toString());
			int i = 0;
			for (Object obj : listObjects) {
				addNewProperty(anonUri, RDF.li(i).toString(), obj,
						idPropertyName, queryBody);
				i++;
			}
		} else if (object instanceof Map<?, ?>) {
			String anonId = new AnonId(UUID.randomUUID().toString()).toString();
			String anonUri = Config.entitiesNamespace + anonId;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
					propertyUri, anonUri);
			Map<?, ?> mapObjects = (Map<?, ?>) object;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri,
					RDF.type.toString(), Config.MapRDFResource.toString());
			int i = 0;
			Set<?> keys = mapObjects.keySet();
			for (Object key : keys) {
				String internalAnonId = new AnonId(UUID.randomUUID().toString())
						.toString();
				String internalAnonUri = Config.entitiesNamespace
						+ internalAnonId;
				queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri, RDF
						.li(i).toString(), internalAnonUri);
				addNewProperty(internalAnonUri,
						Config.keyRDFProperty.toString(), key, idPropertyName,
						queryBody);
				addNewProperty(internalAnonUri,
						Config.valueRDFProperty.toString(),
						mapObjects.get(key), idPropertyName, queryBody);
				i++;
			}
		} else if (isLiteral(object)) {
			queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subjectUri,
					propertyUri, object.toString(), getLiteralDataType(object));
		} else { // it's an entity
			String entityId = getEntityId(object, idPropertyName);
			queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
					propertyUri, getShortUriFromLocalName(entityId));
		}
	}

	private XSDDatatype getLiteralDataType(Object object) {
		List<XSDDatatype> types = new ArrayList<XSDDatatype>();
		types.add(XSDDatatype.XSDstring);
		types.add(XSDDatatype.XSDint);
		types.add(XSDDatatype.XSDinteger);
		types.add(XSDDatatype.XSDboolean);
		types.add(XSDDatatype.XSDbyte);
		types.add(XSDDatatype.XSDdecimal);
		types.add(XSDDatatype.XSDdouble);
		types.add(XSDDatatype.XSDfloat);
		types.add(XSDDatatype.XSDlong);
		types.add(XSDDatatype.XSDshort);
		for (XSDDatatype type : types) {
			if (type.getJavaClass().isInstance(object)) {
				return type;
			}
		}
		return null;
	}

	private boolean isLiteral(Object object) {
		return getLiteralDataType(object) != null;
	}

	public void deleteAll(Set<?> entities, String idPropertyName)
			throws SerializationException {
		deleteAll(entities, idPropertyName, "default");
	}

	public void deleteAll(Set<?> entities, String idPropertyName,
			String graphName) throws SerializationException {
		Set<Object> ids = new HashSet<Object>();
		for (Object entity : entities) {
			ids.add(getEntityId(entity, idPropertyName));
		}
		deleteEntitiesByPropertyValues(ids, idPropertyName, graphName);
	}

	public void deleteEntitiesByPropertyValues(Set<Object> propertyValues,
			String propertyName) throws SerializationException {
		deleteEntitiesByPropertyValues(propertyValues, propertyName, "default");
	}

	public void deleteEntitiesByPropertyValues(Set<Object> propertyValues,
			String propertyName, String graphName)
			throws SerializationException {
		Preconditions.checkNotNull(propertyValues);
		Preconditions.checkNotNull(propertyName);
		Preconditions.checkArgument(!propertyValues.isEmpty());

		String[] queryBody = getEmptyQueryBody();

		prepareDeleteByPropertyValuesQueryBody(propertyValues, propertyName,
				queryBody);

		String queryString = prepareQueryFromBody(graphName, queryBody);
		logger.info("Prepared delete Query:\n" + queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	private void prepareDeleteByPropertyValuesQueryBody(
			Set<Object> propertyValues, String propertyName, String[] queryBody) {
		String unionBlock = "{ ";
		int left = propertyValues.size();
		for (Object value : propertyValues) {
			left--;
			unionBlock += prepareLiteralTriple("?s",
					getShortUriFromLocalName(propertyName), value.toString(),
					getLiteralDataType(value));
			if (left > 0) {
				unionBlock += "} UNION { ";
			}
		}
		unionBlock += "} . ";

		queryBody[DELETE_INDEX] += prepareTriple("?s", "?p", "?o")
				+ prepareTriple("?o", "?p1", "?o1")
				+ prepareTriple("?o", "?p2", "?o2");
		queryBody[WHERE_4_DELETE_INDEX] += unionBlock
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
						Config.MapRDFResource.toString())
				+ prepareTriple("?o", "?p1", "?o1")
				+ prepareTriple("?o", "?p2", "?o2");
	}

	/**
	 * 
	 * @param propertyValue
	 *            must be either a String or a primitive value
	 * @param propertyName
	 * @throws SerializationException
	 */
	public void deleteEntitiesByPropertyValue(Object propertyValue,
			String propertyName) throws SerializationException {
		if (!isLiteral(propertyValue))
			throw new SerializationException(
					"The value is neither a string or a primitive value");
		deleteEntitiesByPropertyValue(propertyValue, propertyName, "default");
	}

	/**
	 * 
	 * @param propertyValue
	 *            must be either a String or a primitive value
	 * @param propertyName
	 * @param graphName
	 * @throws SerializationException
	 */
	public void deleteEntitiesByPropertyValue(Object propertyValue,
			String propertyName, String graphName)
			throws SerializationException {
		if (!isLiteral(propertyValue))
			throw new SerializationException(
					"The value is neither a string or a primitive value");
		String[] queryBody = getEmptyQueryBody();
		prepareDeleteByPropertyValueQueryBody(propertyValue, propertyName,
				queryBody);

		String queryString = prepareQueryFromBody(graphName, queryBody);
		logger.info("Prepared delete Query:\n" + queryString);
		UpdateRequest query = UpdateFactory.create(queryString,
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	private void prepareDeleteByPropertyValueQueryBody(Object propertyValue,
			String propertyName, String[] queryBody) {
		queryBody[DELETE_INDEX] += prepareTriple("?s", "?p", "?o")
				+ prepareLiteralTriple("?s",
						getShortUriFromLocalName(propertyName),
						propertyValue.toString(),
						getLiteralDataType(propertyValue))
				+ prepareTriple("?o", "?p1", "?o1")
				+ prepareTriple("?o1", "?p2", "?o2")
				+ prepareTriple("?o1", "?p3", "?o3")
				+ prepareTriple("?o", RDF.type.toString(), RDF.Seq.toString())
				+ prepareTriple("?o", RDF.type.toString(), RDF.Bag.toString())
				+ prepareTriple("?o", RDF.type.toString(),
						Config.MapRDFResource.toString());
		queryBody[WHERE_4_DELETE_INDEX] += prepareLiteralTriple("?s",
				getShortUriFromLocalName(propertyName),
				propertyValue.toString(), getLiteralDataType(propertyValue))
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
						Config.MapRDFResource.toString())
				+ prepareTriple("?o", "?p1", "?o1")
				+ prepareTriple("?o1", "?p2", "?o2")
				+ prepareTriple("?o1", "?p3", "?o3") + "} ";
	}

	public <T> Set<T> getAll(Class<T> entitiesClass)
			throws DeserializationException {
		return getAll(entitiesClass, "default");
	}

	public Set<?> getAllEntities(String graphName)
			throws DeserializationException {
		return getAll(null, graphName);
	}

	@SuppressWarnings("unchecked")
	public <T> Set<T> getAll(Class<T> entitiesClass, String graphName)
			throws DeserializationException {
		Set<T> entities = new HashSet<T>();
		Model model = datasetAccessor.getModel(getGraphURI(graphName));
		if (model == null) {
			return entities;
		}
		StmtIterator iter = model.listStatements(null,
				Config.javaClassRDFProperty,
				entitiesClass != null ? entitiesClass.getName() : null);
		while (iter.hasNext()) {
			entities.add((T) toJava(iter.next().getSubject(), model));
		}
		return entities;
	}

	private String[] getEmptyQueryBody() {
		String[] queryBody = { "", "", "", "", "", "", "" };
		return queryBody;
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
		Model model = datasetAccessor.getModel(getGraphURI(graphName));
		if (model == null) {
			logger.warn("Graph {} does not exist yet", graphName);
			return entities;
		}
		Iterable<Resource> resources = getRDFResourcesByPropertyValue(
				new PropertyImpl(Config.entitiesNamespace, propertyName),
				property, model);
		if (resources != null) {
			for (Resource resource : resources) {
				entities.add(toJava(resource, model));
			}
		}
		return entities;
	}

	public Object getEntityById(String id, String idPropertyName)
			throws DeserializationException {
		return getEntityById(id, idPropertyName, "default");
	}

	public Object getEntityById(String id, String idPropertyName,
			String graphName) throws DeserializationException {
		Preconditions.checkNotNull(idPropertyName);
		Preconditions.checkNotNull(id);
		Model model = datasetAccessor.getModel(getGraphURI(graphName));
		if (model == null) {
			logger.info("Graph {} does not exist yet", graphName);
			return null;
		}
		Resource resource = getRDFResourceByPropertyValue(new PropertyImpl(
				Config.entitiesNamespace, idPropertyName), id, model);
		if (resource == null) {
			logger.info("No entity found with {} {} on the KB", idPropertyName,
					id);
			return null;
		}
		Object entity = toJava(resource, model);
		logger.debug("Retrieved entity: {}", entity.toString());
		return entity;
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
		return getIds(entitiesClass, idPropertyName, "default");
	}

	public Set<String> getIds(Class<?> entitiesClass, String idPropertyName,
			String graphName) {
		Set<String> ids = new HashSet<String>();
		Model model = datasetAccessor.getModel(getGraphURI(graphName));
		if (model == null) {
			logger.warn("Graph {} does not exist yet", graphName);
			return ids;
		}
		StmtIterator iter = model.listStatements(null,
				Config.javaClassRDFProperty, entitiesClass.getName());
		while (iter.hasNext()) {
			ids.add(iter
					.nextStatement()
					.getSubject()
					.getProperty(
							new PropertyImpl(Config.entitiesNamespace,
									idPropertyName)).getObject().asLiteral()
					.toString());
		}
		return ids;
	}

	private Class<?> getJavaClass(Resource resource, Model model)
			throws ClassNotFoundException {
		RDFNode rdfValue = getRDFPropertyValue(resource,
				Config.javaClassRDFProperty, model);
		if (rdfValue == null)
			return null;
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
			// properties.put(KBConfig.javaClassProperty,
			// object.getClass().getName());
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

	private String getShortUriFromLocalName(String localname) {
		return Config.uriPrefix + ":" + Util.urlEncode(localname);
	}

	private String prepareQueryFromBody(String graphName, String[] queryBody) {
		String queryString = "PREFIX "
				+ Config.uriPrefix
				+ ":<"
				+ Config.entitiesNamespace
				+ "> PREFIX rdf:<"
				+ RDF.getURI()
				+ "> PREFIX xsd:<"
				+ XSD.getURI()
				+ "> "
				+ "DELETE DATA { "
				+ inGraph(queryBody[DELETE_DATA_INDEX], graphName)
				+ " } ; "
				+ (graphName.equals("default") ? "" : "WITH <"
						+ getGraphURI(graphName) + "> ")
				+ "DELETE { "
				+ queryBody[DELETE_INDEX]
				+ " } WHERE { "
				+ queryBody[WHERE_4_DELETE_INDEX]
				+ " } ; DELETE WHERE { "
				+ inGraph(queryBody[DELETE_WHERE_INDEX], graphName)
				+ " } ; INSERT DATA { "
				+ inGraph(queryBody[INSERT_DATA_INDEX], graphName)
				+ " } ; "
				+ (graphName.equals("default") ? "" : "WITH <"
						+ getGraphURI(graphName) + "> ") + "INSERT { "
				+ queryBody[INSERT_INDEX] + " } WHERE { "
				+ queryBody[WHERE_4_INSERT_INDEX] + " } ";
		return queryString;
	}

	private String inGraph(String body, String graphName) {
		if (graphName.equals("default"))
			return body;
		return "GRAPH <" + getGraphURI(graphName) + "> { " + body + "} ";
	}

	private void prepareAddQueryBody(Object entity, String idPropertyName,
			String entityId, String[] queryBody) throws SerializationException,
			DeserializationException {
		Map<String, Object> properties = getJavaProperties(entity);
		logger.info("Adding new entity with {} {}", idPropertyName, entityId);
		String javaClassName = entity.getClass().getName();
		String javaClassSimpleName = entity.getClass().getSimpleName();
		String subjectUri = getShortUriFromLocalName(entityId);

		queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
				RDF.type.toString(),
				getShortUriFromLocalName(javaClassSimpleName));
		queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subjectUri,
				Config.javaClassRDFProperty.toString(), javaClassName,
				getLiteralDataType(javaClassName));

		for (String property : properties.keySet()) {
			Object value = properties.get(property);
			if (value != null) {
				addNewProperty(subjectUri, getShortUriFromLocalName(property),
						value, idPropertyName, queryBody);
			}
		}
	}

	private String prepareLiteralTriple(String s, String p, String o,
			XSDDatatype dataType) {
		if (s.contains("/"))
			s = "<" + s + ">";
		if (p.contains("/"))
			p = "<" + p + ">";
		return s + " " + p + " \"" + o + "\"^^<" + dataType.getURI() + "> . ";
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

	private Object toJava(RDFNode rdfNode, Model model)
			throws DeserializationException {
		return toJava(rdfNode, model, new HashMap<RDFNode, Object>());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object toJava(RDFNode rdfNode, Model model,
			Map<RDFNode, Object> alreadyDeserialized)
			throws DeserializationException {
		if (alreadyDeserialized.containsKey(rdfNode))
			return alreadyDeserialized.get(rdfNode);
		Object entity = null;
		if (rdfNode.isResource()) {
			StmtIterator propIterator = model
					.listStatements(new SkipPropertiesSelector(rdfNode
							.asResource(), RDF.type,
							Config.javaClassRDFProperty));
			if (rdfNode.asResource().hasProperty(RDF.type, RDF.Bag)) {
				entity = new HashSet();
				alreadyDeserialized.put(rdfNode, entity);
				while (propIterator.hasNext()) {
					((Set) entity).add(toJava(propIterator.next().getObject(),
							model, alreadyDeserialized));
				}
			} else if (rdfNode.asResource().hasProperty(RDF.type, RDF.Seq)) {
				entity = new ArrayList();
				alreadyDeserialized.put(rdfNode, entity);
				while (propIterator.hasNext()) {
					((List) entity).add(toJava(propIterator.next().getObject(),
							model, alreadyDeserialized));
				}
			} else if (rdfNode.asResource().hasProperty(RDF.type,
					Config.MapRDFResource)) {
				entity = new HashMap();
				alreadyDeserialized.put(rdfNode, entity);
				while (propIterator.hasNext()) {
					Resource mapNode = propIterator.next().getObject()
							.asResource();
					RDFNode keyNode = mapNode
							.getProperty(Config.keyRDFProperty).getObject();

					RDFNode valueNode = mapNode.getProperty(
							Config.valueRDFProperty).getObject();
					Object key = toJava(keyNode, model, alreadyDeserialized);
					Object value = toJava(valueNode, model, alreadyDeserialized);
					((Map) entity).put(key, value);
				}
			} else {
				try {
					Class<?> entityClass = getJavaClass(rdfNode.asResource(),
							model);
					if (entityClass == null)
						return null;
					entity = entityClass.newInstance();
					alreadyDeserialized.put(rdfNode, entity);
					Map<String, Object> properties = new HashMap<String, Object>();
					while (propIterator.hasNext()) {
						Statement stmt = propIterator.nextStatement();
						RDFNode rdfObject = stmt.getObject();
						String javaProperty = stmt.getPredicate()
								.getLocalName();
						Object value = toJava(rdfObject, model,
								alreadyDeserialized);
						properties.put(javaProperty, value);
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

	public void uploadOntology(OntModel model, String graphName) {
		datasetAccessor.add(getGraphURI(graphName), model);
	}

	public void clearGraph(String graphName) {
		UpdateRequest query = UpdateFactory.create("CLEAR GRAPH <"
				+ getGraphURI(graphName) + ">", Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

	public void clearAll() {
		UpdateRequest query = UpdateFactory.create("CLEAR ALL",
				Syntax.syntaxSPARQL_11);
		UpdateProcessor execUpdate = UpdateExecutionFactory.createRemote(query,
				getKnowledgeBaseUpdateURL());
		execUpdate.execute();
	}

}
