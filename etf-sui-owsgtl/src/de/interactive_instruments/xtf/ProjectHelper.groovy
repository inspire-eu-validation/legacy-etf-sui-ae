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

import com.eviware.soapui.impl.rest.support.RestUtils
import com.eviware.soapui.impl.wsdl.panels.support.MockTestSuiteRunner
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.XPathContainsAssertion
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.XQueryContainsAssertion
import com.eviware.soapui.model.propertyexpansion.PropertyExpander
import com.eviware.soapui.model.testsuite.TestAssertion
import com.eviware.soapui.model.testsuite.TestStepResult
import com.eviware.soapui.support.XmlHolder
import de.interactive_instruments.UriUtils
import de.interactive_instruments.etf.model.exceptions.TestCaseNotFoundException
import de.interactive_instruments.etf.model.exceptions.TestStepNotFoundException
import de.interactive_instruments.exceptions.EmptyContentParseException
import de.interactive_instruments.exceptions.XmlParseException

class ProjModel {
	String testStep=null
	String testCase=null
	String testSuite=null

	String toString() {
		return testSuite+"-|-"+testCase+"-|-"+testStep
	}
}

class ProjectHelper extends SOAPUI_I {

	private testSuite

	ProjectHelper() {
		if(testRunner!=null) {
			if(testRunner instanceof MockTestSuiteRunner) {
				testSuite = testRunner.getTestSuite()
			}else{
				testSuite = testRunner.testCase.testSuite
			}
		}else if(messageExchange!=null) {
				testSuite = messageExchange.modelItem.testStep.testCase.testSuite
		}else{
			throw new Exception("Incorrect ProjectHelper initialization")
		}
	}

	private getTestCaseI() {
		if(testRunner!=null) {
			return testRunner.testCase
		}
		return messageExchange.modelItem.testStep.testCase
	}

	def getTestSuite(String testSuiteName, boolean createIfMissing=false) {
		def testSuite = this.testSuite.project.getTestSuiteByName(testSuiteName)
		if(!testSuite) {
			if(createIfMissing==false) {
				throw new Exception("TestSuite \""+testSuiteName+"\" not found!")
			}
			testSuite = this.testSuite.project.addNewTestSuite(testSuiteName)
		}
		return testSuite
	}

	def getTestCase(String testCaseName=null, testSuite=null,
					boolean createIfMissing=false) {
		if(testCaseName==null)
			return getTestCaseI()
		if(testSuite==null) {
			testSuite=this.testSuite
		}
		def testCase = testSuite.getTestCaseByName(testCaseName)
		if (testCase==null) {
			if(createIfMissing==false) {
				throw new TestCaseNotFoundException("TestCase \""+testCaseName+"\" not found!")
			}
			testCase = testSuite.addNewTestCase(testCaseName)
		}
		return testCase
	}

	WsdlTestStep getTestStep(String testStepName, testCase=null,
							 String testStepType=null) {
		if(testCase==null) {
			testCase=getTestCaseI()
		}
		WsdlTestStep testStep = testCase.getTestStepByName(testStepName)
		if(!testStep) {
			if(!testStepType) {
				throw new TestStepNotFoundException("TestStep \""+testStepName+
				"\" not found in TestCase \""+testCase.getLabel()+
				"\"!")
			}
			testStep = testCase.addTestStep(testStepType, testStepName)
		}
		return testStep
	}

	/*
	public XmlHolder getTestStepResponseAsXML(String testStepName) {
		return new XmlHolder(getTestStepResult(testStepName));
	}
	*/
	def getTestStepResponseAsXML(String testStepName) {
		def response =  getTestStepResult(testStepName)
		if(response!=null && response instanceof XmlHolder) {
			return response
		}
		throw new XmlParseException("Well-formed XML data expected, however not able to parse response.")
	}

