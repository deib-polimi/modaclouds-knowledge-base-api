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
package it.polimi.modaclouds.monitoring.kb.test;

import java.io.File;

import org.apache.jena.fuseki.server.FusekiConfig;
import org.apache.jena.fuseki.server.SPARQLServer;
import org.apache.jena.fuseki.server.ServerConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ KBTest.class })
public class AllTests {

	private static SPARQLServer fusekiServer;

	@BeforeClass
	public static void init() {
		File datasetDir = new File("target/generated-test-resources/dataset");
		datasetDir.mkdirs();
		ServerConfig config = FusekiConfig.configure(AllTests.class
				.getResource("/moda_fuseki_configuration.ttl").getFile());
		fusekiServer = new SPARQLServer(config);
		fusekiServer.start();
	}

	@AfterClass
	public static void teardown() {
		fusekiServer.stop();
	}

}
