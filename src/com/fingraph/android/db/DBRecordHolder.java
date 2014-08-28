/*******************************************************************************
 * Copyright 2014 tgrape Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.fingraph.android.db;

public class DBRecordHolder {
	String command; 
	String sendData;
	String date;
	
	public DBRecordHolder(String _command, String _sendData, String _date) {
		this.command = _command;
		this.sendData = _sendData;
		this.date = _date;
	}
	
	public String getSendData(){
		return this.sendData;
	}
}
