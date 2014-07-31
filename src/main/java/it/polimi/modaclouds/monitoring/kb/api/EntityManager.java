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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.vocabulary.RDF;

public class EntityManager {

	private static Logger logger = LoggerFactory.getLogger(EntityManager.class);

	public static KBEntity toJava(Resource r, Model model, String class_package) {
		KBEntity entity = null;
		if (model.contains(r, null, (RDFNode) null)) {
			try {
				Class<? extends KBEntity> entityClass = getJavaClass(r, model, class_package);
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
					addProperty(model, stmt, javaProperty, rdfObject, properties);
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void addProperty(Model model, Statement statement, String javaProperty, Object kbObject, Map<String, Object> properties) { 
		//add control for List and Map, and update the Set control. Controllare se sono seq, bag o map nel file RDF
		if(statement.getObject().isResource()){
			StmtIterator stmtIterator = model.listStatements(statement.getObject().asResource(), RDF.type, (RDFNode) null);
			while (stmtIterator.hasNext()) {
				Statement innerStatement = stmtIterator.next();
				if(innerStatement.getObject().asResource().equals(RDF.Bag)){
					Set set = (Set) properties.get(javaProperty);
					if (set == null) {
						set = new HashSet();
						properties.put(javaProperty, set);
					}
					StmtIterator stmtIterator2 = model.listStatements(new SkipTypeSelector(statement.getObject().asResource()));
					while (stmtIterator2.hasNext()){
						Statement innerStatement2 = stmtIterator2.next();
						set.add(innerStatement2.getObject().toString());

					}	
					properties.put(javaProperty, set);

				} else if(innerStatement.getObject().asResource().equals(RDF.Seq)) {
					StmtIterator stmtIterator2 = model.listStatements(new SkipTypeSelector(statement.getObject().asResource()));
					List list = (List) properties.get(javaProperty);
					if (list == null) {
						list = new ArrayList<>();
						properties.put(javaProperty, list);
					}
					while (stmtIterator2.hasNext()){
						Statement innerStatement2 = stmtIterator2.next();
						list.add(innerStatement2.getObject().toString());
					}		
					properties.put(javaProperty, list);

				} else if (innerStatement.getObject().asResource().equals(new ResourceImpl(KBEntity.uriBase + "Map"))) {
					StmtIterator stmtIterator2 = model.listStatements(new SkipTypeSelector(statement.getObject().asResource()));
					Map map = (Map) properties.get(javaProperty);
					if (map == null) {
						map = new HashMap<>();
						properties.put(javaProperty, map);
					}
					while (stmtIterator2.hasNext()){
						Statement innerStatement2 = stmtIterator2.next();
						StmtIterator stmtIterator3 = model.listStatements(innerStatement2.getObject().asResource(), null, (RDFNode) null);
						String key = new String();
						String value = new String();
						while (stmtIterator3.hasNext()){
							Statement innerStatement3 = stmtIterator3.next();
							if(innerStatement3.getPredicate().equals(new PropertyImpl(KBEntity.uriBase + "key")))
								key = innerStatement3.getObject().toString();
							if(innerStatement3.getPredicate().equals(new PropertyImpl(KBEntity.uriBase + "value")))
								value = innerStatement3.getObject().toString();	
						}
						map.put(key, value);
					}		
					properties.put(javaProperty, map);
				}	
			}			
		} else {
			properties.put(javaProperty, kbObject);
		}
		System.out.println();
	}

	private static String toJavaName(String stringURI) {
		String javaName = null;
		try {
			URI uri = new URI(stringURI);
			String uriStr = uri.toString();
			if(uri.toString().contains("#"))
				javaName = uriStr.substring(uriStr.lastIndexOf("#") + 1, uriStr.length());
			else
				javaName = uriStr.substring(uriStr.lastIndexOf("/") + 1, uriStr.length());
		} catch (URISyntaxException e) {
			logger.error("Error while converting uri to java name", e);
		}
		return javaName;
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends KBEntity> getJavaClass(Resource resource, Model model, String class_package) {
		Resource objectType = model.listObjectsOfProperty(resource, RDF.type)
				.next().asResource();
		String className = toJavaName(objectType.getURI());
		Class<? extends KBEntity> objectClass = null;
		try {
			objectClass = (Class<? extends KBEntity>) Class
					.forName(class_package + className);
		} catch (ClassNotFoundException e) {
			logger.error("error while creating class from name", e);
		}
		return objectClass;
	}

	static <T extends KBEntity> String getKBClassURI(Class<T> javaClass) {
		return KBEntity.uriBase + javaClass.getSimpleName();
	}

	public static String getKBPropertyURI(String property) {
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
