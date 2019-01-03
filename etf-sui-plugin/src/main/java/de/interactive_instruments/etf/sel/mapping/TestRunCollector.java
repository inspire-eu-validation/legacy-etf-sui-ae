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
package de.interactive_instruments.etf.sel.mapping;

import static de.interactive_instruments.etf.dal.dto.result.TestResultStatus.UNDEFINED;

import java.io.IOException;
import java.util.*;

import com.eviware.soapui.impl.support.AbstractHttpRequest;
import com.eviware.soapui.impl.support.http.HttpRequestTestStep;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.support.PathUtils;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequest;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep;
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlRunTestCaseTestStep;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.XQueryContainsAssertion;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.testsuite.*;
import com.eviware.soapui.model.testsuite.AssertionError;

import org.apache.commons.io.IOUtils;

import de.interactive_instruments.IFile;
import de.interactive_instruments.SUtils;
import de.interactive_instruments.UriUtils;
import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.exceptions.ExcUtils;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
// @ListenerConfiguration
public class TestRunCollector implements TestRunListener {

	private TestResultCollector collector;
	private IFile tmpDir;

	/**
	 * @see de.interactive_instruments.etf.dal.dto.result.TestResultStatus
	 */
	private enum TestResultStatus {
		PASSED(0), FAILED(1), PASSED_MANUAL(7);
		public final int value;

		TestResultStatus(final int value) {
			this.value = value;
		}
	}

	private TestResultStatus testAssertionStatus = TestResultStatus.PASSED;
	private TestResultStatus testStepStatus = TestResultStatus.PASSED;

	private void setManualOrError(final boolean manual) {
		if (manual) {
			testAssertionStatus = TestResultStatus.PASSED_MANUAL;
			if (testStepStatus != TestResultStatus.FAILED) {
				testStepStatus = TestResultStatus.PASSED_MANUAL;
			}
		} else {
			setFailed();
		}
	}

	private void setFailed() {
		testAssertionStatus = TestResultStatus.FAILED;
		testStepStatus = TestResultStatus.FAILED;
	}

	// called by SoapUI GUI
	public TestRunCollector() {}

	// called by Test Driver
	public TestRunCollector(final TestResultCollector collector) {
		this.collector = collector;
		this.tmpDir = new IFile(collector.getTempDir());
	}

