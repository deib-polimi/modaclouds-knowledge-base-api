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

import it.polimi.modaclouds.qos_models.monitoring_ontology.InternalComponent;
import it.polimi.modaclouds.qos_models.monitoring_ontology.KBEntity;
import it.polimi.modaclouds.qos_models.monitoring_ontology.VM;

import java.util.ArrayList;
import java.util.List;

public class SofteamDeployment extends DeploymentModelFactory {

	public static int numberOfAgents = 2;

	@Override
	public List<KBEntity> getModel() {

		List<KBEntity> entities = new ArrayList<KBEntity>();

		VM adminServer = new VM();
		adminServer.setCloudProvider("Amazon");
		adminServer.setId("AdministrationServer");
		adminServer.setUri(baseURI + adminServer.getId() + "1");
		
		InternalComponent serverApp = new InternalComponent();
		serverApp.setId("ServerApp");
		serverApp.addRequiredComponent(adminServer);
		serverApp.setUri(baseURI + serverApp.getId() + "1");
		
		for (int i = 0; i < numberOfAgents; i++) {
			VM mainAgent = new VM();
			mainAgent.setCloudProvider("Amazon");
			mainAgent.setId("MainAgent");
			mainAgent.setUri(baseURI + mainAgent.getId() + i);
			
			InternalComponent agentApp = new InternalComponent();
			agentApp.setId("AgentApp");
			agentApp.addRequiredComponent(mainAgent);
			agentApp.setUri(baseURI + agentApp.getId() + i);
			
			serverApp.addRequiredComponent(agentApp);
		}
		
		entities.add(serverApp);

		return entities;
	}

}
