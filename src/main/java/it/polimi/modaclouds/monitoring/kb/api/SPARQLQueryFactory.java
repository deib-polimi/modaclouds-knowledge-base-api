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

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.beanutils.PropertyUtils;

import com.google.common.base.Preconditions;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.XSD;

public class SPARQLQueryFactory {

	private static final String prologue = "PREFIX " + KbAPI.KB_NS_PREFIX
			+ ":<" + KbAPI.KB_NAME_SPACE + "> PREFIX rdf:<" + RDF.getURI()
			+ "> PREFIX xsd:<" + XSD.getURI() + "> ";

	private static final int DELETE_DATA_INDEX = 0;
	private static final int DELETE_INDEX = 1;
	private static final int DELETE_WHERE_INDEX = 2;
	private static final int WHERE_4_DELETE_INDEX = 3;
	private static final int INSERT_DATA_INDEX = 4;
	private static final int INSERT_INDEX = 5;
	private static final int WHERE_4_INSERT_INDEX = 6;

	public String clearAll() {
		return "CLEAR ALL";
	}

	public String add(Object entity, String idPropertyName, String graphName)
			throws SerializationException {
		Preconditions.checkNotNull(idPropertyName);
		Preconditions.checkNotNull(entity);
		Preconditions.checkNotNull(graphName);
		String entityId = getEntityId(entity, idPropertyName);
		String[] queryBody = getEmptyQueryBody();
		prepareDeleteByPropertyValueQueryBody(entityId, idPropertyName,
				queryBody);
		prepareAddQueryBody(entity, idPropertyName, entityId, queryBody);
		return prologue + prepareQueryFromBody(graphName, queryBody);
	}

