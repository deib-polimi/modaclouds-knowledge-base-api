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
import it.polimi.modaclouds.qos_models.monitoring_ontology.MO;
import it.polimi.modaclouds.qos_models.monitoring_ontology.Method;
import it.polimi.modaclouds.qos_models.monitoring_ontology.VM;

import java.util.ArrayList;
import java.util.List;

public class OFBizDeployment extends DeploymentModelFactory {
	
	private static final String amazonFEVMURL = "http://54.73.155.143";
	private static final String amazonBEVMURL = "http://54.246.34.122";
	private static final String amazonFEURL = "http://54.73.155.143:8080";
	private static final String amazonMySqlURL = "http://54.246.34.122:3306";
	
	@Override
	public List<KBEntity> getModel() {
		
		List<KBEntity> entities = new ArrayList<KBEntity>();
		
		VM amazonFrontendVM = new VM();
		amazonFrontendVM.setUri(MO.URI+"FrontendVM-1");
		amazonFrontendVM.setId("FrontendVM");
		amazonFrontendVM.setUrl(amazonFEVMURL);
		amazonFrontendVM.setCloudProvider("Amazon");

		VM amazonBackendVM = new VM();
		amazonBackendVM.setUri(MO.URI+"BackendVM-1");
		amazonBackendVM.setId("BackendVM");
		amazonBackendVM.setUrl(amazonBEVMURL);
		amazonBackendVM.setCloudProvider("Amazon");

		InternalComponent amazonJVM = new InternalComponent();
		amazonJVM.setUri(MO.URI + "JVM-1");
		amazonJVM.setId("JVM");
		amazonJVM.addRequiredComponent(amazonFrontendVM);

		InternalComponent amazonMySQL = new InternalComponent();
		amazonMySQL.setUri(MO.URI + "MySQL-1");
		amazonMySQL.setId("MySQL");
		amazonMySQL.setUrl(amazonMySqlURL);
		amazonJVM.addRequiredComponent(amazonBackendVM);

		InternalComponent amazonFrontend = new InternalComponent();
		amazonFrontend.setUri(MO.URI + "Frontend-1");
		amazonFrontend.setId("Frontend");
		amazonFrontend.setUrl(amazonFEURL);
		amazonFrontend.addRequiredComponent(amazonJVM);
		amazonFrontend.addRequiredComponent(amazonMySQL);

		amazonFrontend.addProvidedMethod(new Method("/addtocartbulk"));
		amazonFrontend.addProvidedMethod(new Method("/checkLogin"));
		amazonFrontend.addProvidedMethod(new Method("/checkoutoptions"));
		amazonFrontend.addProvidedMethod(new Method("/login"));
		amazonFrontend.addProvidedMethod(new Method("/logout"));
		amazonFrontend.addProvidedMethod(new Method("/main"));
		amazonFrontend.addProvidedMethod(new Method("/orderhistory"));
		amazonFrontend.addProvidedMethod(new Method("/quickadd"));

		amazonMySQL.addProvidedMethod(new Method("/create"));
		amazonMySQL.addProvidedMethod(new Method("/read"));
		amazonMySQL.addProvidedMethod(new Method("/update"));
		amazonMySQL.addProvidedMethod(new Method("/delete"));
		
		entities.add(amazonFrontend);
		
		return entities;
	}

}
