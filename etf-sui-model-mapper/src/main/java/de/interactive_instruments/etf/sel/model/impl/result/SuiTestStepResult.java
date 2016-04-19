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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.support.AbstractHttpRequest;
import com.eviware.soapui.impl.support.http.HttpRequestTestStep;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep;
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.testsuite.Assertable.AssertionStatus;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestStepResult.TestStepStatus;

import de.interactive_instruments.IFile;
import de.interactive_instruments.SUtils;
import de.interactive_instruments.container.LazyLoadContainer;
import de.interactive_instruments.etf.dal.assembler.AssemblerException;
import de.interactive_instruments.etf.model.item.EID;
import de.interactive_instruments.etf.model.item.EidFactory;
import de.interactive_instruments.etf.model.item.ModelItem;
import de.interactive_instruments.etf.model.result.AbstractTestAssertionResult;
import de.interactive_instruments.etf.model.result.AbstractTestStepResult;
import de.interactive_instruments.etf.model.result.TestResultStatus;
import de.interactive_instruments.etf.model.specification.Assertion;
import de.interactive_instruments.etf.sel.Utils;
import de.interactive_instruments.etf.sel.model.impl.spec.SuiTestStep;
import de.interactive_instruments.etf.sel.model.mapping.ModelBridge;
import de.interactive_instruments.exceptions.ContainerFactoryException;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;

/**
 * Maps a TestStepResult to a model TestStepResult
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
public class SuiTestStepResult extends AbstractTestStepResult {

	private final SuiTestCaseResult parent;

	public SuiTestStepResult(
			SuiTestCaseResult parent,
			com.eviware.soapui.model.testsuite.TestStepResult suiResult)
					throws ObjectWithIdNotFoundException {
		this.parent = parent;
		this.id = EidFactory.getDefault().createRandomUuid();

		try {
			TestStep suiTestStep = suiResult.getTestStep();
			final ModelBridge modelBridge = (ModelBridge) ((WsdlProject) suiTestStep.getTestCase().getTestSuite().getProject()).getActiveEnvironment();
			this.associatedTestStep = modelBridge.getTestStep(suiTestStep);

			this.duration = suiResult.getTimeTaken();
			this.startTimestamp = new Date(suiResult.getTimeStamp());

			if (suiResult.getStatus() == TestStepStatus.OK) {
				this.status = TestResultStatus.OK;
			} else if (suiResult.getTestStep().isDisabled()) {
				this.status = TestResultStatus.SKIPPED;
			} else {
				this.status = TestResultStatus.FAILED;
			}

			// Get additional information from a request that is
			// an instance of HttpTestRequestStep
			if (suiResult.getTestStep() instanceof HttpTestRequestStep || suiResult.getTestStep() instanceof RestTestRequestStep) {
				// HttpTestRequestStep testRequest = (HttpTestRequestStep) suiResult.getTestStep();
				HttpRequestTestStep testRequest = (HttpRequestTestStep) suiResult.getTestStep();

				AbstractHttpRequest httpRequest = null;
				if (testRequest.getHttpRequest() instanceof AbstractHttpRequest) {
					httpRequest = (AbstractHttpRequest) testRequest.getHttpRequest();
				}

				// Endpoint
				if (testRequest.hasProperty("Endpoint")) {
					this.resource = PropertyExpander.expandProperties(suiTestStep,
							testRequest.getProperty("Endpoint").getValue());
				}

				if (httpRequest != null && this.associatedTestStep instanceof SuiTestStep) {
					if (this.associatedTestStep instanceof SuiTestStep) {
						((SuiTestStep) this.associatedTestStep).updateRequestData();
					}
					this.testObjectOutput = modelBridge.getAppendixFactory().createReferencedContainer(
							"HttpResponse", ((SuiTestStep) this.associatedTestStep).getDumpFile().toURI());
				}

				assertionResults = new ArrayList<>();
				for (TestAssertion assertion : testRequest.getTestRequest().getAssertionList()) {
					Assertion assocAssertion = this.associatedTestStep.getAssertionById(assertion.getId());
					SuiTestAssertionResult assertionResult = new SuiTestAssertionResult(this,
							assertion.getStatus(), assocAssertion);
					// Add the result of the assertions
					if (assertion.getStatus() == AssertionStatus.FAILED) {
						for (AssertionError error : assertion.getErrors()) {
							assertionResult.addMessage(
									modelBridge.getAppendixFactory().create(
											assertion.getId(),
											"text/plain",
											error.getMessage()));
						}
					}
					assertionResults.add(assertionResult);
				}
			}

			// Additionally add messages
			if (suiResult.getMessages() != null) {
				int i = 1;
				this.messages = new ArrayList<LazyLoadContainer>();
				for (String message : suiResult.getMessages()) {
					this.messages.add(modelBridge.getAppendixFactory().create(
							"Message." + i++,
							"text/plain",
							message));
				}
			}

			if (this.associatedTestStep instanceof SuiTestStep) {
				((SuiTestStep) this.associatedTestStep).updateRequestData();
			}
		} catch (ContainerFactoryException | IOException | AssemblerException | StoreException e) {
			ExcUtils.supress(e);
			Utils.logError(e);
			SoapUI.logError(e);
		}
	}

	@Override
	public ModelItem getParent() {
		return parent;
	}
}
