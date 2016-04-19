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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.*;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.testsuite.TestStep;

import org.apache.xmlbeans.impl.values.XmlValueDisconnectedException;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.TimeUtils;
import de.interactive_instruments.Version;
import de.interactive_instruments.etf.model.item.DefaultVersionData;
import de.interactive_instruments.etf.model.item.DefaultVersionDataFactory;
import de.interactive_instruments.etf.model.item.EID;
import de.interactive_instruments.etf.model.item.ModelItem;
import de.interactive_instruments.etf.model.plan.Requirement;
import de.interactive_instruments.etf.model.specification.AbstractTestCase;
import de.interactive_instruments.etf.model.specification.Assertion;
import de.interactive_instruments.etf.model.specification.TestCaseStatus;
import de.interactive_instruments.etf.sel.Utils;
import de.interactive_instruments.etf.sel.model.impl.Separators;
import de.interactive_instruments.etf.sel.model.impl.result.SuiTestReport;
import de.interactive_instruments.etf.sel.model.mapping.ModelBridge;
import de.interactive_instruments.etf.sel.model.mapping.SuiRepository;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.properties.*;
import de.interactive_instruments.properties.Properties;

/**
 * Maps a TestCase to a model TestCase
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
public class SuiTestCase extends AbstractTestCase implements PropertyChangeListener {

	private SuiRepository associatedSource;

	private final static String propertyPrefix = "etf.";

	public SuiTestCase(
			final com.eviware.soapui.model.testsuite.TestCase suiTestCase,
			final SuiRepository source) throws ObjectWithIdNotFoundException {

		this.associatedSource = source;
		this.properties = new Properties();

		// Check for test steps that are added at runtime
		suiTestCase.addPropertyChangeListener(this);

		id = new EID(suiTestCase.getId());
		this.label = suiTestCase.getLabel();
		this.description = suiTestCase.getDescription();

		try {
			/*
			 *  The properties of the soapui testcase have to be read directly
			 *  because later a XmlDisconnect exception is thrown...
			 */
			properties.setProperty("ShortDescription",
					getTrimPropertyValueOrSetValue(suiTestCase, "ShortDescription", ""));

			properties.setProperty("Author",
					getTrimPropertyValueOrSetValue(
							suiTestCase, "Author", System.getProperty("user.name")));

			properties.setProperty("LastEditor",
					getTrimPropertyValueOrSetValue(
							suiTestCase, "LastEditor", System.getProperty("user.name")));

			final String expRes = getTrimPropertyValueOrSetValue(suiTestCase,
					"ExpectedResults", "");
			if (expRes != null && !expRes.isEmpty()) {
				expectedResults = Arrays.asList(expRes.split(Separators.MULTIPLE_VALUE));
			}

			final String strDate = getTrimPropertyValueOrSetValue(suiTestCase,
					"CreationDate", TimeUtils.dateToIsoString(new Date()));

			final String updateDate = getTrimPropertyValueOrSetValue(suiTestCase,
					"LastUpdateDate", TimeUtils.dateToIsoString(new Date()));

			final String strVer = getTrimPropertyValueOrSetValue(suiTestCase,
					"Version", "1.0.0");

			versionData = DefaultVersionDataFactory.getInstance().create(
					properties.getProperty("Author"), TimeUtils.string8601ToDate(strDate),
					properties.getProperty("updateDate"), TimeUtils.string8601ToDate(strDate),
					new Version(strVer), new byte[0]);

			final String assocReq = getTrimPropertyValueOrSetValue(suiTestCase,
					"AssociatedRequirements", "");
			if (assocReq != null && !assocReq.isEmpty()) {
				String[] requirementLabels = assocReq.split(Separators.MULTIPLE_VALUE);
				associatedRequirements = associatedSource.getRequirementsByNames(requirementLabels);
				if (associatedRequirements == null) {
					Utils.log("No requirements found for TestCase \"" +
							this.label + "\": " + assocReq);
				}
			}

			final String strStat = getTrimPropertyValueOrSetValue(suiTestCase,
					"Status", "IMPLEMENTED");
			if (strStat != null && !strStat.isEmpty()) {
				status = TestCaseStatus.valueOf(strStat.toUpperCase());
				if (status == TestCaseStatus.IMPLEMENTED && suiTestCase.isDisabled()) {
					status = TestCaseStatus.DEACTIVATED;
				}
			}

			updateTestSteps(suiTestCase);

		} catch (IllegalArgumentException e) {
			Utils.logError(e, "Unable to parse property in TestCase \"" +
					this.label + "\"");
		} catch (XmlValueDisconnectedException e) {
			Utils.logError(e);
		}
	}

	public void updateTestSteps(final com.eviware.soapui.model.testsuite.TestCase suiTestCase) {
		// TODO loop body into method addTestStep
		if (suiTestCase.getTestStepCount() > 0) {
			final ModelBridge modelBridge = (ModelBridge) ((WsdlProject) suiTestCase.getTestSuite().getProject()).getActiveEnvironment();
			for (TestStep suiTestStep : suiTestCase.getTestStepList()) {
				// Create a new entry in the cache
				addTestStep(modelBridge, suiTestStep);
			}
		}
	}

	private void addTestStep(final ModelBridge modelBridge, TestStep suiTestStep) {
		de.interactive_instruments.etf.model.specification.TestStep testStep = modelBridge.cacheTestStep(this, suiTestStep);
		if (testStep.getAssertions() != null) {
			for (final Assertion assertion : testStep.getAssertions()) {
				final String refReq = SUtils.leftOfSubStrOrNull(
						assertion.getLabel(), Separators.REQ_ASSERTION);
				if (refReq == null) {
					continue;
				}
				((SuiAssertion) assertion).setProperty("RequirementReference", refReq);
			}
		}
		if (this.testSteps == null) {
			this.testSteps = new ArrayList<>();
		}
		this.testSteps.add(testStep);
	}

	public void setAssertionRequirementRefs() {

		final HashMap<String, Requirement> allSubRequirements = new HashMap<>();

		for (final Requirement requirement : this.associatedRequirements) {
			final Set<Requirement> subReqs = requirement.getSubRequirements();
			if (!subReqs.isEmpty()) {
				for (Requirement subReq : subReqs) {
					allSubRequirements.put(subReq.getLabel(), subReq);
				}
			}
		}

		for (de.interactive_instruments.etf.model.specification.TestStep testStep : this.testSteps) {
			for (final Assertion assertion : testStep.getAssertions()) {
				final String refReq = SUtils.leftOfSubStrOrNull(
						assertion.getLabel(), Separators.REQ_ASSERTION);
				if (refReq == null) {
					continue;
				}

				Requirement reqForAssertion = allSubRequirements.get(refReq);
				if (reqForAssertion == null) {
					Utils.warn("No requirement for Assertion " + assertion.getLabel() +
							"found");
					continue;
				}

				((SuiAssertion) assertion).setProperty("RequirementReference", refReq);
			}
		}
	}

	/**
	 * Returns the trimmed property value or null (if string is empty)
	 */
	private String getTrimPropertyValueOrSetValue(
			final com.eviware.soapui.model.testsuite.TestCase suiTestCase,
			String key, String newValue) throws XmlValueDisconnectedException {
		final String value = suiTestCase.getPropertyValue(propertyPrefix + key);
		if (value != null && !value.trim().isEmpty()) {
			return value.trim();
		}
		suiTestCase.setPropertyValue(propertyPrefix + key, newValue);
		return newValue;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getPropertyName().equals("testSteps")) {
			if (event.getNewValue() != null) {
				// added
				final TestStep ts = (TestStep) event.getNewValue();
				final ModelBridge modelBridge = (ModelBridge) ((WsdlProject) ts.getTestCase().getTestSuite().getProject()).getActiveEnvironment();
				addTestStep(modelBridge, ts);
			} else if (this.testSteps != null) {
				// deleted
				final TestStep ts = (TestStep) event.getOldValue();
				updateTestSteps(ts.getTestCase());
			}
		} else if (event.getPropertyName().equals(
				"com.eviware.soapui.model.ModelItem@description")) {
			properties.setProperty("Description", (String) event.getNewValue());
		}
	}

	@Override
	public ModelItem getParent() {
		return null;
	}
}