	def getTestStepResult(String testStepName) {
		def testStep = this.getTestStep(testStepName)
		def response
		if (testStep instanceof HttpTestRequestStep) {
			response = testStep.getProperty("Response")
			if(response.value==null) {
				throw new EmptyContentParseException("Test step '"+testStepName+"' has an empty response!", testStepName)
			}

			OutputFormat of = new OutputFormat(Util.getResponseHeader(testStep, "Content-Type"))
			if(of.isParsable() || of.isText() )
			{
				try {
					return getGroovyUtils().getXmlHolder(
						testStep.getHttpRequest().getResponseContentAsXml() )
				} catch( e ) {
					// log.warn("XML Parser is unable to parse response: "+response.value)
					log.warn("Well-formed XML data expected, however not able to parse response: ${e.class.name} : ${e.message}")
				}
			}
			return response.value

		}else{
			return testStep.getProperty("result")
		}
	}

	WsdlTestStep generateStandaloneHttpRequest(String testStepName,
											   ProjModel target=null,
											   boolean createRunTestCaseStep=true, boolean failOnError=true)
	{

		if(target==null) {
			target=new ProjModel()
			target.testSuite=this.testSuite.getLabel()
			target.testCase=getTestCase().getLabel()+" (generated)"
			target.testStep=testStepName+" (generated)"
		}else{
			if(target.testSuite==null) {
				target.testSuite=this.testSuite.getLabel()
				// target.testSuite="Generated TestCases";
			}

			if(target.testCase==null) {
				if(!getTestCase().getLabel().contains('(generated)')) {
					target.testCase=getTestCase().getLabel()+" (generated)"
				}else{
                    // generated from within a generated test case.
                    // kinda inception...
					target.testCase=getTestCase().getLabel()
				}
			}else if(!target.testCase.contains("(generated)")) {
				target.testCase+=" (generated)"
			}
			target.testCase = target.testCase.minus(' (disabled)')

			if(target.testStep==null) {
				target.testStep=testStepName+" (generated)"
			}
		}

		log.info("Generating request \""+target.testStep+"\" from test step \""+
			testStepName+"\".")

		WsdlTestStep ts = getTestStep(testStepName)
		if (!(ts instanceof HttpTestRequestStep)) {
			throw new Exception("sourceTestStep \""+testStepName+
				"\"	is not a HttpTestRequestStep!")
		}
		HttpTestRequestStep sourceTestStep = ts

		WsdlTestCase targetTestCase = getTestCase(target.testCase,
			getTestSuite(target.testSuite, true), true)
		targetTestCase.setPropertyValue("XTF.deleteOnTestSetup", "true")
		targetTestCase.setDescription("This test case has been generated in test case \""+
			getTestCase().getLabel()+"\"")
		// Disable Testcase, we will insert a test step that will call this test case
		targetTestCase.setDisabled(true)
		// Important: Discard succesful test step results. Tons of requests will flood
		// the memory without gc cleanup...
		targetTestCase.setDiscardOkResults(true)
		targetTestCase.setMaxResults(10)
		targetTestCase.setFailOnError(failOnError)
		targetTestCase.setFailTestCaseOnErrors(true)

		def testStep
		try{
			testStep = getTestStep( target.testStep, targetTestCase)
		}catch(Exception e) {
		}
		if(testStep!=null) {
			throw new Exception("TestStep \""+target.testStep+
				"\" already exists in target TestCase \""+target.testCase+"\" !")
		}

		try{
			ts.clone(targetTestCase, target.testStep)
		}catch(ClassCastException e) {
			// Cloneing works, but throws class cast exception form HttpTestRequestStep:181
			// com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep
			// cannot be cast to com.eviware.soapui.impl.wsdl.teststeps.WsdlTestRequestStep
			// Possible cleaner workaround: Copy whole testStep config to new teststep
		}
		HttpTestRequestStep targetTestStep =
			getTestStep( target.testStep, targetTestCase)

		// Add some information in the description
		targetTestStep.setDescription("Generated Request. "
			+sourceTestStep.getDescription())

		// To save memory, discard all responses.
		// @Todo: Evaluating the response from anaoher generated test step does not work
		// with this action. Possible solution: group only about 50 test steps
		// in one generated test case. This should be controlled  from outside of
		// this method.
		targetTestStep.getTestRequest().setDiscardResponse(true)

		// Overwrite properties in request with current properties in context
		// Overwrite Post properties
		def propertyExpander = new PropertyExpander(true)
		String sourceRequestBody =
			propertyExpander.expandProperties( this.context,
				sourceTestStep.getProperty("Request").value)
		targetTestStep.getHttpRequest().setRequestContent(sourceRequestBody)
		// Overwrite GET properties
		def sourceParams = sourceTestStep.getHttpRequest().getParams()
		def targetParams = targetTestStep.getHttpRequest().getParams()
		for(int i=0; i < sourceParams.size(); i++) {
			targetParams.getPropertyAt(i).setValue(
				propertyExpander.expandProperties( this.context,
					sourceParams.getPropertyAt(i).getValue()
				)
			)
		}

		// Overwrite properties in assertions
		// Only Xpath and Xquery assertions are supported yet
		for(TestAssertion assertion in targetTestStep.getAssertionList()) {
			if(assertion instanceof XPathContainsAssertion) {
				XPathContainsAssertion xpathAssertion = assertion
				xpathAssertion.setExpectedContent(
					propertyExpander.expandProperties( this.context,
						xpathAssertion.getExpectedContent())
				)
				xpathAssertion.setPath(
					propertyExpander.expandProperties( this.context,
						xpathAssertion.getPath())
				)
			}else if(assertion instanceof XQueryContainsAssertion) {
				XQueryContainsAssertion xqueryAssertion = assertion
				xqueryAssertion.setExpectedContent(
					propertyExpander.expandProperties( this.context,
						xqueryAssertion.getExpectedContent())
				)
				xqueryAssertion.setPath(
					propertyExpander.expandProperties( this.context,
						xqueryAssertion.getPath())
				)
			}

		}

		// Overwrite endpoint
		targetTestStep.getHttpRequest().setEndpoint(
			propertyExpander.expandProperties( this.context,
				UriUtils.ensureUrlEncodedParams(
				targetTestStep.getHttpRequest().getEndpoint()))
		)

		// Remove authorization header if endpoint is not in same domain
		Util.updateTestStepCredentials(targetTestStep, this.context)

		// Set the parameters in the URL explicit as request parameters or
		// the encoding might fail on the server side (double decoding of '+' signs = '')
		RestUtils.extractParams(UriUtils.ensureUrlEncodedParams(
				targetTestStep.getHttpRequest().getEndpoint()),
				targetTestStep.getHttpRequest().getParams(), true)

		// Activate TestStep
		targetTestStep.setDisabled(false)

		// Create a calling step
		if(createRunTestCaseStep) {
			ensureTestCaseCall(target)
		}

		return targetTestStep
	}

