/**
 * Copyright 2010-2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.interactive_instruments.xtf;

import org.apache.log4j.Logger;
import com.eviware.soapui.model.iface.MessageExchange;

import de.interactive_instruments.xtf.exceptions.FatalInternalException;

class SOAPUI_I {
	protected Logger log;
	protected def context;
	protected def testRunner;
	protected def messageExchange;
	
	// public Logger getLog() { return log; }
	public Logger getLog() { return Logger.getLogger("etf-sui-owsgtl"); }
	public def getContext() { return context; }
	public def getTestRunner() { return testRunner; }
		
	
	private static SOAPUI_I instance;
	
	private static SOAPUI_I getInstance() { return instance; }
	
	public static init(Logger log, def context, def testRunner) {
		this.instance = new SOAPUI_I(log, context, testRunner);
	}
	
	public static init(Logger log, def context, MessageExchange messageExchange) {
		this.instance = new SOAPUI_I(log, context, null , messageExchange);
	}
	
	public SOAPUI_I(Logger log, def context, def testRunner, 
		MessageExchange messageExchange=null)
	{
		this.log=log;
		this.context=context;
		if(testRunner!=null) {
			this.testRunner=testRunner;
		}
		this.messageExchange=messageExchange;
	}
			
	public def getGroovyUtils() {
		if(!context)
			new FatalInternalException("Context in SOAPUI_I is not initialized!");
		
		def groovyUtils = new com.eviware.soapui.support.GroovyUtils(context);
		return groovyUtils;
	}
	
	public SOAPUI_I() {
		if(!instance) {
			throw new FatalInternalException(this, 
				"SOAPUI_I not initialized! Invoke \"SOAPUI_I.init(log, context, testRunner);\" first!");
		}
		this.log = this.instance.getLog();
		this.context = this.instance.getContext();
		this.testRunner = this.instance.getTestRunner();
	}
};
