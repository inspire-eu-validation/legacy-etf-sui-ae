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
package de.interactive_instruments.etf.sel.teststeps;

import java.util.List;

import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageExchangeTestStepResult;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlRunTestCaseTestStep;
import com.eviware.soapui.model.testsuite.*;
import com.eviware.soapui.model.testsuite.TestStepResult.TestStepStatus;
import com.eviware.soapui.plugins.auto.PluginTestStep;

import de.interactive_instruments.exceptions.ExcUtils;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@PluginTestStep(typeName = TestCaseDependencyStepFactory.STEP_ID, name = TestCaseDependencyStepFactory.NAME, description = TestCaseDependencyStepFactory.DESCRIPTION, iconPath = TestCaseDependencyStepFactory.ICON)
public class TestCaseDependencyStep extends WsdlRunTestCaseTestStep implements TestCaseDependencyTestStepDef {

	public TestCaseDependencyStep(WsdlTestCase testCase, TestStepConfig config, boolean forLoadTest) {
		super(testCase, config, forLoadTest);
	}

	public TestStepResult run(TestCaseRunner testRunner, TestCaseRunContext testRunContext) {
		final Object o = testRunner.getRunContext().getProperty("#ProjectRunner#");
		if (o != null) {
			try {
				final ProjectRunner projectRunner = (ProjectRunner) o;
				// Check if Test Case has already be run
				final String testSuiteId = getTargetTestCase().getParent().getId();
				final List<TestSuiteRunner> results = projectRunner.getResults();
				for (final TestSuiteRunner testSuiteRunner : results) {
					if (testSuiteRunner.getTestSuite().getId().equals(testSuiteId)) {
						for (final TestCaseRunner caseRunner : testSuiteRunner.getResults()) {
							if (caseRunner.getTestCase().getId().equals(getTargetTestCase().getId())) {
								if (caseRunner.getStatus() == TestRunner.Status.RUNNING) {
									caseRunner.waitUntilFinished();
								}
								final WsdlMessageExchangeTestStepResult result = new WsdlMessageExchangeTestStepResult(this);
								result.setTimeStamp(System.currentTimeMillis());
								result.setTimeTaken(caseRunner.getTimeTaken());
								result.setStatus(runnerStatusToTestStepStatus(caseRunner));
								return result;
							}
						}
					}
				}
			} catch (final ClassCastException ign) {
				ExcUtils.suppress(ign);
			}
		}
		return super.run(testRunner, testRunContext);
	}

	private TestStepStatus runnerStatusToTestStepStatus(final TestCaseRunner testCaseRunner) {
		switch (testCaseRunner.getStatus()) {
		case CANCELED:
			return TestStepStatus.CANCELED;
		case FAILED:
			return TestStepStatus.FAILED;
		case FINISHED:
			return TestStepStatus.OK;
		default:
			return TestStepStatus.UNKNOWN;
		}
	}
}