	void ensureTestCaseCall(ProjModel target) {
		// Set a test step that will call the whole target test case
		def runTestCaseStep =
			getTestStep("Run "+target.testCase, getTestCaseI(), "calltestcase")
		def targetTestCase = getTestCase(target.testCase, getTestSuite(target.testSuite) )
		runTestCaseStep.setTargetTestCase(targetTestCase)
		runTestCaseStep.setRunMode(com.eviware.soapui.config.RunTestCaseRunModeTypeConfig.
			SINGLETON_AND_FAIL)
		runTestCaseStep.setDescription('Generated, technical test step. This test step will run the test case \"'+
			targetTestCase.getLabel()+'\" and fail if any step in that test case fails.')
	}

	void runTestSteps(testStepNames, boolean abortOnError=true) {
		for(name in testStepNames) {
			this.runTestStep(name, abortOnError)
		}
	}

	def runTestStep(String testStepName, boolean abortOnError=true) {
		def testStep = getTestStep(testStepName)

		log.info("Running test step \""+testStepName+"\" requested by test step \""+
			context.getCurrentStep().getLabel()+"\"")

		if (testStep instanceof HttpTestRequestStep) {
			// Remove authorization header if endpoint is not in same domain
			Util.updateTestStepCredentials(testStep, this.context)

			// Set the parameters in the URL explicit as request parameters or
			// the encoding might fail on the server side (double decoding of '+' signs = '')
			RestUtils.extractParams(testStep.getHttpRequest().getEndpoint(),
					testStep.getHttpRequest().getParams(), true)
		}

		// This will not invoke the TestCaseResult listeners
		// def result = testStep.run(testRunner, context)
		// This will invoke the TestCaseResult listeners
		def result = testRunner.runTestStepByName(testStepName)
		if(result.getStatus()!=TestStepResult.TestStepStatus.OK) {

			if (testStep instanceof HttpTestRequestStep) {
				testStep.testRequest.assertionList.each{
					if(it.valid) {
						log.info("Assertion "+it.name+" passed")
					}else if(it.failed) {
						log.warn("Assertion "+it.name+" failed with error:")
						log.warn("  -   "+it.getErrors()[0].getMessage())
					}else{
						log.warn("Assertion "+it.name+" is deactivated")
					}
				}
				def request = testStep.getProperty("Request")
				if(request.value!=null) {
					log.warn("")
					log.warn("---- REQUEST WITHOUT PROPERTY EXPANSION ----")
					log.warn(request.value)
					log.warn("---- REQUEST WITHOUT PROPERTY EXPANSION END ----")
					log.warn("")
					log.warn("")
					log.warn("")
					log.warn("---- REQUEST ----")

					def propertyExpander = new PropertyExpander(true)
					String sourceRequestBody =
						propertyExpander.expandProperties(context, request.value)

					log.warn(sourceRequestBody)
					log.warn("---- REQUEST END ----")
					log.warn("")
				}
				def response = testStep.getProperty("Response")
				if(response.value!=null) {
					log.warn("")
					log.warn("---- RESPONSE ----")
					log.warn(response.value)
					log.warn("---- RESPONSE END ----")
					log.warn("")
				}
			}

			def transferProperties =
				getTestStep("Transfer_Properties", this.testRunner.testCase)
			if(transferProperties!=null) {
				log.warn("")
				log.warn("Transfer_Properties in context:")
				log.warn("---- TRANSFER PROPERTIES ----")
				transferProperties.getPropertyList().each {
					log.warn("  -  "+it.getName()+" :" )
					log.warn("       "+Util.cutString(it.getValue()))
				}
				log.warn("---- TRANSFER PROPERTIES END ----")
				log.warn("")
			}

			if(abortOnError) {
				context.CalledRunTestCaseStep=testStep
				context.CalledTestCaseTestStepResult=result
				log.error("Test step \""+testStepName+"\" failed!")

				// tmp workaround
				// testRunner.gotoStepByName(testStepName)

				throw new Exception("Test step \""+testStepName+"\" failed!");
			}else{
				log.warn("Ignoring failed test step \""+testStepName+"\"")
				return null
			}
		}
		return getTestStepResult(testStepName)
	}

