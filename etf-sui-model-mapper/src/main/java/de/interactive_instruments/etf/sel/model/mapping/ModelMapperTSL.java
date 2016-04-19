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

import com.eviware.soapui.model.testsuite.LoadTest;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestSuiteListener;
import com.eviware.soapui.security.SecurityTest;

import de.interactive_instruments.etf.sel.Utils;

/**
 * Connects the static mapped model (TestCases) with the
 * dynamic model (TestResults)
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
public class ModelMapperTSL implements TestSuiteListener {

	private static ModelBridge getBridge(final TestCase tc) {
		return ModelBridge.getOrCreateEnvBridge(
				tc.getTestSuite().getProject());
	}

	private static ModelBridge getBridge(final TestStep ts) {
		return ModelBridge.getOrCreateEnvBridge(
				ts.getTestCase().getTestSuite().getProject());
	}

	@Override
	public void testCaseAdded(final TestCase testCase) {
		Utils.log("Adding TestCase \"" + testCase.getLabel() +
				"\" to model");
		getBridge(testCase).addTestCasePrxy(testCase);
	}

	@Override
	public void testCaseRemoved(final TestCase testCase) {
		getBridge(testCase).deleteTestCaseByIdPrxy(testCase.getId());
	}

	@Override
	public void testStepAdded(TestStep testStep, int arg1) {
		if (Utils.DISABLE_REPORTING) {
			testStep.getTestCase().getTestSuite().removeTestSuiteListener(this);
			return;
		}

		Utils.log("Adding TestStep \"" + testStep.getLabel() +
				"\" to model");
		getBridge(testStep).cacheTestStep(null, testStep);
	}

	@Override
	public void testStepRemoved(TestStep testStep, int arg1) {
		getBridge(testStep).removeTestStep(testStep);
	}

	///////////////////////////////////////////////
	// DUMMIES
	///////////////////////////////////////////////

	@Override
	public void testStepMoved(TestStep arg0, int arg1, int arg2) {}

	@Override
	public void loadTestAdded(LoadTest arg0) {}

	@Override
	public void loadTestRemoved(LoadTest arg0) {}

	@Override
	public void securityTestAdded(SecurityTest arg0) {}

	@Override
	public void securityTestRemoved(SecurityTest arg0) {}

	@Override
	public void testCaseMoved(TestCase arg0, int arg1, int arg2) {}
}
