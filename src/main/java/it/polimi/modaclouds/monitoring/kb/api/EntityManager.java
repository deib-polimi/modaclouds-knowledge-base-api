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

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

public class EntityManager {

	private static Logger logger = LoggerFactory.getLogger(EntityManager.class);

	// THIS INFO SHOULD BE SAVED IN THE KB
	private static Set<String> setProperties = new HashSet<String>();

	static { // NEED TO BREAK THIS DEPENDENCY WITH MODACLOUDS ONTOLOGY
		setProperties.add("requiredComponents");
		setProperties.add("providedMethods");
		setProperties.add("monitoredResources");
		setProperties.add("inputResources");
	}

	public static KBEntity toJava(Resource r, Model model) {
		KBEntity entity = null;
		if (model.contains(r, null, (RDFNode) null)) {
			try {
				Class<? extends KBEntity> entityClass = getJavaClass(r, model);
				entity = entityClass.newInstance();
				entity.setURI(new URI(r.getURI()));
				StmtIterator stmtIterator = model
						.listStatements(new SkipTypeSelector(r));
				Map<String, Object> properties = new HashMap<String, Object>();
				while (stmtIterator.hasNext()) {
					Statement stmt = stmtIterator.nextStatement();
					RDFNode rdfObject = stmt.getObject();
					String javaProperty = toJavaName(stmt.getPredicate()
							.toString());
					if (rdfObject.isResource()) {
						// TODO add the URI instead of a KBEntity
						Resource resourceObject = rdfObject.asResource();
						KBEntity kbObject = toJava(resourceObject, model);
						addProperty(javaProperty, kbObject, properties);
					} else if (rdfObject.asLiteral().getDatatype() == null) { // plain
						addProperty(javaProperty, rdfObject.asLiteral()
								.getValue(), properties);
					} else if (rdfObject.asLiteral().getDatatype() == XSDDatatype.XSDboolean) {
						addProperty(javaProperty, rdfObject.asLiteral()
								.getBoolean(), properties);
					} else if (rdfObject.asLiteral().getDatatype() == XSDDatatype.XSDint) {
						addProperty(javaProperty, rdfObject.asLiteral()
								.getInt(), properties);
					}
				}
				BeanUtils.populate(entity, properties);
			} catch (InstantiationException | IllegalAccessException
					| URISyntaxException e) {
				logger.error(
						"Error while creating a new instance of an entity", e);
			} catch (InvocationTargetException e) {
				logger.error("Error while populating entity");
			}
		}
		return entity;
	}

//	private static String getIdFromURI(String uri)
//			throws UnsupportedEncodingException {
//		return URLDecoder.decode(uri.substring(uri.lastIndexOf("/") + 1),
//				"UTF-8");
//	}

	private static void addProperty(String javaProperty, Object kbObject,
			Map<String, Object> properties) {
		if (isSet(javaProperty)) {
			Set set = (Set) properties.get(javaProperty);
			if (set == null) {
				set = new HashSet();
				properties.put(javaProperty, set);
			}
			set.add(kbObject);
		} else {
			properties.put(javaProperty, kbObject);
		}
	}

	// TODO need to find another way: example, use reflections with the java
	// class and instance of
	private static boolean isSet(String javaProperty) {
		return setProperties.contains(javaProperty);
	}

	private static String toJavaName(String stringURI) {
		String javaName = null;
		try {
			URI uri = new URI(stringURI);
			String[] segments = uri.getPath().split("/");
			javaName = segments[segments.length - 1];
		} catch (URISyntaxException e) {
			logger.error("Error while converting uri to java name", e);
		}
		return javaName;
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends KBEntity> getJavaClass(Resource resource,
			Model model) {
		Resource objectType = model.listObjectsOfProperty(resource, RDF.type)
				.next().asResource();
		String className = toJavaName(objectType.getURI());
		Class<? extends KBEntity> objectClass = null;
		try {
			objectClass = (Class<? extends KBEntity>) Class
					.forName("it.polimi.modaclouds.qos_models.monitoring_ontology."
							+ className);
		} catch (ClassNotFoundException e) {
			logger.error("error while creating class from name", e);
		}
		return objectClass;
	}

	static <T extends KBEntity> String getKBClassURI(Class<T> javaClass) {
		return KBEntity.uriBase + javaClass.getSimpleName();
	}

	static String getKBPropertyURI(String property) {
		return KBEntity.uriBase + property;
	}

	private static class SkipTypeSelector extends SimpleSelector {

		public SkipTypeSelector(Resource subject) {
			super();
			this.subject = subject;
		}

		public boolean selects(Statement s) {
			return s.getSubject().equals(subject)
					&& !s.getPredicate().equals(RDF.type);
		}

	}

}
