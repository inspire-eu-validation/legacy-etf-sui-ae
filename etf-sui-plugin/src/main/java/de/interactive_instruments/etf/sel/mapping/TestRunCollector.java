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
package de.interactive_instruments.etf.sel.mapping;

import static de.interactive_instruments.etf.dal.dto.result.TestResultStatus.UNDEFINED;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import com.eviware.soapui.impl.support.AbstractHttpRequest;
import com.eviware.soapui.impl.support.http.HttpRequestTestStep;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep;
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.testsuite.*;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.plugins.ListenerConfiguration;

import org.apache.commons.io.IOUtils;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.sel.Utils;
import de.interactive_instruments.etf.testdriver.TestResultCollector;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
@ListenerConfiguration
public class TestRunCollector implements TestRunListener {

	private TestResultCollector collector;
	private IFile tmpDir;

	void init(final TestResultCollector collector, final String translationTemplateId) {
		this.collector = collector;
		this.tmpDir = new IFile(collector.getTempDir());
	}

	@Override
	public void beforeRun(final TestCaseRunner testCaseRunner, final TestCaseRunContext testCaseRunContext) {
		if (collector == null) {
			final Project project = testCaseRunner.getTestCase().getTestSuite().getProject();
			if (project instanceof WsdlProject && ((WsdlProject) project).getActiveEnvironment() instanceof CollectorInjectionAdapter) {
				collector = ((CollectorInjectionAdapter) ((WsdlProject) project).getActiveEnvironment()).getTestResultCollector();
				this.tmpDir = new IFile(collector.getTempDir());
				try {
					this.tmpDir = IFile.createTempDir("sel");
				} catch (IOException e) {
					collector.internalError(e);
				}
			} else {
				collector = new DummyCollector();
			}
		}
		collector.startTestCase(testCaseRunner.getTestCase().getId());
	}

	@Override
	public void afterRun(final TestCaseRunner testCaseRunner, final TestCaseRunContext testCaseRunContext) {
		// Check if the collector is still in the writing test step state
		if (collector.currentModelType() == 4) {
			collector.error("Exception occurred in Test Step: " + testCaseRunner.getReason());
			// end step (we need the id if we are in a sub collector context
			collector.end(testCaseRunner.getRunContext().getCurrentStep().getId(), UNDEFINED.value());
		}
		collector.end(testCaseRunner.getTestCase().getId(), Utils.translateStatus(testCaseRunner.getStatus()));
	}

	@Override
	public void beforeStep(final TestCaseRunner testCaseRunner, final TestCaseRunContext testCaseRunContext) {
		// deprecated
	}

	@Override
	public void beforeStep(final TestCaseRunner testCaseRunner, final TestCaseRunContext testCaseRunContext, final TestStep testStep) {
		collector.startTestStep(testStep.getId());
	}

	@Override
	public void afterStep(final TestCaseRunner testCaseRunner, final TestCaseRunContext testCaseRunContext, final TestStepResult testStepResult) {
		// collector.startTestStep(testStepResult.getTestStep().getId(), testStepResult.getTimeStamp());

		if (testStepResult.getTestStep() instanceof HttpTestRequestStep || testStepResult.getTestStep() instanceof RestTestRequestStep) {

			final HttpRequestTestStep testRequest = (HttpRequestTestStep) testStepResult.getTestStep();
			final AbstractHttpRequest httpRequest;
			if (testRequest.getHttpRequest() instanceof AbstractHttpRequest) {
				httpRequest = (AbstractHttpRequest) testRequest.getHttpRequest();
			} else {
				httpRequest = null;
			}

			// Endpoint
			if (testRequest.hasProperty("Endpoint")) {
				final String endpoint = PropertyExpander.expandProperties(testRequest,
						testRequest.getProperty("Endpoint").getValue());
				try {
					collector.saveAttachment(new StringReader(endpoint), "Endpoint.", "text/plain", "Message");
				} catch (IOException e) {
					collector.internalError(e);
				}
			}

			if (httpRequest != null && httpRequest.getDumpFile() != null) {
				final IFile file = new IFile(httpRequest.getDumpFile());
				if (file.exists() && file.length() > 0) {
					try {
						final IFile copied = file.copyTo(tmpDir.secureExpandPathDown(testStepResult.getTestStep().getId()).toString());
						collector.markAttachment(copied.getPath(), "Http Request", "UTF-8", null, "HttpRequest");
					} catch (IOException e) {
						collector.internalError(e);
					}
				}
			}

			final List<TestAssertion> assertionList = testRequest.getTestRequest().getAssertionList();
			for (int i1 = 0, assertionListSize = assertionList.size(); i1 < assertionListSize; i1++) {
				final TestAssertion assertion = assertionList.get(i1);
				if (assertion.getErrors() != null) {
					collector.startTestAssertion(assertion.getId());
					int i = 1;
					final AssertionError[] errors = assertion.getErrors();
					for (int i2 = 0, errorsLength = errors.length; i2 < errorsLength; i2++) {
						final AssertionError error = errors[i2];
						collector.error(error.getMessage());
						try {
							collector.saveAttachment(IOUtils.toInputStream(error.getMessage(), "UTF-8"), "Error." + i++, "text/plain", "Error");
						} catch (IOException e) {
							collector.internalError(e);
						}
					}
					collector.end(assertion.getId(), Utils.translateStatus(assertion.getStatus()));
				}
			}
		}

		// Add messages
		final String[] messages = testStepResult.getMessages();
		if (messages != null) {
			for (int i = 0, messagesLength = messages.length; i < messagesLength; i++) {
				final String message = messages[i];
				try {
					collector.saveAttachment(IOUtils.toInputStream(message, "UTF-8"), "Message." + i + 1, "text/plain", "Message");
				} catch (IOException e) {
					collector.internalError(e);
				}
			}
		}

		collector.end(testStepResult.getTestStep().getId(), Utils.translateStatus(testStepResult.getStatus()), testStepResult.getTimeStamp() + testStepResult.getTimeTaken());
	}
}