	public String addMany(Iterable<?> entities, String idPropertyName,
			String graphName) throws SerializationException {
		Preconditions.checkNotNull(idPropertyName);
		Preconditions.checkNotNull(entities);
		Preconditions.checkNotNull(graphName);
		String query = prologue;
		for (Object entity : entities) {
			String[] queryBody = getEmptyQueryBody();
			String entityId = getEntityId(entity, idPropertyName);
			prepareDeleteByPropertyValueQueryBody(entityId, idPropertyName,
					queryBody);
			prepareAddQueryBody(entity, idPropertyName, entityId, queryBody);
			query += prepareQueryFromBody(graphName, queryBody);
		}
		return query;
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
						KbAPI.MAP_RESOURCE.getURI());
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
						KbAPI.MAP_RESOURCE.getURI())
				+ prepareTriple("?o", "?p1", "?o1")
				+ prepareTriple("?o1", "?p2", "?o2")
				+ prepareTriple("?o1", "?p3", "?o3") + "} ";
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

	private String getShortUriFromLocalName(String localname) {
		return KbAPI.KB_NS_PREFIX + ":" + localname;
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

	public String getEntityId(Object entity, String idPropertyName)
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

	private String[] getEmptyQueryBody() {
		String[] queryBody = { "", "", "", "", "", "", "" };
		return queryBody;
	}

	private void prepareDeleteByPropertyValuesQueryBody(Set<?> propertyValues,
			String propertyName, String[] queryBody) {
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
				+ prepareTriple("?o1", "?p2", "?o2")
				+ prepareTriple("?o1", "?p3", "?o3");
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
						KbAPI.MAP_RESOURCE.getURI())
				+ prepareTriple("?o", "?p1", "?o1")
				+ prepareTriple("?o1", "?p2", "?o2")
				+ prepareTriple("?o1", "?p3", "?o3") + "} ";
	}

	private void prepareAddQueryBody(Object entity, String idPropertyName,
			String entityId, String[] queryBody) throws SerializationException {
		Map<String, Object> properties = getJavaProperties(entity);
		String javaClassName = entity.getClass().getName();
		String javaClassSimpleName = entity.getClass().getSimpleName();
		String subjectUri = getShortUriFromLocalName(entityId);

		queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
				RDF.type.toString(),
				getShortUriFromLocalName(javaClassSimpleName));
		queryBody[INSERT_DATA_INDEX] += prepareLiteralTriple(subjectUri,
				KbAPI.JAVA_CLASS_PROPERTY.getURI().toString(), javaClassName,
				getLiteralDataType(javaClassName));

		for (String property : properties.keySet()) {
			Object value = properties.get(property);
			if (value != null) {
				addNewProperty(subjectUri, getShortUriFromLocalName(property),
						value, idPropertyName, queryBody);
			}
		}
	}

	private void addNewProperty(String subjectUri, String propertyUri,
			Object object, String idPropertyName, String[] queryBody)
			throws SerializationException {
		if (object instanceof Set<?>) {
			String anonId = new AnonId(UUID.randomUUID().toString()).toString();
			String anonUri = KbAPI.KB_NAME_SPACE + anonId;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
					propertyUri, anonUri);
			Set<?> setObjects = (Set<?>) object;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri,
					RDF.type.toString(), RDF.Bag.toString());
			int i = 0;
			for (Object obj : setObjects) {
				if (obj != null)
					addNewProperty(anonUri, RDF.li(i).toString(), obj,
							idPropertyName, queryBody);
				i++;
			}
		} else if (object instanceof List<?>) {
			String anonId = new AnonId(UUID.randomUUID().toString()).toString();
			String anonUri = KbAPI.KB_NAME_SPACE + anonId;
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
			String anonUri = KbAPI.KB_NAME_SPACE + anonId;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(subjectUri,
					propertyUri, anonUri);
			Map<?, ?> mapObjects = (Map<?, ?>) object;
			queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri,
					RDF.type.toString(), KbAPI.MAP_RESOURCE.getURI());
			int i = 0;
			Set<?> keys = mapObjects.keySet();
			for (Object key : keys) {
				String internalAnonId = new AnonId(UUID.randomUUID().toString())
						.toString();
				String internalAnonUri = KbAPI.KB_NAME_SPACE + internalAnonId;
				queryBody[INSERT_DATA_INDEX] += prepareTriple(anonUri, RDF
						.li(i).toString(), internalAnonUri);
				addNewProperty(internalAnonUri,
						KbAPI.MAP_KEY_PROPERTY.getURI(), key, idPropertyName,
						queryBody);
				addNewProperty(internalAnonUri,
						KbAPI.MAP_VALUE_PROPERTY.getURI(), mapObjects.get(key),
						idPropertyName, queryBody);
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

	private boolean isLiteral(Object object) {
		return getLiteralDataType(object) != null;
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

	private String prepareQueryFromBody(String graphName, String[] queryBody) {
		String queryString = "DELETE DATA { "
				+ inGraph(queryBody[DELETE_DATA_INDEX], graphName)
				+ " } ; "
				+ (graphName.equals("default") ? "" : "WITH <" + graphName
						+ "> ")
				+ "DELETE { "
				+ queryBody[DELETE_INDEX]
				+ " } WHERE { "
				+ queryBody[WHERE_4_DELETE_INDEX]
				+ " } ; DELETE WHERE { "
				+ inGraph(queryBody[DELETE_WHERE_INDEX], graphName)
				+ " } ; INSERT DATA { "
				+ inGraph(queryBody[INSERT_DATA_INDEX], graphName)
				+ " } ; "
				+ (graphName.equals("default") ? "" : "WITH <" + graphName
						+ "> ") + "INSERT { " + queryBody[INSERT_INDEX]
				+ " } WHERE { " + queryBody[WHERE_4_INSERT_INDEX] + " } ; ";
		return queryString;
	}

	private String inGraph(String body, String graphName) {
		if (graphName.equals("default"))
			return body;
		return "GRAPH <" + graphName + "> { " + body + "} ";
	}

	public String getEntityById(String id, String idPropertyName,
			String graphName) {
		String query = "PREFIX "
				+ KbAPI.KB_NS_PREFIX
				+ ": <"
				+ KbAPI.KB_NAME_SPACE
				+ "> PREFIX rdf: <"
				+ RDF.getURI()
				+ "> PREFIX xsd: <"
				+ XSD.getURI()
				+ "> CONSTRUCT { ?s ?p ?o . ?o ?p1 ?o1 . ?o1 ?p2 ?o2 . ?o1 ?p3 ?o3 } "
				+ "WHERE { "
				+ (graphName.equals("default") ? "" : "GRAPH <" + graphName
						+ "> ")
				+ " { ?s ?p ?o ; "
				+ getShortUriFromLocalName(idPropertyName)
				+ " \""
				+ id
				+ "\" . OPTIONAL { ?o ?p1 ?o1 ; a rdf:Seq } . OPTIONAL { ?o ?p1 ?o1 ; a rdf:Bag } . "
				+ "OPTIONAL { ?o ?p1 ?o1 ; a <" + KbAPI.MAP_RESOURCE.getURI()
				+ "> . " + "?o1 ?p2 ?o2 . ?o1 ?p3 ?o3 } } } ";
		return query;
	}

	public <T> String getAll(Class<T> entitiesClass, String graphName) {
		String query = "PREFIX "
				+ KbAPI.KB_NS_PREFIX
				+ ": <"
				+ KbAPI.KB_NAME_SPACE
				+ "> PREFIX rdf: <"
				+ RDF.getURI()
				+ "> PREFIX xsd: <"
				+ XSD.getURI()
				+ "> CONSTRUCT { ?s ?p ?o . ?o ?p1 ?o1 . ?o1 ?p2 ?o2 . ?o1 ?p3 ?o3 } "
				+ "WHERE { "
				+ inGraph(
						"?s ?p ?o ; <"
								+ KbAPI.JAVA_CLASS_PROPERTY.getURI()
								+ "> "
								+ (entitiesClass != null ? ("\""
										+ entitiesClass.getName() + "\"")
										: "?class")
								+ " . OPTIONAL { ?o ?p1 ?o1 ; a rdf:Seq } . OPTIONAL { ?o ?p1 ?o1 ; a rdf:Bag } . "
								+ "OPTIONAL { ?o ?p1 ?o1 ; a <"
								+ KbAPI.MAP_RESOURCE.getURI() + "> . "
								+ "?o1 ?p2 ?o2 . ?o1 ?p3 ?o3 } ", graphName)
				+ "} ";
		return query;
	}

	public String deleteEntitiesByPropertyValue(Object propertyValue,
			String propertyName, String graphName) {
		String[] queryBody = getEmptyQueryBody();
		prepareDeleteByPropertyValueQueryBody(propertyValue, propertyName,
				queryBody);
		return prologue + prepareQueryFromBody(graphName, queryBody);
	}

	public String deleteEntitiesByPropertyValues(Set<?> propertyValues,
			String propertyName, String graphName) {
		Preconditions.checkNotNull(propertyValues);
		Preconditions.checkNotNull(propertyName);
		Preconditions.checkArgument(!propertyValues.isEmpty());

		String[] queryBody = getEmptyQueryBody();

		prepareDeleteByPropertyValuesQueryBody(propertyValues, propertyName,
				queryBody);

		return prologue + prepareQueryFromBody(graphName, queryBody);
	}

	public String clearGraph(String graphName) {
		return "CLEAR GRAPH <" + graphName + ">";
	}

	public String putModel(OntModel model, String graphName) {
		Writer triples = new StringWriter();
		model.write(triples, "N-TRIPLE");
		String queryString = "INSERT DATA { "
				+ inGraph(triples.toString(), graphName) + " }";
		return queryString;
	}

}
