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

import it.polimi.modaclouds.monitoring.kb.api.KBConnector;
import it.polimi.modaclouds.monitoring.objectstoreapi.ObjectStoreConnector;
import it.polimi.modaclouds.qos_models.monitoring_ontology.MO;
import it.polimi.modaclouds.qos_models.monitoring_ontology.MonitorableResource;
import it.polimi.modaclouds.qos_models.monitoring_ontology.Parameter;
import it.polimi.modaclouds.qos_models.monitoring_ontology.StatisticalDataAnalyzer;
import it.polimi.modaclouds.qos_models.monitoring_ontology.VM;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class KBTest {

	private KBConnector kbConnector;

	@Before
	public void init() {
		ObjectStoreConnector objectStoreConnector = null;
		try {
			objectStoreConnector = ObjectStoreConnector.getInstance();
			MO.setKnowledgeBaseURL(objectStoreConnector.getKBUrl());
			kbConnector = KBConnector.getInstance();
			kbConnector.setKbURL(new URL(MO.getKnowledgeBaseDataURL()));
		} catch (MalformedURLException | FileNotFoundException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testInstallSDA() {
		StatisticalDataAnalyzer ftsSDA = new StatisticalDataAnalyzer();
		ftsSDA.setId(UUID.randomUUID().toString());
		ftsSDA.setType("ForecastingTimeSeries");
		ftsSDA.setMethod("AR");
		ftsSDA.setPeriod("60");
		ftsSDA.setReturnedMetric("CpuUtilizationForecast");
		ftsSDA.setTargetMetric("CpuUtilization");
		ftsSDA.setUrl("http://localhost:8800");
		
		List<MonitorableResource> targetResources = new ArrayList<MonitorableResource>();
		VM vm = new VM();
		vm.setType("FrontendVM");
		targetResources.add(vm);
		ftsSDA.setTargetResources(targetResources);
		
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(new Parameter("forecastPeriod","60"));
		parameters.add(new Parameter("oreder","1"));
		ftsSDA.setParameters(parameters);

		kbConnector.add(ftsSDA);
	}

	@Test
	public void testRetrieveSDAs() {
		List<StatisticalDataAnalyzer> sdas = kbConnector.getAll(StatisticalDataAnalyzer.class);
		if (sdas != null) {
			for (StatisticalDataAnalyzer sda : sdas) {
				sda.setStarted(true);
				kbConnector.add(sda);
			}
		}
	}

}
