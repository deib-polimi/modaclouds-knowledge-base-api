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

import java.util.List;

public class Correlation extends SDA {

	private List<String> otherMetrics;
	private String trainingPeriod;

	public List<String> getOtherMetrics() {
		return otherMetrics;
	}

	public void setOtherMetrics(List<String> otherMetrics) {
		this.otherMetrics = otherMetrics;
	}

	public String getTrainingPeriod() {
		return trainingPeriod;
	}

	public void setTrainingPeriod(String trainingPeriod) {
		this.trainingPeriod = trainingPeriod;
	}
	
}
