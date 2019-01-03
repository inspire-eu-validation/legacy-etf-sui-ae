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

import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep;
import com.eviware.soapui.impl.wsdl.teststeps.registry.WsdlTestStepFactory;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class TestCaseDependencyStepFactory extends WsdlTestStepFactory {

	public static final String STEP_ID = "testcasedependency";
	public static final String NAME = "Test Case Dependency";
	public static final String DESCRIPTION = "Executes a Test Case, if it hasn't been already executed in a Test Run, "
			+ "and fails if the dependent Test Case fails";
	public static final String ICON = "/run_test_case_step.png";

	public TestCaseDependencyStepFactory() {
		super(STEP_ID, NAME,
				DESCRIPTION, null);
	}

	@Override
	public WsdlTestStep buildTestStep(final WsdlTestCase wsdlTestCase, final TestStepConfig testStepConfig,
			final boolean forLoadTest) {
		return new TestCaseDependencyStep(wsdlTestCase, testStepConfig, forLoadTest);
	}

	@Override
	public TestStepConfig createNewTestStep(final WsdlTestCase wsdlTestCase, final String s) {
		TestStepConfig testStepConfig = TestStepConfig.Factory.newInstance();
		testStepConfig.setType(STEP_ID);
		testStepConfig.setName(getTestStepName());
		return testStepConfig;
	}

	@Override
	public boolean canCreate() {
		return true;
	}
}
