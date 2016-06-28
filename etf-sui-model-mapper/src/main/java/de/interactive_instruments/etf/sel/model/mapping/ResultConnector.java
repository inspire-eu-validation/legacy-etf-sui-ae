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
package de.interactive_instruments.etf.sel.model.mapping;

import java.io.IOException;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlRunTestCaseTestStep;
import com.eviware.soapui.model.testsuite.TestCaseRunContext;
import com.eviware.soapui.model.testsuite.TestCaseRunner;
import com.eviware.soapui.model.testsuite.TestRunListener;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult;

import de.interactive_instruments.etf.dal.assembler.AssemblerException;
import de.interactive_instruments.etf.sel.Utils;
import de.interactive_instruments.etf.sel.model.impl.result.SuiTestCaseResult;
import de.interactive_instruments.etf.sel.model.impl.spec.SuiTestCase;
import de.interactive_instruments.exceptions.ImmutableLockException;
import de.interactive_instruments.exceptions.StoreException;

/**
 * Creates SuiTestCaseResult objects after each TestRun which are afterwards
 * connected with static model items
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
public class ResultConnector implements TestRunListener {

	/**
	 * Object will be created before a test run.
	 */
	public ResultConnector() {
		Utils.log("ResultConnector " + this.hashCode() + " created");
	}

	/**
	 * Creates SuiTestCaseResults after a test run and saves results in the TestReport
	 */
	@Override
	public void afterRun(final TestCaseRunner runner, final TestCaseRunContext runContext) {

		if (Utils.DISABLE_REPORTING) {
			runner.getTestCase().removeTestRunListener(this);
			return;
		}

		Utils.log("ResultConnector " + this.hashCode() + " invoked");

		// Get the required objects in the context and handle errors
		final String testCaseId = runner.getTestCase().getId();
		final ModelBridge modelBridge = (ModelBridge) ((WsdlProject) runner.getTestCase().getTestSuite().getProject()).getActiveEnvironment();
		final SuiRepository source = modelBridge.getSuiRepository();
		if (source == null) {
			Utils.logError(new Exception("Source not found: " +
					runner.getTestCase().getTestSuite().getProject().getName() + " " +
					runner.getTestCase().getTestSuite().getProject().getId()));
			return;
		}
		final SuiTestCase testCase = source.getTestCaseById(testCaseId);
		if (testCase == null) {
			Utils.logError(new Exception("TestCase not found " +
					runner.getTestCase().getName() + " " +
					runner.getTestCase().getId()));
			return;
		}
		try {

			/*
			 *  Create a new SuiTestCaseResult object which will process the
			 *  results from the SoapUI testrunner and connect it with the TestCase
			 *
			 */
			final SuiTestCaseResult tcResult = new SuiTestCaseResult(null, testCase, runner.getResults());

			// Check if the test case was called from another test step
			if (runContext.hasProperty("#CallingRunTestCaseStep#")) {
				final WsdlRunTestCaseTestStep runTestCaseTestStep = (WsdlRunTestCaseTestStep) runContext
						.getProperty("#CallingRunTestCaseStep#");
				final String testSuiteLabel = runTestCaseTestStep.getTestCase().getTestSuite().getLabel();
				modelBridge.getReport().queueGeneratedTestCaseResult(testSuiteLabel, tcResult);
			} else {
				final String testSuiteLabel = runner.getTestCase().getTestSuite().getLabel();
				modelBridge.getReport().addTestCaseResult(testSuiteLabel, tcResult);
			}

		} catch (IOException | AssemblerException | StoreException | ImmutableLockException e) {
			Utils.logError(e);
		}
	}

	/*
	 * DUMMIES
	 */

	@Override
	public void beforeRun(final TestCaseRunner runner, final TestCaseRunContext runContext) {
		// TODO Auto-generated method stub
	}

	@Override
	public void afterStep(final TestCaseRunner runner, final TestCaseRunContext runContext,
			TestStepResult result) {
		// TODO Auto-generated method stub
	}

	@Override
	public void beforeStep(final TestCaseRunner arg0, final TestCaseRunContext arg1) {
		// TODO Auto-generated method stub
	}

	@Override
	public void beforeStep(final TestCaseRunner arg0, final TestCaseRunContext arg1,
			final TestStep arg2) {
		// TODO Auto-generated method stub
	}

}
