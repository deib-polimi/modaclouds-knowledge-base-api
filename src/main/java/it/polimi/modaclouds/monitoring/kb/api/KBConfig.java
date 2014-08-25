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


public class KBConfig {

	// TODO namespace and uriPrefix are both here and in qos-models
	public static final String namespace = "http://www.modaclouds.eu/rdfs/1.0/entities#";
	public static final String uriPrefix = "modaent";
	
	public static final String javaClassProperty = "class";
	public static final Property javaClassRDFProperty = new PropertyImpl(namespace, javaClassProperty);
	
	public static final Property keyRDFProperty = new PropertyImpl(namespace, "key");
	public static final Property valueRDFProperty = new PropertyImpl(namespace, "value");
	
	public static final Resource MapRDFResource = new ResourceImpl(namespace + "Map");
}
