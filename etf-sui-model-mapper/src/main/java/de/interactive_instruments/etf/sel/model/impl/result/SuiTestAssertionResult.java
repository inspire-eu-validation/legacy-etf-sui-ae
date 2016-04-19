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

import com.eviware.soapui.model.testsuite.Assertable;

import de.interactive_instruments.container.LazyLoadContainer;
import de.interactive_instruments.etf.model.item.EidFactory;
import de.interactive_instruments.etf.model.item.ModelItem;
import de.interactive_instruments.etf.model.result.AbstractTestAssertionResult;
import de.interactive_instruments.etf.model.result.TestResultStatus;
import de.interactive_instruments.etf.model.specification.Assertion;

/**
 * Maps a SoapUI TestAssertionResult to a model TestAssertionResult
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class SuiTestAssertionResult extends AbstractTestAssertionResult {

	private SuiTestStepResult parent;
	private TestResultStatus status;

	public SuiTestAssertionResult(SuiTestStepResult parent, Assertable.AssertionStatus status,
			Assertion assertion) {
		super(assertion);
		this.id = EidFactory.getDefault().createRandomUuid();
		this.parent = parent;
		this.status = status == Assertable.AssertionStatus.VALID ? TestResultStatus.OK : TestResultStatus.FAILED;
	}

	@Override
	public ModelItem getParent() {
		return parent;
	}

	@Override
	public TestResultStatus getResultStatus() {
		return this.status;
	}

	public void addMessage(LazyLoadContainer container) {
		if (this.messages == null) {
			this.messages = new ArrayList<>();
		}
		this.messages.add(container);
	}
}
