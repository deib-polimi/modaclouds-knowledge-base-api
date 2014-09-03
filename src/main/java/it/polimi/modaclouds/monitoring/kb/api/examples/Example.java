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
package it.polimi.modaclouds.monitoring.kb.api.examples;

import it.polimi.modaclouds.monitoring.kb.api.FusekiKBAPI;

import java.util.Arrays;

import com.google.common.collect.Sets;

public class Example {

	public static void main(String[] args) {
		FusekiKBAPI kb = new FusekiKBAPI("http://localhost:3030/modaclouds/kb");
		try {
			MyEntity entity1 = new MyEntity();
			entity1.setId("1");
			MyEntity entity2 = new MyEntity();
			entity2.setId("2");
			MyEntity entity3 = new MyEntity();
			entity3.setId("3");
			entity3.addElementToList("1");
			entity3.addElementToList("2");
			entity3.addElementToList("3");
			entity3.addElementToList("4");
			entity3.setString("Hello world!");
			kb.add(Arrays.asList(new Object[]{entity1,entity2,entity3}), "id","mygraph");
			kb.deleteEntitiesByPropertyValues(Sets.newHashSet(new String[]{"1","2"}),"id","mygraph");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
