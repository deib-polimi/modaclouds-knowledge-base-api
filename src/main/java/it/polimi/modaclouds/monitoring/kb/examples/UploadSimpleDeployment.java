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
package it.polimi.modaclouds.monitoring.kb.examples;

import it.polimi.modaclouds.monitoring.kb.api.KBConnector;
import it.polimi.modaclouds.qos_models.monitoring_ontology.VM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadSimpleDeployment {

	private static Logger logger = LoggerFactory
			.getLogger(UploadSimpleDeployment.class.getName());


	public static void main(String[] args) {
		try {
			KBConnector knowledgeBase = KBConnector.getInstance();
			
			VM amazonVM = new VM();
			amazonVM.setUri("http://www.modaclouds.eu/rdfs/1.0/monitoring/FrontendVM-1");
			amazonVM.setId("FrontendVM");
			amazonVM.setCloudProvider("Amazon");
			amazonVM.setNumberOfCpus(1);
			amazonVM.setStarted(true);
			
			knowledgeBase.add(amazonVM);

		} catch (Exception e) {
			logger.error("Error", e);
		}
	}

	
}
