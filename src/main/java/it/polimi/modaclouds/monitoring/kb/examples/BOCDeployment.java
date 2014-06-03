package it.polimi.modaclouds.monitoring.kb.examples;

import it.polimi.modaclouds.qos_models.monitoring_ontology.InternalComponent;
import it.polimi.modaclouds.qos_models.monitoring_ontology.KBEntity;
import it.polimi.modaclouds.qos_models.monitoring_ontology.VM;

import java.util.ArrayList;
import java.util.List;

public class BOCDeployment extends DeploymentModelFactory {

	private static final int nBLTiers = 3;
	
	@Override
	public List<KBEntity> getModel() {
		
		List<KBEntity> entities = new ArrayList<KBEntity>();
		
		VM winVM = new VM();
		winVM.setCloudProvider("Flexiant");
		winVM.setId("WinVM");
		winVM.setUri(baseURI + winVM.getId() + "1");
		
		InternalComponent tomcat = new InternalComponent();
		tomcat.setId("Tomcat");
		tomcat.addRequiredComponent(winVM);
		tomcat.setUri(baseURI + tomcat.getId() + "1");
		
		InternalComponent war = new InternalComponent();
		war.setId("War");
		war.addRequiredComponent(tomcat);
		war.setUri(baseURI + war.getId() + "1");
		
		InternalComponent sqlDB = new InternalComponent();
		sqlDB.setId("SQLDB");
		sqlDB.addRequiredComponent(winVM);
		sqlDB.setUri(baseURI + sqlDB.getId() + "1");
		
		for (int i=0; i<nBLTiers ; i++) {
			InternalComponent bLTier = new InternalComponent();
			bLTier.setId("BLTier");
			bLTier.addRequiredComponent(sqlDB);
			bLTier.addRequiredComponent(winVM);
			bLTier.setUri(baseURI + bLTier.getId() + i);
			
			war.addRequiredComponent(bLTier);
		}
		
		entities.add(war);
		
		return entities;
	}

}
