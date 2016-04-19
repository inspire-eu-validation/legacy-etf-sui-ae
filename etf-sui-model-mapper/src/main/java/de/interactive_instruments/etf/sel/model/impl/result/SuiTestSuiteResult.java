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
import java.util.UUID;

import de.interactive_instruments.etf.model.item.EID;
import de.interactive_instruments.etf.model.item.EidFactory;
import de.interactive_instruments.etf.model.item.ModelItem;
import de.interactive_instruments.etf.model.result.AbstractTestSuiteResult;
import de.interactive_instruments.etf.model.result.TestCaseResult;
import de.interactive_instruments.etf.model.result.TestResultStatus;
import de.interactive_instruments.etf.model.result.TestSuiteResult;

/**
 * Maps a SoapUI TestResult to a model TestResult
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class SuiTestSuiteResult extends AbstractTestSuiteResult {

	public SuiTestSuiteResult(String testSuiteName) {
		testCaseResult = new ArrayList<>();
		this.label = testSuiteName;
		this.id = EidFactory.getDefault().createRandomUuid();
	}

	public void addResult(TestCaseResult testCaseResult) {
		this.testCaseResult.add(testCaseResult);
	}

	@Override
	public TestResultStatus getResultStatus() {

		for (TestCaseResult testCaseResult : this.testCaseResult) {
			if (testCaseResult.getResultStatus().isFailed()) {
				return TestResultStatus.FAILED;
			}
		}
		return TestResultStatus.OK;
	}

	@Override
	public ModelItem getParent() {
		return null;
	}
}