	@Override
	public void beforeRun(final TestCaseRunner testCaseRunner, final TestCaseRunContext testCaseRunContext) {
		if (collector == null) {
			final Project project = testCaseRunner.getTestCase().getTestSuite().getProject();
			if (project instanceof WsdlProject
					&& ((WsdlProject) project).getActiveEnvironment() instanceof CollectorInjectionAdapter) {
				collector = ((CollectorInjectionAdapter) ((WsdlProject) project).getActiveEnvironment())
						.getTestResultCollector();
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
		testAssertionStatus = TestResultStatus.PASSED;
		testStepStatus = TestResultStatus.PASSED;
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
		collector.end(testCaseRunner.getTestCase().getId());
	}

	@Override
	public void beforeStep(final TestCaseRunner testCaseRunner, final TestCaseRunContext testCaseRunContext) {
		// deprecated
	}

	@Override
	public void beforeStep(final TestCaseRunner testCaseRunner, final TestCaseRunContext testCaseRunContext,
			final TestStep testStep) {
		Objects.requireNonNull(collector, "Collector not initialized before test step").startTestStep(testStep.getId());

		if (testStep instanceof HttpTestRequestStep || testStep instanceof RestTestRequestStep) {
			final HttpRequestTestStep testRequest = (HttpRequestTestStep) testStep;
			if (testRequest.getHttpRequest() instanceof AbstractHttpRequest) {
				final AbstractHttpRequest httpRequest = testRequest.getHttpRequest();
				final IFile dumpFile = new IFile(collector.getTempDir(), "dumpFile-" + UUID.randomUUID().toString());
				httpRequest.setDumpFile(dumpFile.getAbsolutePath());
				// 2GB
				httpRequest.setMaxSize(2147483648L);
			}
		}
	}

	/**
	 * Returns true if message is a manual test instruction
	 * Errors are passed to the logger of the collector
	 */
	private static boolean addMessage(final String message, final TestResultCollector collector, final int i) {
		final int startIndex = message.indexOf("<etfTranslate ");
		final int endIndex = message.lastIndexOf("</etfTranslate>");

		if (startIndex != -1) {
			final int wi = message.indexOf("what='", startIndex + 14);
			final boolean singleQ = wi > -1;
			final int whatIndex = singleQ ? wi : message.indexOf("what=\"", startIndex + 14);

			if (whatIndex != -1) {
				final int endWhatIndex = message.indexOf(singleQ ? "'" : "\"", whatIndex + 6);
				if (whatIndex < endWhatIndex) {
					final String t = message.substring(whatIndex + 6, endWhatIndex);
					final String translationTemplateId = t.startsWith("TR.") ? t : "TR." + t;
					final boolean manual = translationTemplateId.startsWith("TR.manual.");
					if (endIndex > 0) {
						// Check that are no >s in what
						final int messagesIndex = message.indexOf(">", startIndex + 1);
						if (whatIndex < messagesIndex && messagesIndex < endIndex) {
							final Map<String, String> tokenArguments = new TreeMap<>();
							parseRec(message, messagesIndex + 1, endIndex, tokenArguments);
							if (!tokenArguments.isEmpty()) {
								// if there is only one tokenArgument combination with an INFO token,
								// change the template to TR.INFO
								if (tokenArguments.size() == 1 && tokenArguments.containsKey("INFO")) {
									// Add message with INFO message
									collector.addMessage("TR.fallbackInfo", tokenArguments);
									// Log Assertion info message
									collector.info(tokenArguments.get("INFO"));
								} else {
									// Add message with token/arguments
									collector.addMessage(translationTemplateId, tokenArguments);
								}
							} else {
								// Add message without tokenArguments
								collector.addMessage(translationTemplateId);
							}
							if (!manual) {
								collector.error("Assertion failed with error '" + translationTemplateId + "'");
							}
							return manual;
						}
					} else {
						collector.addMessage(translationTemplateId);
						if (!manual) {
							collector.error("Assertion failed with error '" + translationTemplateId + "'");
						}
						return manual;
					}
				}
			}
		}

		try {
			// TODO use TR.fallbackInfo as temporary fallback for errors
			final Map<String, String> map = new HashMap<String, String>() {
				{
					put("INFO", message);
				}
			};
			collector.error(message);
			collector.addMessage("TR.fallbackInfo", map);
			collector.saveAttachment(IOUtils.toInputStream(message, "UTF-8"), "Error." + i, "text/plain", "Error");
		} catch (IOException e) {
			collector.internalError(e);
		}
		return false;
	}

	/**
	 * Existing arguments are appended (comma-separated)!
	 *
	 * Ignores: PASS and FAIL
	 *
	 * @param message
	 * @param parseIndex
	 * @param maxIndex
	 * @param tokenArguments
	 */
	private static void parseRec(final String message, final int parseIndex, final int maxIndex,
			final Map<String, String> tokenArguments) {
		final int startIndex = message.indexOf("<", parseIndex);
		if (startIndex != -1) {
			final int closingTagSignIndex = message.indexOf(">", startIndex + 2);
			if (closingTagSignIndex != -1 && message.charAt(closingTagSignIndex - 1) != '/') {
				final int firstAttributeInTag = message.indexOf(" ", startIndex + 2);
				final int tokenNameEndIndex = firstAttributeInTag > startIndex && firstAttributeInTag < closingTagSignIndex
						? firstAttributeInTag : closingTagSignIndex;
				final String token = message.substring(startIndex + 1, tokenNameEndIndex);
				if (!SUtils.isNullOrEmpty(token) && !token.equals("PASS") && !token.equals("FAIL")) {
					// Argument ends with the token
					final int argumentEndIndex = message.indexOf("</" + token + ">", closingTagSignIndex);
					if (tokenNameEndIndex < argumentEndIndex) {
						// Arguments begins after closing tag sign
						final String argument = message.substring(closingTagSignIndex + 1, argumentEndIndex);
						// Support listing, by appending an argument to a token that already exists
						final String existingArgument = tokenArguments.get(token);
						if (existingArgument != null) {
							// there is already an existing argument. Append non null, unique argument
							if (!SUtils.isNullOrEmpty(argument) && !argument.equals(existingArgument)) {
								tokenArguments.put(token, argument + ", " + existingArgument);
							}
						} else {
							// Add non empty argument or write the string "[not found in response]"
							if (!SUtils.isNullOrEmpty(argument)) {
								tokenArguments.put(token, argument);
							} else {
								tokenArguments.put(token, "[ empty value (invalid response?) ]");
							}
						}
						final int completeEndIndex = argumentEndIndex + token.length();
						if (completeEndIndex + 15 < maxIndex) {
							parseRec(message, completeEndIndex + 1, maxIndex, tokenArguments);
						}
					}
				}
			}
		}
	}

	@Override
	public void afterStep(final TestCaseRunner testCaseRunner, final TestCaseRunContext testCaseRunContext,
			final TestStepResult testStepResult) {
		Objects.requireNonNull(collector, "Collector not initialized after test step");
		testStepStatus = TestResultStatus.PASSED;

		int status = -1;
		if (testStepResult.getTestStep() instanceof HttpTestRequestStep
				|| testStepResult.getTestStep() instanceof RestTestRequestStep) {

			final HttpRequestTestStep testRequest = (HttpRequestTestStep) testStepResult.getTestStep();
			final AbstractHttpRequest httpRequest;
			if (testRequest.getHttpRequest() instanceof AbstractHttpRequest) {
				httpRequest = (AbstractHttpRequest) testRequest.getHttpRequest();
			} else {
				httpRequest = null;
			}

			// Endpoint
			final String endpoint;

			if (testRequest.hasProperty("Endpoint")) {
				endpoint = UriUtils.withoutQueryParameters(PropertyExpander.expandProperties(testRequest,
						testRequest.getProperty("Endpoint").getValue()));
				try {
					collector.saveAttachment(endpoint, "Endpoint", "text/plain", "ServiceEndpoint");
				} catch (IOException e) {
					collector.internalError(e);
				}
			} else {
				endpoint = null;
			}

			if (httpRequest != null) {
				if (httpRequest.getDumpFile() != null) {
					final IFile file = new IFile(PathUtils.resolveResourcePath(httpRequest.getDumpFile(), httpRequest));
					if (file.exists() && file.length() > 0) {
						try {
							final IFile copied = file.copyTo(
									tmpDir.secureExpandPathDown("response-" + testStepResult.getTestStep().getId()).toString());
							collector.markAttachment(copied.getName(), "Service Response", "UTF-8", null, "ServiceResponse");
						} catch (IOException e) {
							collector.internalError(e);
						}
					} else if (httpRequest.getResponse() != null
							&& !SUtils.isNullOrEmpty(httpRequest.getResponse().getContentAsString())) {
						// collector.getLogger().info("Received empty response");
						try {
							collector.saveAttachment(
									IOUtils.toInputStream(httpRequest.getResponse().getContentAsString(), "UTF-8"),
									"Service Response", null, "ServiceResponse");
						} catch (final IOException e) {
							collector.internalError(e);
						}
					}
				} else if (httpRequest.getResponse() != null
						&& !SUtils.isNullOrEmpty(httpRequest.getResponse().getContentAsString())) {
					try {
						collector.saveAttachment(IOUtils.toInputStream(httpRequest.getResponse().getContentAsString(), "UTF-8"),
								"Service Response", null, "ServiceResponse");
					} catch (final IOException e) {
						collector.internalError(e);
					}
				}
			}

			// Request
			if (testRequest.hasProperty("Request")) {

				// GET, PUT, ...
				if (!testRequest.getProperty("Request").getValue().isEmpty()) {
					// POST
					final String endpointText = !SUtils.isNullOrEmpty(endpoint) ? "Endpoint: "
							+ SUtils.ENDL
							+ PropertyExpander.expandProperties(testRequest, testRequest.getPropertyValue("Endpoint"))
							+ SUtils.ENDL : "";
					final String addRequestInfo = PropertyExpander.expandProperties(testRequest, "<!-- " + SUtils.ENDL +
							endpointText +
							"RequestHeaders: " + SUtils.ENDL +
							testRequest.getHttpRequest().getRequestHeaders().toString() + SUtils.ENDL +
							"-->" + SUtils.ENDL);

					final String expandedProperties = PropertyExpander.expandProperties(testRequest,
							addRequestInfo + testRequest.getProperty("Request").getValue());
					try {
						collector.saveAttachment(IOUtils.toInputStream(expandedProperties, "UTF-8"),
								"Request Parameter", null, "PostData");
					} catch (final IOException e) {
						ExcUtils.suppress(e);
					}
				}
				if (httpRequest != null && httpRequest instanceof TestPropertyHolder) {
					final List<TestProperty> propertyList = ((TestPropertyHolder) httpRequest).getPropertyList();
					final Map<String, String> parameterMap = new HashMap<>();
					for (final TestProperty testProperty : propertyList) {
						if (!testProperty.getName().startsWith("Transfer_Properties")) {
							parameterMap.put(testProperty.getName(),
									PropertyExpander.expandProperties(testRequest, testProperty.getValue()));
						}
					}

					final String query;
					if (httpRequest.getResponse() != null &&
							!SUtils.isNullOrEmpty(httpRequest.getResponse().getURL().toString())) {
						query = UriUtils.withQueryParameters(httpRequest.getResponse().getURL().toString(), parameterMap, true);
					} else {
						query = UriUtils.withQueryParameters(endpoint, parameterMap, true);
					}

					if (!SUtils.isNullOrEmpty(query)) {
						try {
							collector.saveAttachment(
									PropertyExpander.expandProperties(testRequest, query),
									"Request Parameter", null, "GetParameter");
						} catch (final IOException e) {
							ExcUtils.suppress(e);
						}
					}
				} else {
					// Request type not supported
				}
			}

			final List<TestAssertion> assertionList = testRequest.getTestRequest().getAssertionList();
			for (int i1 = 0, assertionListSize = assertionList.size(); i1 < assertionListSize; i1++) {
				final TestAssertion assertion = assertionList.get(i1);
				collector.startTestAssertion(assertion.getId());
				testAssertionStatus = TestResultStatus.PASSED;
				if (assertion.getErrors() != null) {
					int i = 1;
					final AssertionError[] errors = assertion.getErrors();
					for (int i2 = 0, errorsLength = errors.length; i2 < errorsLength; i2++) {
						final AssertionError error = errors[i2];
						if (assertion instanceof XQueryContainsAssertion) {
							final XQueryContainsAssertion xqueryAssertion = (XQueryContainsAssertion) assertion;
							// OK, this is very tricky:
							// we need to call selectFromCurrent() to get the whole message
							// because XmlUnit shortens the output. But as this assertion maybe reused in the run
							// we need to reset the internal properties...
							final String expectedContent = xqueryAssertion.getExpectedContent();
							xqueryAssertion.selectFromCurrent();
							final String translation = xqueryAssertion.getExpectedContent();
							if (translation.contains("<etfTranslate")) {
								setManualOrError(addMessage(translation, collector, i++));
							} else {
								setManualOrError(addMessage(error.getMessage(), collector, i++));
							}
							xqueryAssertion.setExpectedContent(expectedContent);
						} else {
							setManualOrError(addMessage(error.getMessage(), collector, i++));
						}
					}
				}
				collector.end(assertion.getId(), testAssertionStatus.value);
			}
		} else {
			if (testStepResult.getStatus() == TestStepResult.TestStepStatus.FAILED) {
				if (testStepResult.getTestStep() instanceof WsdlRunTestCaseTestStep) {
					status = 2;
				} else {
					status = 1;
				}
			} else {
				status = 0;
			}

			// Add messages
			final String[] messages = testStepResult.getMessages();
			if (messages != null) {
				for (int i = 0, messagesLength = messages.length; i < messagesLength; i++) {
					final String message = messages[i];
					try {
						if (message.contains("<etfTranslate")) {
							addMessage(message, collector, i + 1);
						} else {
							collector.saveAttachment(IOUtils.toInputStream(message, "UTF-8"), "Message." + (i + 1),
									"text/plain", "Message");
						}
					} catch (IOException e) {
						collector.internalError(e);
					}
				}
			}
		}

		if (status == -1) {
			// auto determine
			collector.end(testStepResult.getTestStep().getId(), testStepResult.getTimeStamp() + testStepResult.getTimeTaken());
		} else {
			collector.end(testStepResult.getTestStep().getId(), status,
					testStepResult.getTimeStamp() + testStepResult.getTimeTaken());
		}
	}
}
