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

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;


class Config {

	// TODO namespace and uriPrefix are both here and in qos-models
	static final String entitiesNamespace = "http://www.modaclouds.eu/rdfs/1.0/entities#";
	static final String graphsNamespace = "http://www.modaclouds.eu/rdfs/1.0/graphs#";
	static final String uriPrefix = "modaent";
	
	static final String javaClassProperty = "class";
	static final Property javaClassRDFProperty = new PropertyImpl(entitiesNamespace, javaClassProperty);
	
	static final Property keyRDFProperty = new PropertyImpl(entitiesNamespace, "key");
	static final Property valueRDFProperty = new PropertyImpl(entitiesNamespace, "value");
	
	static final Resource MapRDFResource = new ResourceImpl(entitiesNamespace + "Map");
}
