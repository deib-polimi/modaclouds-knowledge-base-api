package it.polimi.modaclouds.monitoring.kb.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class KbEntityImpl extends KBEntity {
	
	private HashSet<String> set = new HashSet<>();
	private List<String> list = new ArrayList<>();
	private Map<String, String> map = new HashMap<String, String>();
	private String string = new String();
	
	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	public void addElementToSet(String s){
		set.add(s);
	}
	
	public void addElementToList(String s){
		list.add(s);
	}
	
	public void addElementToMap(String key, String s){
		map.put(key, s);
	}

	public HashSet<String> getSet() {
		return set;
	}

	public void setSet(HashSet<String> set) {
		this.set = set;
	}

	public List<String> getList() {
		return list;
	}

	public void setList(List<String> list) {
		this.list = list;
	}

	public Map<String, String> getMap() {
		return map;
	}

	public void setMap(Map<String, String> map) {
		this.map = map;
	}
	
	

}
