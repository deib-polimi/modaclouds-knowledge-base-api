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

import it.polimi.deib.csparql_rest_api.RSP_services_csparql_API;
import it.polimi.deib.csparql_rest_api.exception.QueryErrorException;
import it.polimi.deib.csparql_rest_api.exception.ServerErrorException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

public class RspKbAPI extends KbAPI {

	private static final Logger logger = LoggerFactory
			.getLogger(RspKbAPI.class);

	private RSP_services_csparql_API rsp;

	public RspKbAPI(String rspServerAddress) {
		this.rsp = new RSP_services_csparql_API(rspServerAddress);
	}

	@Override
	public void clearAll() throws Exception {
		rsp.launchUpdateQuery(sqf.clearAll());
	}

	@Override
	public void add(Object entity, String idPropertyName, String graphName)
			throws SerializationException, IOException {
		String query = sqf.add(entity, idPropertyName, graphName);
		try {
			rsp.launchUpdateQuery(query);
		} catch (QueryErrorException e) {
			throw new SerializationException(e);
		} catch (ServerErrorException e) {
			throw new IOException();
		}
	}

	@Override
	public void add(Object entity, String idPropertyName)
			throws SerializationException, IOException {
		add(entity, idPropertyName, "default");
	}
	
	@Override
	public void addMany(Iterable<?> entities, String idPropertyName,
			String graphName) throws SerializationException, IOException {
		String query = sqf.addMany(entities, idPropertyName, graphName);
		try {
			rsp.launchUpdateQuery(query);
		} catch (QueryErrorException e) {
			throw new SerializationException(e);
		} catch (ServerErrorException e) {
			throw new IOException();
		}
	}

	public Object getEntityById(String id, String idPropertyName)
			throws DeserializationException, IOException {
		return getEntityById(id, idPropertyName, "default");
	}

	@Override
	public Object getEntityById(String id, String idPropertyName,
			String graphName) throws DeserializationException, IOException {
		String query = sqf.getEntityById(id, idPropertyName, graphName);
		String jsonModel;
		try {
			jsonModel = rsp.evaluateSparqlQuery(query);
			logger.debug("Retrieved json model: \n{}", jsonModel);
		} catch (ServerErrorException e) {
			throw new IOException();
		} catch (QueryErrorException e) {
			throw new DeserializationException(e);
		}
		Model model = ModelFactory.createDefaultModel();
		InputStream is = new ByteArrayInputStream(jsonModel.getBytes());
		model.read(is, null, "RDF/JSON");
		// model.write(System.out,"TTL");
		Resource resource = getRDFResourceByPropertyValue(new PropertyImpl(
				KB_NAME_SPACE, idPropertyName), id, model);
		if (resource == null) {
			logger.info("No entity found with {} {} on the KB", idPropertyName,
					id);
			return null;
		}
		return toJava(resource, model);
	}

	@Override
	public void deleteEntitiesByPropertyValue(Object propertyValue,
			String propertyName) throws IOException {
		deleteEntitiesByPropertyValue(propertyValue, propertyName, "default");
	}

	@Override
	public void deleteEntitiesByPropertyValue(Object propertyValue,
			String propertyName, String graphName) throws IOException {
		String query = sqf.deleteEntitiesByPropertyValue(propertyValue,
				propertyName, graphName);
		try {
			rsp.launchUpdateQuery(query);
		} catch (ServerErrorException e) {
			throw new IOException();
		} catch (QueryErrorException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void deleteEntitiesByPropertyValues(Set<?> propertyValues,
			String propertyName, String graphName) throws IOException {
		String query = sqf.deleteEntitiesByPropertyValues(propertyValues,
				propertyName, graphName);
		try {
			rsp.launchUpdateQuery(query);
		} catch (ServerErrorException e) {
			throw new IOException();
		} catch (QueryErrorException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Set<T> getAll(Class<T> entitiesClass, String graphName)
			throws DeserializationException, IOException {
		String query = sqf.getAll(entitiesClass, graphName);
		String jsonModel;
		try {
			jsonModel = rsp.evaluateSparqlQuery(query);
			logger.debug("Retrieved json model: \n{}", jsonModel);
		} catch (ServerErrorException e) {
			throw new IOException();
		} catch (QueryErrorException e) {
			throw new DeserializationException(e);
		}
		Model model = ModelFactory.createDefaultModel();
		InputStream is = new ByteArrayInputStream(jsonModel.getBytes());
		model.read(is, null, "RDF/JSON");
		Set<T> entities = new HashSet<T>();
		StmtIterator iter = model.listStatements(null,
				JAVA_CLASS_PROPERTY,
				entitiesClass != null ? entitiesClass.getName() : null);
		while (iter.hasNext()) {
			entities.add((T) toJava(iter.next().getSubject(), model));
		}
		return entities;
	}

	@Override
	public Object getAll(String graphName)
			throws DeserializationException, IOException {
		String query = sqf.getAll(null, graphName);
		String jsonModel;
		try {
			jsonModel = rsp.evaluateSparqlQuery(query);
			logger.debug("Retrieved json model: \n{}", jsonModel);
		} catch (ServerErrorException e) {
			throw new IOException();
		} catch (QueryErrorException e) {
			throw new DeserializationException(e);
		}
		Model model = ModelFactory.createDefaultModel();
		InputStream is = new ByteArrayInputStream(jsonModel.getBytes());
		model.read(is, null, "RDF/JSON");
		Set<Object> entities = new HashSet<Object>();
		StmtIterator iter = model.listStatements(null,
				JAVA_CLASS_PROPERTY, (RDFNode) null);
		while (iter.hasNext()) {
			entities.add(toJava(iter.next().getSubject(), model));
		}
		return entities;
	}

	@Override
	public void clearGraph(String graphName) throws IOException {
		try {
			rsp.launchUpdateQuery(sqf.clearGraph(graphName));
		} catch (ServerErrorException e) {
			throw new IOException();
		} catch (QueryErrorException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void putModel(OntModel model, String graphName) throws IOException {
		try {
			rsp.launchUpdateQuery(sqf.putModel(model, graphName));
		} catch (ServerErrorException e) {
			throw new IOException();
		} catch (QueryErrorException e) {
			throw new RuntimeException(e);
		}
	}

	

}
