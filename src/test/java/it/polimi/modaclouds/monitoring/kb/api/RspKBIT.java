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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.polimi.deib.csparql_rest_api.RSP_services_csparql_API;
import it.polimi.deib.rsp_services_csparql.server.rsp_services_csparql_server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class RspKBIT {

	private static Thread rspThread;
	private KbAPI kb;
	private RSP_services_csparql_API rsp;
	private static final int rspPort = Util.findFreePort();

	@BeforeClass
	public static void setUpBeforeClass() {
		rspThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String[] rspArgs = new String[1];
					rspArgs[0] = "test-setup.properties";
					System.setProperty("rsp_server.static_resources.path",
							"/tmp/kb");
					System.setProperty("csparql_server.port",
							Integer.toString(rspPort));
					System.setProperty("log4j.configuration", "log4j.properties");
					rsp_services_csparql_server.main(rspArgs);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		rspThread.start();
	}

	@AfterClass
	public static void tearDownAfterClass() {
		rspThread.interrupt();
	}

	@Before
	public void setUp() throws Exception {
		Util.waitForResponseCode("http://localhost:" + rspPort + "/queries", 200, 5, 5000);
		kb = new RspKbAPI("http://localhost:" + rspPort);
		rsp = new RSP_services_csparql_API("http://localhost:" + rspPort);
		kb.clearAll();
	}

	@Test
	public void clearAllShouldClearAll() throws Exception {
		rsp.launchUpdateQuery("INSERT DATA { GRAPH <http://example.org/graph> { <http://example.org#s2> <http://example.org#p2> <http://example.org#o2> } }");
		String json1 = rsp
				.evaluateSparqlQuery("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <http://example.org/graph> { ?s ?p ?o } }");
		assertTrue(!json1.matches("\\s*\\{\\s*\\}\\s*"));
		kb.clearAll();
		String json2 = rsp
				.evaluateSparqlQuery("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <http://example.org/graph> { ?s ?p ?o } }");
		assertTrue(json2.matches("\\s*\\{\\s*\\}\\s*"));
	}

	@Test
	public void shouldFindWhatIPutSimple() throws SerializationException,
			IOException, DeserializationException {
		MySimpleEntity entity1 = new MySimpleEntity();
		entity1.setId("entity1");
		kb.add(entity1, "id");
		Object retrievedEntity1 = kb.getEntityById("entity1", "id");
		assertTrue(retrievedEntity1 instanceof MySimpleEntity);
		assertEquals(entity1.getId(),
				((MySimpleEntity) retrievedEntity1).getId());
	}

	@Test
	public void shouldFindWhatIPut() throws Exception {
		MyEntity entity1 = new MyEntity();
		entity1.setId("entity1");
		entity1.addStringToSet("el0");
		entity1.addStringToSet("el1");
		kb.add(entity1, "id");
		Object retrievedEntity1 = kb.getEntityById("entity1", "id");
		assertTrue(((MyEntity) retrievedEntity1).getId().equals("entity1"));
		assertTrue(((MyEntity) retrievedEntity1).getSetString().contains("el0"));
		assertTrue(((MyEntity) retrievedEntity1).getSetString().contains("el1"));
	}

	@Test
	public void test() {
		Model model = ModelFactory.createDefaultModel();
		String str = "{\"http://www.modaclouds.eu/rdfs/1.0/entities#entity2\":{\"http://www.modaclouds.eu/rdfs/1.0/entities#class\":[{\"type\":\"literal\",\"value\":\"it.polimi.modaclouds.monitoring.kb.api.MyEntity\",\"datatype\":\"http://www.w3.org/2001/XMLSchema#string\"}],\"http://www.modaclouds.eu/rdfs/1.0/entities#set\":[{\"type\":\"uri\",\"value\":\"http://www.modaclouds.eu/rdfs/1.0/entities#9576359d-6315-4de0-bb87-8e1ac115327d\"}],\"http://www.modaclouds.eu/rdfs/1.0/entities#list\":[{\"type\":\"uri\",\"value\":\"http://www.modaclouds.eu/rdfs/1.0/entities#866908df-1c8f-4271-8359-24e25d75f85c\"}],\"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\":[{\"type\":\"uri\",\"value\":\"http://www.modaclouds.eu/rdfs/1.0/entities#MyEntity\"}],\"http://www.modaclouds.eu/rdfs/1.0/entities#id\":[{\"type\":\"literal\",\"value\":\"entity2\",\"datatype\":\"http://www.w3.org/2001/XMLSchema#string\"}],\"http://www.modaclouds.eu/rdfs/1.0/entities#map\":[{\"type\":\"uri\",\"value\":\"http://www.modaclouds.eu/rdfs/1.0/entities#decf1090-d825-4601-b875-d9db3354fa82\"}]}}";
		InputStream is = new ByteArrayInputStream(str.getBytes());
		model.read(is, null, "RDF/JSON");
		assertTrue(!model.isEmpty());
	}

	@Test
	public void shouldBeEmptyAfterRemove() throws Exception {
		MyEntity entity1 = new MyEntity();
		entity1.setId("entity1");
		entity1.addStringToSet("el0");
		entity1.setString("provaString");
		kb.add(entity1, "id");
		kb.deleteEntitiesByPropertyValue("entity1", "id");
		String json2 = rsp
				.evaluateSparqlQuery("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
		assertTrue(json2.matches("\\s*\\{\\s*\\}\\s*"));
	}

	@Test
	public void shoudlOverwriteOnSameId() throws Exception {
		MyEntity entity1 = new MyEntity();
		entity1.setId("entity1");
		entity1.setEntity(entity1);
		entity1.addStringToSet("el0");
		entity1.addStringToSet("el1");
		kb.add(entity1, "id");
		MyEntity entity2 = new MyEntity();
		entity2.setId("entity1");
		kb.add(entity2, "id");
		assertTrue(((MyEntity) kb.getEntityById("entity1", "id")).getSet()
				.isEmpty());
		kb.deleteEntitiesByPropertyValue("entity1", "id");
		String json2 = rsp
				.evaluateSparqlQuery("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <http://example.org/graph> { ?s ?p ?o } }");
		assertTrue(json2.matches("\\s*\\{\\s*\\}\\s*"));
	}

	@Test
	public void shouldFindWhatIPutWhenAddingMany() throws Exception {
		MyEntity entity1 = new MyEntity();
		entity1.setId("entity1");
		entity1.addStringToSet("el0");
		entity1.addStringToSet("el1");
		MyEntity entity2 = new MyEntity();
		entity2.setId("entity2");
		entity2.addStringToSet("el2");
		kb.addMany(Arrays.asList(new MyEntity[] { entity1, entity2 }), "id",
				"default");
		Object retrievedEntity1 = kb.getEntityById("entity1", "id");
		assertTrue(((MyEntity) retrievedEntity1).getId().equals("entity1"));
		assertTrue(((MyEntity) retrievedEntity1).getSetString().contains("el0"));
		assertTrue(((MyEntity) retrievedEntity1).getSetString().contains("el1"));
		Object retrievedEntity2 = kb.getEntityById("entity2", "id");
		assertTrue(((MyEntity) retrievedEntity2).getId().equals("entity2"));
		assertTrue(((MyEntity) retrievedEntity2).getSetString().contains("el2"));
	}

	@Test
	public void shoudlOverwriteOnSameWhenAddingMany() throws Exception {
		MyEntity entity1 = new MyEntity();
		entity1.setId("entity1");
		entity1.setEntity(entity1);
		entity1.addStringToSet("el0");
		entity1.addStringToSet("el1");
		MyEntity entity2 = new MyEntity();
		entity2.setId("entity1");
		kb.addMany(Arrays.asList(new MyEntity[] { entity1, entity2 }), "id",
				"default");
		assertTrue(((MyEntity) kb.getEntityById("entity1", "id")).getSet()
				.isEmpty());
		kb.deleteEntitiesByPropertyValue("entity1", "id");
		String json2 = rsp
				.evaluateSparqlQuery("CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <http://example.org/graph> { ?s ?p ?o } }");
		assertTrue(json2.matches("\\s*\\{\\s*\\}\\s*"));
	}

	@After
	public void tearDown() throws Exception {
		kb.clearAll();
	}

}
