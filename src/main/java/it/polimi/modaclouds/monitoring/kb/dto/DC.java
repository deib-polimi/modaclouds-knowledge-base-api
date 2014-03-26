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
package it.polimi.modaclouds.monitoring.kb.dto;

public class DC extends KBEntity {

	private boolean isEnabled;
	private float samplingProbability;
	private String samplingTime;
	private boolean isStarted;
	
	public boolean isEnabled() {
		return isEnabled;
	}
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	public float getSamplingProbability() {
		return samplingProbability;
	}
	public void setSamplingProbability(float samplingProbability) {
		this.samplingProbability = samplingProbability;
	}
	public String getSamplingTime() {
		return samplingTime;
	}
	public void setSamplingTime(String samplingTime) {
		this.samplingTime = samplingTime;
	}
	public boolean isStarted() {
		return isStarted;
	}
	public void setStarted(boolean isStarted) {
		this.isStarted = isStarted;
	}
	
	
}
