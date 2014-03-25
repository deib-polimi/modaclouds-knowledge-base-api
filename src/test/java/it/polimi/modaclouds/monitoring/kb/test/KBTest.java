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
import it.polimi.modaclouds.monitoring.kb.dto.ForecastingTimeSeries;
import it.polimi.modaclouds.monitoring.kb.dto.SDA;
import it.polimi.modaclouds.monitoring.objectstoreapi.ObjectStoreConnector;
import it.polimi.modaclouds.qos_models.monitoring_ontology.MO;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;

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
		} catch (MalformedURLException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public void testInstallSDA() {
		ForecastingTimeSeries fSda = new ForecastingTimeSeries();
		fSda.setId(UUID.randomUUID().toString());
		fSda.setForecastPeriod("60");
		fSda.setMethod("AR");
		fSda.setOrder("1");
		fSda.setPeriod("60");
		fSda.setReturnedMetric("CpuUtilizationForecast");
		fSda.setTargetMetric("CpuUtilization");
		fSda.setTargetResource("vm1");
		fSda.setUrl("http://localhost:8800");
		
		kbConnector.installSDA(fSda);
	}
	
	public void testRetrieveSDAs() {
		List<SDA> sdas = kbConnector.getInstalledSDAs();
		for (SDA sda: sdas) {
			kbConnector.setStarted(sda);
		}
	}
	
	

}
