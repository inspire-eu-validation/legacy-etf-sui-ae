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
package de.interactive_instruments.etf.sel.model.mapping;

import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlTestSuite;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.testsuite.TestProperty;
import com.eviware.soapui.model.testsuite.TestSuite;

import de.interactive_instruments.Releasable;
import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.model.plan.Requirement;
import de.interactive_instruments.etf.model.specification.AbstractTestCase;
import de.interactive_instruments.etf.model.specification.TestCase;
import de.interactive_instruments.etf.sel.Utils;
import de.interactive_instruments.etf.sel.model.impl.Separators;
import de.interactive_instruments.etf.sel.model.impl.plan.SuiRequirement;
import de.interactive_instruments.etf.sel.model.impl.spec.SuiTestCase;
import de.interactive_instruments.exceptions.ExcUtils;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.properties.KVP;
import de.interactive_instruments.properties.KVPImpl;

/**
 * Repository for SoapUI Test Cases and Requirements
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
final public class SuiRepository implements Releasable {

	private Map<String, SuiRequirement> idRequirementsMap;
	private Map<String, SuiRequirement> nameRequirementsMap;

	final private Map<String, SuiTestCase> testCases;

	public SuiRepository(Project project) throws ObjectWithIdNotFoundException {

		idRequirementsMap = new TreeMap<String, SuiRequirement>();
		testCases = new TreeMap<String, SuiTestCase>();

		final WsdlTestSuite tsTS = (WsdlTestSuite) project.getTestSuiteByName("TestSetup");
		if (tsTS != null) {
			final WsdlTestCase requirementsTestCase = tsTS.getTestCaseByName("Requirements");
			if (requirementsTestCase != null) {
				// For Requirements Id and SubID assignment
				final Map<String, String> reqSubIdsAndParentIds = new TreeMap<String, String>();

				// Add the Requirements in the map as id,req pairs
				final List<TestProperty> properties = requirementsTestCase.getPropertyList();
				for (final TestProperty property : properties) {

					final KVP<String> idAndProp = KVPImpl.createOrNull(
							property.getName(), Separators.ID_PROPERTY);
					if (idAndProp != null) {
						setOrAddRequirement(idAndProp.getKey(),
								idAndProp.getValue(), property.getValue());

						// Check if the this is a subrequirement x.x
						final KVP<String> idAndSubId = KVPImpl.createOrNull(
								idAndProp.getKey(), Separators.SUB_REQ);
						if (idAndSubId != null) {
							// Insert PREFIX + SubId, Parent id
							reqSubIdsAndParentIds.put(idAndProp.getKey(), idAndSubId.getKey());
						}
					}
				}
				// Add all sub Requirements x.x to the parent requirements
				for (final Entry<String, String> entry : reqSubIdsAndParentIds.entrySet()) {
					final String parentAndSubReqId = entry.getKey();
					final String subReqId = SUtils.rigthOfSubStrOrNull(
							parentAndSubReqId, Separators.SUB_REQ);
					final Requirement subRequriment = idRequirementsMap.get(parentAndSubReqId);
					final Requirement parentRequriment = idRequirementsMap.get(entry.getValue());
					// Assign sub requirements to parent requirements without Parent id
					if (parentRequriment == null) {
						Utils.warn("Skipping sub requirement " +
								parentAndSubReqId + " which has no parent requirement!");
					} else {
						((SuiRequirement) parentRequriment).addSubRequirement(subRequriment);
					}
				}

				// Reorganize: kvp: id,req to kvp: name,req
				nameRequirementsMap = new TreeMap<String, SuiRequirement>();
				for (final SuiRequirement req : idRequirementsMap.values()) {
					final String name = req.getLabel();
					if (name == null || name.isEmpty()) {
						Utils.warn("Requirement with id " + req.getId() +
								" is unamed!");
						continue;
					}
					nameRequirementsMap.put(name, req);
				}
			}
		}

		// Check if every TestCase has the required properties
		for (TestSuite ts : ((WsdlProject) project).getTestSuiteList()) {
			final WsdlTestSuite wts = (WsdlTestSuite) ts;
			for (final com.eviware.soapui.model.testsuite.TestCase _tc : wts.getTestCaseList()) {
				testCases.put(_tc.getId(), new SuiTestCase(_tc, this));
			}
		}

	}

	private void setOrAddRequirement(String id, String property, String value) {
		SuiRequirement requirement = idRequirementsMap.get(id);
		if (requirement == null) {
			requirement = new SuiRequirement(id);
			idRequirementsMap.put(id, requirement);
		}
		requirement.setProperty(property, value);
	}

	public Requirement getRequirementById(String id) {
		return idRequirementsMap.get(id);
	}

	public Requirement getRequirementByName(String name) {
		return nameRequirementsMap.get(name);
	}

	public SuiTestCase getTestCaseById(String id) {
		return testCases.get(id);
	}

	public TestCase getTestCaseByName(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<Requirement> getRequirementsByIds(String[] ids) {
		final Set<Requirement> retReq = new TreeSet<>();
		for (final String id : ids) {
			final Requirement req = idRequirementsMap.get(id);
			if (req != null) {
				retReq.add(req);
			}
		}
		return retReq;
	}

	public Set<Requirement> getRequirementsByNames(String[] names) {
		// names == ids in this impelmentation
		return getRequirementsByIds(names);
	}

	public TestCase deleteTestCaseById(String id) {
		return testCases.remove(id);
	}

	public TestCase deleteTestCaseByName(String name) {
		for (final TestCase testCase : testCases.values()) {
			if (name.equals(testCase.getLabel())) {
				return testCases.remove(testCase.getId());
			}
		}
		return null;
	}

	public void addTestCase(final com.eviware.soapui.model.testsuite.TestCase _tc) {
		try {
			testCases.put(_tc.getId(), new SuiTestCase(_tc, this));
		} catch (ObjectWithIdNotFoundException e) {
			ExcUtils.supress(e);
			Utils.logError(e);
		}
	}

	// Not needed

	public void updateTestCaseById(String id, AbstractTestCase testCase) {}

	public void updateTestCaseByName(String name, AbstractTestCase testCase) {}

	public Collection<SuiRequirement> getRequirements() {
		return this.idRequirementsMap.values();
	}

	public Collection<SuiTestCase> getTestCases() {
		return this.testCases.values();
	}

	@Override
	public void release() {
		if (idRequirementsMap != null) {
			idRequirementsMap.clear();
		}
		if (nameRequirementsMap != null) {
			nameRequirementsMap.clear();
		}
		if (testCases != null) {
			testCases.clear();
		}
	}
}
