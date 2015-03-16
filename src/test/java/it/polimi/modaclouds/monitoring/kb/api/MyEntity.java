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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyEntity {

	private Set<MyEntity> set;
	private List<MyEntity> list;
	private Map<String, MyEntity> map;
	private String string;
	private MyEntity entity;
	private String id;

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	public void addElementToSet(MyEntity s) {
		getSet().add(s);
	}

	public void addElementToList(MyEntity s) {
		getList().add(s);
	}

	public void addElementToMap(String key, MyEntity s) {
		getMap().put(key, s);
	}

	public Set<MyEntity> getSet() {
		if (set == null)
			set = new HashSet<MyEntity>();
		return set;
	}

	public void setSet(Set<MyEntity> set) {
		this.set = set;
	}

	public List<MyEntity> getList() {
		if (list == null)
			list = new ArrayList<MyEntity>();
		return list;
	}

	public void setList(List<MyEntity> list) {
		this.list = list;
	}

	public Map<String, MyEntity> getMap() {
		if (map == null)
			map = new HashMap<String, MyEntity>();
		return map;
	}

	public void setMap(Map<String, MyEntity> map) {
		this.map = map;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public MyEntity getEntity() {
		return entity;
	}

	public void setEntity(MyEntity entity) {
		this.entity = entity;
	}
	
	
	
}
