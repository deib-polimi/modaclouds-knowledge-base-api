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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;

import com.hp.hpl.jena.ontology.OntModel;
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
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.shared.NotFoundException;
import com.hp.hpl.jena.vocabulary.RDF;

public abstract class KbAPI {

	public static String KB_NAME_SPACE = "http://www.modaclouds.eu/kbapi#";
	public static String KB_NS_PREFIX = "kb";

	static final String JAVA_CLASS_PROPERTY_NAME = "class";
	static final Property JAVA_CLASS_PROPERTY = new PropertyImpl(KB_NAME_SPACE,
			JAVA_CLASS_PROPERTY_NAME);
	static final Property MAP_KEY_PROPERTY = new PropertyImpl(KB_NAME_SPACE,
			"key");
	static final Property MAP_VALUE_PROPERTY = new PropertyImpl(KB_NAME_SPACE,
			"value");
	static final Resource MAP_RESOURCE = new ResourceImpl(KB_NAME_SPACE + "Map");

	protected SPARQLQueryFactory sqf;

	public KbAPI() {
		sqf = new SPARQLQueryFactory();
	}

	public abstract void clearAll() throws Exception;

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
	 * @throws IOException
	 */
	public abstract void add(Object entity, String idPropertyName)
			throws SerializationException, IOException;

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
	 * @throws IOException
	 */
	public abstract void add(Object entity, String idPropertyName,
			String graphName) throws SerializationException, IOException;

	public abstract void addMany(Iterable<?> entities, String idPropertyName,
			String graphName) throws SerializationException, IOException;

	public abstract Object getEntityById(String id, String idPropertyName,
			String graphName) throws DeserializationException, IOException,
			NotFoundException;

	public abstract Object getEntityById(String id, String idPropertyName)
			throws DeserializationException, IOException, NotFoundException;

	public abstract void deleteEntitiesByPropertyValue(Object propertyValue,
			String propertyName) throws IOException;

	public abstract void deleteEntitiesByPropertyValue(Object propertyValue,
			String propertyName, String graphName) throws IOException;

	public abstract void deleteEntitiesByPropertyValues(
			Set<?> propertyValues, String propertyName, String graphName)
			throws IOException;

	public abstract <T> Set<T> getAll(Class<T> entitiesClass, String graphName)
			throws DeserializationException, IOException;

	public abstract void clearGraph(String graphName) throws IOException;

	public abstract void putModel(OntModel model, String graphName)
			throws IOException;

	public abstract Object getAll(String graphName)
			throws DeserializationException, IOException;

	protected Object toJava(RDFNode rdfNode, Model model)
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
							.asResource(), RDF.type, JAVA_CLASS_PROPERTY));
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
			} else if (rdfNode.asResource().hasProperty(RDF.type, MAP_RESOURCE)) {
				entity = new HashMap();
				alreadyDeserialized.put(rdfNode, entity);
				while (propIterator.hasNext()) {
					Resource mapNode = propIterator.next().getObject()
							.asResource();
					RDFNode keyNode = mapNode.getProperty(MAP_KEY_PROPERTY)
							.getObject();

					RDFNode valueNode = mapNode.getProperty(MAP_VALUE_PROPERTY)
							.getObject();
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

	private Class<?> getJavaClass(Resource resource, Model model)
			throws ClassNotFoundException {
		RDFNode rdfValue = getRDFPropertyValue(resource, JAVA_CLASS_PROPERTY,
				model);
		if (rdfValue == null)
			return null;
		String javaClass = rdfValue.asLiteral().getString();
		return Class.forName(javaClass);
	}

	private RDFNode getRDFPropertyValue(Resource resource, Property property,
			Model model) {
		NodeIterator iterator = model.listObjectsOfProperty(resource, property);
		return iterator.hasNext() ? iterator.next() : null;
	}

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

	protected Resource getRDFResourceByPropertyValue(Property property,
			String value, Model model) {
		ResIterator iterator = model.listSubjectsWithProperty(property, value);
		return iterator.hasNext() ? iterator.next() : null;
	}
}