	@Deprecated
	getTransferProperty() {
		return getTestStep("Transfer_Properties", getTestCaseI(),
			"properties")
	}

	String getTransferProperty(String value) {
		return getTestStep("Transfer_Properties", getTestCaseI(),
			"properties").getPropertyValue(value)
	}

	XmlHolder getTransferPropertyAsXml(String value) {
		return groovyUtils.getXmlHolder(
			getTestStep("Transfer_Properties", getTestCaseI(),
			"properties").getPropertyValue(value)
		)
	}

	def setTransferProperty(name, value) {
		def testStep = getTestStep("Transfer_Properties", getTestCaseI(),
			"properties")
		testStep.setPropertyValue(name, value.toString())
		return testStep
	}

	void disableTestSteps(testStepNames) {
		for(testStepName in testStepNames) {
			if(!getTestStep(testStepName).isDisabled()) {
				log.info("Disabling test step \""+testStepName+"\"")
				getTestStep(testStepName).setDisabled(true)
			}
		}
	}

	void activateTestSteps(testStepNames) {
		for(testStepName in testStepNames) {
			if(getTestStep(testStepName).isDisabled()) {
				log.info("Activating test step \""+testStepName+"\"")
				getTestStep(testStepName).setDisabled(false)
			}
		}
	}

	File loadFileFromProjectDir(String path) {
		return new File (
			new File(this.testSuite.project.getPath()).getParent()+
			File.separator+path
		)
	}
}
