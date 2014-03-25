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

public class ForecastingTimeSeries extends SDA {

	private String forecastPeriod;
	private String order;
	private String autoregressive;
	private String integrated;
	private String movingAverage;

	public String getForecastPeriod() {
		return forecastPeriod;
	}

	public void setForecastPeriod(String forecastPeriod) {
		this.forecastPeriod = forecastPeriod;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public String getAutoregressive() {
		return autoregressive;
	}

	public void setAutoregressive(String autoregressive) {
		this.autoregressive = autoregressive;
	}

	public String getIntegrated() {
		return integrated;
	}

	public void setIntegrated(String integrated) {
		this.integrated = integrated;
	}

	public String getMovingAverage() {
		return movingAverage;
	}

	public void setMovingAverage(String movingAverage) {
		this.movingAverage = movingAverage;
	}
}
