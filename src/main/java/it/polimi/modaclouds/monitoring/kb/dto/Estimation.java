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

public class Estimation extends SDA {

	private String window;
	private String nCPU;
	private String maxTime;
	private String avgWin;
	private String warmUp;
	public String getWindow() {
		return window;
	}
	public void setWindow(String window) {
		this.window = window;
	}
	public String getnCPU() {
		return nCPU;
	}
	public void setnCPU(String nCPU) {
		this.nCPU = nCPU;
	}
	public String getMaxTime() {
		return maxTime;
	}
	public void setMaxTime(String maxTime) {
		this.maxTime = maxTime;
	}
	public String getAvgWin() {
		return avgWin;
	}
	public void setAvgWin(String avgWin) {
		this.avgWin = avgWin;
	}
	public String getWarmUp() {
		return warmUp;
	}
	public void setWarmUp(String warmUp) {
		this.warmUp = warmUp;
	}
	
	
}
