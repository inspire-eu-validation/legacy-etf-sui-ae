/**
 * Copyright 2010-2019 interactive instruments GmbH
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
package de.interactive_instruments.xtf

import com.eviware.soapui.model.iface.MessageExchange
import com.eviware.soapui.model.testsuite.TestRunContext
import de.interactive_instruments.xtf.exceptions.FatalInternalException
import org.apache.log4j.Logger

class SOAPUI_I {
	protected Logger log
	protected context
	protected testRunner
	protected messageExchange

	// public Logger getLog() { return log; }
	Logger getLog() { return Logger.getLogger("etf-sui-owsgtl") }

	private static SOAPUI_I instance

	private static SOAPUI_I getInstance() { return instance }

	def getContext() { return context }

	def getTestRunner() { return testRunner }

	def getMessageExchange() { return messageExchange }

	static init(Logger log, context, testRunner) {
		instance = new SOAPUI_I(log, context, testRunner)
	}

	static init(Logger log, TestRunContext context, MessageExchange messageExchange) {
		instance = new SOAPUI_I(log, context, null , messageExchange)
	}

	SOAPUI_I(Logger log, context, testRunner, MessageExchange messageExchange=null)
	{
		this.log=log
		this.context=context
		if(testRunner!=null) {
			this.testRunner=testRunner
		}
		this.messageExchange=messageExchange
	}

	def getGroovyUtils() {
		if(!context)
			new FatalInternalException("Context in SOAPUI_I is not initialized!")

		def groovyUtils = new com.eviware.soapui.support.GroovyUtils(context)
		return groovyUtils
	}

	SOAPUI_I() {
		if(!instance) {
			throw new FatalInternalException(this, 
				"SOAPUI_I not initialized! Invoke \"SOAPUI_I.init(log, context, testRunner);\" first!")
		}
		this.log = this.instance.getLog()
		this.context = this.instance.context
		this.testRunner = this.instance.testRunner
		this.messageExchange=this.instance.messageExchange
	}
}
