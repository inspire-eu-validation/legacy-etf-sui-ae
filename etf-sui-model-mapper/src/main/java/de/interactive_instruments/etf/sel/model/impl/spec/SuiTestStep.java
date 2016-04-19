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
package de.interactive_instruments.etf.sel.model.impl.spec;

import java.io.IOException;
import java.util.TreeMap;
import java.util.UUID;

import com.eviware.soapui.impl.support.AbstractHttpRequest;
import com.eviware.soapui.impl.support.http.HttpRequestTestStep;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequest;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.support.UISupport;

import de.interactive_instruments.IFile;
import de.interactive_instruments.SUtils;
import de.interactive_instruments.Version;
import de.interactive_instruments.etf.dal.assembler.AssemblerException;
import de.interactive_instruments.etf.model.item.DefaultVersionData;
import de.interactive_instruments.etf.model.item.DefaultVersionDataFactory;
import de.interactive_instruments.etf.model.item.EID;
import de.interactive_instruments.etf.model.item.ModelItem;
import de.interactive_instruments.etf.model.specification.AbstractTestStep;
import de.interactive_instruments.etf.model.specification.Assertion;
import de.interactive_instruments.etf.sel.Utils;
import de.interactive_instruments.etf.sel.model.mapping.ModelBridge;
import de.interactive_instruments.exceptions.ContainerFactoryException;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.properties.Properties;

/**
 * Maps a TestStep to a model TestStep
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class SuiTestStep extends AbstractTestStep {

	private final SuiTestCase parent;
	private IFile dumpFile = null;
	private final TestStep suiTestStep;

	public SuiTestStep(
			final SuiTestCase parent,
			final com.eviware.soapui.model.testsuite.TestStep suiTestStep) {
		this.parent = parent;
		label = suiTestStep.getLabel();
		description = suiTestStep.getDescription();
		id = new EID(suiTestStep.getId());
		properties = new Properties();
		versionData = DefaultVersionDataFactory.getInstance().createHash(this.toString());
		this.suiTestStep = suiTestStep;
		try {

			// TODO add type property to model
			properties.setProperty("type", suiTestStep.getClass().getSimpleName());

			if (suiTestStep instanceof HttpRequestTestStep) {
				AbstractHttpRequest httpRequest = null;

				final ModelBridge modelBridge = (ModelBridge) ((WsdlProject) suiTestStep.getTestCase().getTestSuite().getProject()).getActiveEnvironment();

				final HttpRequestTestStep testRequest = (HttpRequestTestStep) suiTestStep;
				if (testRequest.getHttpRequest() instanceof AbstractHttpRequest) {
					httpRequest = (AbstractHttpRequest) testRequest.getHttpRequest();
					dumpFile = new IFile(modelBridge.getAppendixFactory().getNewReferencedContainerStoreUri().toString());
					dumpFile.createNewFile();
					httpRequest.setDumpFile(dumpFile.getAbsolutePath());
				}

				updateRequestData();

				if (testRequest.getAssertionCount() > 0) {
					assertions = new TreeMap<String, Assertion>();
				}
				for (TestAssertion assertion : testRequest.getAssertionList()) {
					this.assertions.put(assertion.getId(), new SuiAssertion(this, assertion));
				}
			}
		} catch (Exception e) {
			ExcUtils.supress(e);
			Utils.logError(e);
			UISupport.showErrorMessage("An error occured! See error log for details.");
		}
	}

	public IFile getDumpFile() {
		return dumpFile;
	}

	public void updateRequestData()
			throws ContainerFactoryException, StoreException, IOException, AssemblerException {

		if (suiTestStep instanceof HttpRequestTestStep) {
			final ModelBridge modelBridge = (ModelBridge) ((WsdlProject) suiTestStep.getTestCase().getTestSuite().getProject()).getActiveEnvironment();

			final HttpRequestTestStep testRequest = (HttpRequestTestStep) suiTestStep;
			AbstractHttpRequest httpRequest = null;
			if (testRequest.getHttpRequest() instanceof AbstractHttpRequest) {
				httpRequest = testRequest.getHttpRequest();
			}

			// Request
			if (testRequest.hasProperty("Request")) {
				final String expandedEndpoint = PropertyExpander.expandProperties(suiTestStep, testRequest.getPropertyValue("Endpoint"));

				final String endpoint = !SUtils.isNullOrEmpty(expandedEndpoint) ? "Endpoint: " + SUtils.ENDL + PropertyExpander.expandProperties(suiTestStep, testRequest.getPropertyValue("Endpoint"))
						+ SUtils.ENDL : "";
				final String addRequestInfo = PropertyExpander.expandProperties(suiTestStep, "<!-- " + SUtils.ENDL +
						endpoint +
						"RequestHeaders: " + SUtils.ENDL +
						testRequest.getHttpRequest().getRequestHeaders().toString() + SUtils.ENDL +
						"-->" + SUtils.ENDL);

				// If the RequestBody is not empty, its a post request otherwise
				// GET, PUT, ...
				if (!testRequest.getProperty("Request").getValue().isEmpty()) {
					// POST
					final String expandedProperties = PropertyExpander.expandProperties(suiTestStep,
							addRequestInfo + testRequest.getProperty("Request").getValue());
					this.testObjectInput = modelBridge.getAppendixFactory().create("HttpRequest", expandedProperties);
				} else if (httpRequest != null && httpRequest instanceof TestPropertyHolder) {
					// Extract the key value pairs of the GET Request and expand it
					final String parameters = endpoint + PropertyExpander.expandProperties(suiTestStep,
							Utils.testPropsToString(((TestPropertyHolder) httpRequest).getPropertyList(), true));
					this.testObjectInput = modelBridge.getAppendixFactory().create("HttpRequest",
							// looks bad in the report
							// addRequestInfo+parameters);
							parameters);
				} else {
					this.testObjectInput = modelBridge.getAppendixFactory()
							.create("HttpRequest", "Request type not supported yet");
				}
			}
		}
	}

	@Override
	public ModelItem getParent() {
		return parent;
	}
}
