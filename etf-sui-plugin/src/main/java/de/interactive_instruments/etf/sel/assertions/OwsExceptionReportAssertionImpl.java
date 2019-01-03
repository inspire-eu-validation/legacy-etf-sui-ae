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
package de.interactive_instruments.etf.sel.assertions;

import static de.interactive_instruments.etf.sel.assertions.OwsExceptionReportAssertionImpl.*;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoryMapping;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.ResponseAssertion;
import com.eviware.soapui.plugins.auto.PluginTestAssertion;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.XmlHolder;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

/**
 * Checks if a response contains an OWS exception report
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
@PluginTestAssertion(id = ID, label = LABEL, description = DESCRIPTION, category = AssertionCategoryMapping.VALIDATE_RESPONSE_CONTENT_CATEGORY)
public class OwsExceptionReportAssertionImpl extends WsdlMessageAssertion
		implements OwsExceptionReportAssertion, ResponseAssertion {
	public static final String ID = "OwsExceptionReportAssertion";
	public static final String LABEL = "Fail if service returns OWS Exception Report";
	public static final String DESCRIPTION = "Check for exceptions returned by an Open Web Service";

	public OwsExceptionReportAssertionImpl(TestAssertionConfig assertionConfig, Assertable modelItem) {
		super(assertionConfig, modelItem, false, true, false, false);
	}

	protected String internalAssertRequest(MessageExchange messageExchange, SubmitContext context) throws AssertionException {
		if (!messageExchange.hasRequest(true))
			return "Missing Request";
		else
			return "Validation failed";
	}

	protected String internalAssertResponse(MessageExchange messageExchange, SubmitContext context)
			throws AssertionException {
		final String response = messageExchange.getResponseContentAsXml();
		if (response == null || response.equals("")) {
			throw new AssertionException(new AssertionError("Unable to parse empty xml response"));
		}

		final XmlObject xml;
		try {
			xml = XmlObject.Factory.parse(messageExchange.getResponseContentAsXml());
		} catch (XmlException e) {
			throw new AssertionException(new AssertionError("Unable to parse response as xml: " + e.toString()));
		}

		final XmlObject[] fragment = xml.selectPath(XPATH_EXPRESSION);
		if (fragment != null && fragment[0] != null) {
			String exceptionFound = fragment[0].xmlText();
			if (exceptionFound.equals("<xml-fragment>true</xml-fragment>")) {
				// Try to get the error message
				String errorMessage = null;
				try {
					XmlHolder holder = new XmlHolder(xml);
					errorMessage = holder.getNodeValue("//*:ExceptionText");
				} catch (XmlException e) {}

				if (errorMessage != null && !errorMessage.equals("")) {
					throw new AssertionException(new AssertionError("Service returned an exception: " + errorMessage));
				}

				throw new AssertionException(new AssertionError("Service returned an exception"));
			}
		}

		return "Response does not contain an exception report.";
	}

	@Override
	public boolean configure() {
		UISupport.showInfoMessage("Adding Assertion that will fail if service returns an exception report",
				"OwsExceptionReportChecker");
		return true;
	}

	@Override
	protected String internalAssertProperty(TestPropertyHolder arg0,
			String arg1, MessageExchange arg2, SubmitContext arg3)
			throws AssertionException {
		return null;
	}
}
