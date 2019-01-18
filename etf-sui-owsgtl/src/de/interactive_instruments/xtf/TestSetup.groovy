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

import com.eviware.soapui.SoapUI
import com.eviware.soapui.support.log.Log4JMonitor
import de.interactive_instruments.xtf.exceptions.InvalidProjectParameterException
import org.apache.log4j.Level

class TestSetup {


	private static String authPwd;
	private static String authUser;
	private static String authMethod;
	private static String[] featuresInResponseList;
	private static String minFeaturesForTestCreation;
	private static long maxResponseTime;
	private static TestSetup instance;



	public TestSetup(def project) {

		// etf.testIntensive
		Util.getPropertyValueOrDefault(project, "testIntensive","false").equals("true");

		// etf.authPwd
		this.authPwd = Util.getPropertyValueOrDefault(project,
			"authPwd", "");

		// etf.authUser
		this.authUser = Util.getPropertyValueOrDefault(project,
			"authUser", "");

		// etf.authMethod
		this.authMethod = Util.getPropertyValueOrDefault(project,
			"authMethod", "httpBasic");

		// etf.maxBBOX
		Util.getPropertyValueOrDefault(project,	"maxBBOX",null);

		// etf.featuresInResponseList
		featuresInResponseList =
			 Util.getPropertyValueOrDefault(
				 project, "featuresInResponseList", "1000").split(",");

	    // etf.minFeaturesForTestCreation
		minFeaturesForTestCreation =
			 Util.getPropertyValueOrDefault(
				 project, "minFeaturesForTestCreation", "500");

		// etf.maxResponseTime
		maxResponseTime =
			 Util.getPropertyValueOrDefault(
				 project, "maxResponseTime", "0").toLong();
		if(maxResponseTime>0) {
			// seconds to ms
			maxResponseTime*=1000;
		}else if(maxResponseTime<0){
			throw new InvalidProjectParameterException(this,
				"Negative max response time set!");
		}
	}

	public static boolean isTestIntensive() {
		final def project = SOAPUI_I.getInstance().getTestRunner().testCase.testSuite.project;
		return Util.getPropertyValueOrDefault(project,
			"testIntensive","false").equals("true");
	}

	public static Bbox getMaxBbox() {
		final def project = SOAPUI_I.getInstance().getTestRunner().testCase.testSuite.project;
		return new Bbox( Util.getPropertyValueOrDefault(project, "maxBBOX",null) );
	}

	public static void init() {

		initGroovyLogAppender();
		def log = SOAPUI_I.getInstance().getLog();

		// Ensure existence of project properties
		def project = SOAPUI_I.getInstance().getTestRunner().testCase.testSuite.project;
		def projProps = [
			'serviceEndpoint',
			'testIntensive',
			'authUser',
			'authPwd',
			'authMethod'
		];
		for( prop in projProps ) {
			if( !project.hasProperty(prop) ) {
				project.setPropertyValue(prop, '');
				log.info(prop+" property created");
			}
		}
		// Log Project properties
		log.info("Setting up tests with parameters: ");
		for( prop in project.getPropertyList() ) {
			log.info("  -  "+prop.getName()+" : "+Util.cutString(prop.getValue()) );
		}

		// Load project properties
		this.instance = new TestSetup(project);

		Util.updateCredentials( SOAPUI_I.getInstance().getTestRunner() );
		deleteGeneratedTestCases( SOAPUI_I.getInstance().getTestRunner() );
		// setTimeoutAssertions( SOAPUI_I.getInstance().getTestRunner() );
	}

	public static void initGroovyLogAppender() {
		final Log4JMonitor logMonitor = SoapUI.getLogMonitor();
		final String loggerName = "etf-sui-owsgtl";

		if (!SoapUI.isCommandLine() && ( logMonitor == null
				||  !logMonitor.hasLogArea(loggerName)) ) {
			logMonitor.addLogArea("etf log",
					loggerName,
					false).setLevel(Level.INFO);
		}
	}

	private static void deleteGeneratedTestCases(def testRunner) {
		// Delete all test cases with XTF.deleteOnTestSetup=true property
		for( testSuite in testRunner.testCase.testSuite.project.getTestSuiteList() ) {
			for( testCase in testSuite.getTestCaseList() ) {
				final String del = testCase.getPropertyValue("XTF.deleteOnTestSetup");

				/*
				// Does not work, because the property can not be set on a generated
				// teststep!?!?
				if(del!=null && del.equals("true")) {
					testSuite.removeTestCase(testCase);
				}
				*/

				if(testCase.hasProperty("XTF.deleteOnTestSetup") &&
					!testCase.getPropertyValue("XTF.deleteOnTestSetup").equals("false") )
				{
					SOAPUI_I.getInstance().getLog().info(
						"Removeing generated TestCase "+testCase.getLabel());
					testSuite.removeTestCase(testCase);
				}else{
					for(testStep in testCase.getTestStepList()) {
						if(testStep.hasProperty("XTF.deleteOnTestSetup") &&
							!testStep.getPropertyValue("XTF.deleteOnTestSetup").equals("false") )
						{
							SOAPUI_I.getInstance().getLog().info(
								"Removeing generated TestStep "+testStep.getLabel());
							testCase.removeTestStep(testStep);
						}
					}
				}

			}
		}
	}
}
