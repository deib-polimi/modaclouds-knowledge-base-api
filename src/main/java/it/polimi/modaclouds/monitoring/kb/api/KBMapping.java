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

public class KBMapping {

	private static Map<String, String> java2kb = new HashMap<String, String>();
	private static Map<String, String> kb2java = new HashMap<String, String>();
	private static Logger logger = LoggerFactory.getLogger(KBMapping.class
			.getName());

	static { // these are lists
		java2kb.put("locations", "location");
		java2kb.put("targetResources", "targetResource");
		java2kb.put("parameters", "parameter");
		java2kb.put("instantiatedSDAs", "instantiatedSDA");
		java2kb.put("calledMethods", "calledMethod");
		java2kb.put("requiredComponents", "requiredComponent");
		java2kb.put("providedMethods", "providedMethod");
		java2kb.put("instantiatedDCs", "instantiatedDC");
		java2kb.put("availableAggregateFunctions", "availableAggregateFunction");
		for (String javaName : java2kb.keySet()) {
			kb2java.put(java2kb.get(javaName), javaName);
		}
	}

	public static String toKB(String javaName, boolean usePrefix) {
		String kbName = java2kb.get(javaName);
		if (kbName == null)
			kbName = javaName;
		return (usePrefix ? (MO.prefix + ":") : MO.URI) + kbName;
	}

	public static <T extends KBEntity> T toJava(Resource r, Model model,
			Class<T> entityClass) {
		T entity = null;
		// for (StmtIterator iter = model.listStatements(); iter.hasNext();) {
		// System.out.println(iter.nextStatement().toString());
		// }
		if (model.contains(r, null, (RDFNode) null)) {
			try {
				entity = entityClass.newInstance();
				entity.setUri(r.getURI());
				StmtIterator stmtIterator = model
						.listStatements(new MySelector(r));
				Map<String, Object> properties = new HashMap<String, Object>();
				while (stmtIterator.hasNext()) {
					Statement stmt = stmtIterator.nextStatement();
					RDFNode rdfObject = stmt.getObject();
					String javaProperty = toJavaName(stmt.getPredicate()
							.toString());
					if (rdfObject.isResource()) {
						Resource resourceObject = rdfObject.asResource();
						Class<? extends KBEntity> objectClass = getJavaClass(
								resourceObject, model);
						Object kbObject = toJava(resourceObject, model,
								objectClass);
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
			} catch (InstantiationException | IllegalAccessException e) {
				logger.error(
						"Error while creating a new instance of an entity", e);
			} catch (InvocationTargetException e) {
				logger.error("Error while populating entity");
			}
		}
		return entity;
	}

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

	private static boolean isSet(String javaProperty) {
		return java2kb.keySet().contains(javaProperty);
	}

	private static String toJavaName(String stringURI) {
		String javaName = null;
		try {
			URI uri = new URI(stringURI);
			String[] segments = uri.getPath().split("/");
			javaName = kb2java.get(segments[segments.length - 1]);
			if (javaName == null)
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

	private static class MySelector extends SimpleSelector {

		public MySelector(Resource subject) {
			super();
			this.subject = subject;
		}

		public boolean selects(Statement s) {
			return s.getSubject().equals(subject)
					&& !s.getPredicate().equals(RDF.type);
		}

	}

}
