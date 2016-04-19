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

import java.text.ParseException;
import java.util.UUID;

import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.*;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.security.assertion.InvalidHttpStatusCodesAssertion;
import com.eviware.soapui.security.assertion.ValidHttpStatusCodesAssertion;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

import de.interactive_instruments.etf.model.item.*;
import de.interactive_instruments.etf.model.specification.AbstractAssertion;
import de.interactive_instruments.etf.model.specification.AssertionStatus;
import de.interactive_instruments.etf.sel.assertions.OwsExceptionReportAssertion;
import de.interactive_instruments.etf.sel.assertions.SchemaAssertion;
import de.interactive_instruments.properties.MutablePropertyHolder;
import de.interactive_instruments.properties.Properties;

/**
 * Maps a SoapUI Assertion to a model Assertion
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
public class SuiAssertion extends AbstractAssertion implements MutablePropertyHolder {

	private final SuiTestStep parent;

	public SuiAssertion(SuiTestStep parent, TestAssertion assertion) {
		this.parent = parent;
		this.id = EidFactory.getDefault().createFromStrAsUUID(assertion.getId());
		this.label = assertion.getLabel();
		this.description = assertion.getDescription();
		this.assertionType = assertion.getClass().getSimpleName();
		this.properties = new Properties();
		this.versionData = DefaultVersionDataFactory.getInstance().createHash(this.toString());

		if (assertion.isDisabled()) {
			this.assertionStatus = AssertionStatus.DEACTIVATED;
		} else {
			this.assertionStatus = AssertionStatus.IMPLEMENTED;
		}

		// Set the assertion expression
		if (assertion instanceof GroovyScriptAssertion) {
			final GroovyScriptAssertion gassert = (GroovyScriptAssertion) assertion;
			setExpressionAndExpectedResult(assertion, gassert.getScriptText(), "!false");
		} else if (assertion instanceof XPathContainsAssertion) {
			final XPathContainsAssertion xpassert = (XPathContainsAssertion) assertion;
			setExpressionAndExpectedResult(assertion, xpassert.getPath(), xpassert.getExpectedContent());
		} else if (assertion instanceof XQueryContainsAssertion) {
			final XQueryContainsAssertion xqassert = (XQueryContainsAssertion) assertion;
			setExpressionAndExpectedResult(assertion, xqassert.getPath(), xqassert.getExpectedContent());
		} else if (assertion instanceof SimpleContainsAssertion) {
			final SimpleContainsAssertion scassert = (SimpleContainsAssertion) assertion;
			setExpressionAndExpectedResult(assertion, scassert.getToken(), "true");
		} else if (assertion instanceof SimpleNotContainsAssertion) {
			final SimpleNotContainsAssertion scassert = (SimpleNotContainsAssertion) assertion;
			setExpressionAndExpectedResult(assertion, scassert.getToken(), "false");
		} else if (assertion instanceof ResponseSLAAssertion) {
			final ResponseSLAAssertion slaassert = (ResponseSLAAssertion) assertion;
			setExpressionAndExpectedResult(assertion, null, slaassert.getSLA());
		} else if (assertion instanceof ValidHttpStatusCodesAssertion ||
				assertion instanceof InvalidHttpStatusCodesAssertion) {
			XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(((WsdlMessageAssertion) assertion).getConfiguration());
			if (assertion instanceof ValidHttpStatusCodesAssertion) {
				this.expectedResult = "true";
			} else {
				this.expectedResult = "false";
			}
			setExpressionAndExpectedResult(assertion, reader.readString("codes", ""), this.expectedResult);
		} else if (assertion instanceof SchemaAssertion) {
			final SchemaAssertion schassert = (SchemaAssertion) assertion;
			setExpressionAndExpectedResult(assertion, schassert.getPathToXSD(), expectedResult);
		} else if (assertion instanceof OwsExceptionReportAssertion) {
			final OwsExceptionReportAssertion erassert = (OwsExceptionReportAssertion) assertion;
			setExpressionAndExpectedResult(assertion, erassert.XPATH_EXPRESSION, "false");
		}
	}

	private void setExpressionAndExpectedResult(final TestAssertion assertion,
			final String expression,
			final String expectedResult) {
		this.expression = PropertyExpander.expandProperties(assertion, expression);
		this.expectedResult = PropertyExpander.expandProperties(assertion, expectedResult);
	}

	@Override
	public ModelItem getParent() {
		return parent;
	}

	@Override
	public MutablePropertyHolder setProperty(String name, String value) {
		this.properties.setProperty(name, value);
		return this;
	}

	@Override
	public void removeProperty(String s) {
		this.properties.removeProperty(s);
	}

}
