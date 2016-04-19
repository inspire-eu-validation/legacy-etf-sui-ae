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
package de.interactive_instruments.etf.sel.model.impl.result;

import java.util.ArrayList;
import java.util.List;

import com.eviware.soapui.model.testsuite.TestStepResult;

import de.interactive_instruments.etf.model.item.EID;
import de.interactive_instruments.etf.model.item.EidFactory;
import de.interactive_instruments.etf.model.item.ModelItem;
import de.interactive_instruments.etf.model.result.AbstractTestCaseResult;
import de.interactive_instruments.etf.model.result.TestResultStatus;
import de.interactive_instruments.etf.model.specification.TestCase;
import de.interactive_instruments.etf.model.specification.TestCaseStatus;
import de.interactive_instruments.etf.sel.Utils;
import de.interactive_instruments.etf.sel.model.impl.spec.SuiTestCase;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;

/**
 * Maps a TestCaseResult to a model TestCaseResult
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
public class SuiTestCaseResult extends AbstractTestCaseResult {

	private final SuiTestSuiteResult parent;

	public SuiTestCaseResult(
			SuiTestSuiteResult parent,
			SuiTestCase associatedTestCase,
			List<TestStepResult> suiTestStepResults) {
		super(associatedTestCase);
		this.id = EidFactory.getDefault().createRandomUuid();
		this.parent = parent;
		this.testStepResults = new ArrayList<>();
		if (associatedTestCase.getImplStatus() != TestCaseStatus.IMPLEMENTED) {
			this.status = TestResultStatus.OK;
		} else {
			// TODO: how to handle deactivated test cases which nevertheless were run?
			this.status = TestResultStatus.OK;
		}

		try {
			// Associated TestStepResults and the check overall status
			for (final TestStepResult result : suiTestStepResults) {
				SuiTestStepResult testStepRes = new SuiTestStepResult(this, result);
				this.testStepResults.add(testStepRes);

				if (testStepRes.getResultStatus() == TestResultStatus.FAILED) {
					this.status = TestResultStatus.FAILED;
				}
			}
		} catch (ObjectWithIdNotFoundException e) {
			// this.status=TestResultStatus.FAILED;
			Utils.logError(e, "Error determining test step result");
		}
	}

	@Override
	public ModelItem getParent() {
		return parent;
	}
}
