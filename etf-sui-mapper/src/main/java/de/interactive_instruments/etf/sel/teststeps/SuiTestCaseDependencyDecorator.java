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
package de.interactive_instruments.etf.sel.teststeps;

import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;

import com.eviware.soapui.config.TestStepConfig;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlRunTestCaseTestStep;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.model.testsuite.*;
import com.eviware.soapui.security.SecurityTest;
import com.eviware.soapui.support.types.StringToObjectMap;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class SuiTestCaseDependencyDecorator implements TestCaseDependency, Comparable<SuiTestCaseDependencyDecorator> {

	private final TestCase testCase;
	private Collection<TestCaseDependency> dependencies = null;

	private SuiTestCaseDependencyDecorator(final TestCase testCase) {
		this.testCase = testCase;
	}

	@Override
	public Collection<TestCaseDependency> getDependencies() {
		if (dependencies == null) {
			dependencies = getDependencies(this.testCase);
			if (dependencies == null) {
				dependencies = Collections.EMPTY_LIST;
			}
		}
		return dependencies;
	}

	/*
	public static Collection<TestCaseDependency> getDependencies(final TestCase testCase) {
		final List<TestCaseDependencyTestStepDef> deps = new ArrayList<>();
		for (final TestStep testStep : testCase.getTestStepList()) {
			if(testStep instanceof TestCaseDependencyTestStepDef) {
				deps.add((TestCaseDependencyTestStepDef) testStep);
			}
		}
		if (!deps.isEmpty()) {
			return deps.stream().map(step -> new SuiTestCaseDependencyDecorator(step.getTargetTestCase())).collect(
					Collectors.toCollection(TreeSet::new));
		}
		return null;
	}
	*/

	public static Collection<TestCaseDependency> getDependencies(final TestCase testCase) {
		final List deps = new ArrayList<>();
		for (final TestStep testStep : testCase.getTestStepList()) {
			if (testStep instanceof WsdlRunTestCaseTestStep) {
				deps.add(testStep);
			} else if (testStep instanceof TestCaseDependencyTestStepDef) {
				deps.add(testStep);
			}
		}
		if (!deps.isEmpty()) {
			return (Collection<TestCaseDependency>) deps.stream().map(step -> new SuiTestCaseDependencyDecorator(
					(step instanceof WsdlRunTestCaseTestStep) ? ((WsdlRunTestCaseTestStep) step).getTargetTestCase()
							: ((TestCaseDependencyTestStepDef) step).getTargetTestCase()))
					.collect(
							Collectors.toCollection(TreeSet::new));
		}
		return null;
	}

	// delegates
	@Override
	public TestSuite getTestSuite() {
		return testCase.getTestSuite();
	}

	@Override
	public TestStep getTestStepAt(final int i) {
		return testCase.getTestStepAt(i);
	}

	@Override
	public int getIndexOfTestStep(final TestStep testStep) {
		return testCase.getIndexOfTestStep(testStep);
	}

	@Override
	public int getTestStepCount() {
		return testCase.getTestStepCount();
	}

	@Override
	public List<TestStep> getTestStepList() {
		return testCase.getTestStepList();
	}

	@Override
	public LoadTest getLoadTestAt(final int i) {
		return testCase.getLoadTestAt(i);
	}

	@Override
	public LoadTest getLoadTestByName(final String s) {
		return testCase.getLoadTestByName(s);
	}

	@Override
	public int getIndexOfLoadTest(final LoadTest loadTest) {
		return testCase.getIndexOfLoadTest(loadTest);
	}

	@Override
	public int getLoadTestCount() {
		return testCase.getLoadTestCount();
	}

	@Override
	public List<LoadTest> getLoadTestList() {
		return testCase.getLoadTestList();
	}

	@Override
	public TestCaseRunner run(final StringToObjectMap stringToObjectMap, final boolean b) {
		return testCase.run(stringToObjectMap, b);
	}

	@Override
	public void addTestRunListener(final TestRunListener testRunListener) {
		testCase.addTestRunListener(testRunListener);
	}

	@Override
	public void removeTestRunListener(final TestRunListener testRunListener) {
		testCase.removeTestRunListener(testRunListener);
	}

	@Override
	public int getTestStepIndexByName(final String s) {
		return testCase.getTestStepIndexByName(s);
	}

	@Override
	public <T extends TestStep> T findPreviousStepOfType(final TestStep testStep, final Class<T> aClass) {
		return testCase.findPreviousStepOfType(testStep, aClass);
	}

	@Override
	public <T extends TestStep> T findNextStepOfType(final TestStep testStep, final Class<T> aClass) {
		return testCase.findNextStepOfType(testStep, aClass);
	}

	@Override
	public <T extends TestStep> List<T> getTestStepsOfType(final Class<T> aClass) {
		return testCase.getTestStepsOfType(aClass);
	}

	@Override
	public void moveTestStep(final int i, final int i1) {
		testCase.moveTestStep(i, i1);
	}

	@Override
	public TestStep getTestStepByName(final String s) {
		return testCase.getTestStepByName(s);
	}

	@Override
	public TestStep getTestStepById(final UUID uuid) {
		return testCase.getTestStepById(uuid);
	}

	@Override
	public boolean isDisabled() {
		return testCase.isDisabled();
	}

	@Override
	public String getLabel() {
		return testCase.getLabel();
	}

	@Override
	public SecurityTest getSecurityTestAt(final int i) {
		return testCase.getSecurityTestAt(i);
	}

	@Override
	public SecurityTest getSecurityTestByName(final String s) {
		return testCase.getSecurityTestByName(s);
	}

	@Override
	public int getIndexOfSecurityTest(final SecurityTest securityTest) {
		return testCase.getIndexOfSecurityTest(securityTest);
	}

	@Override
	public int getSecurityTestCount() {
		return testCase.getSecurityTestCount();
	}

	@Override
	public List<SecurityTest> getSecurityTestList() {
		return testCase.getSecurityTestList();
	}

	@Override
	public TestStep insertTestStep(final TestStepConfig testStepConfig, final int i) {
		return testCase.insertTestStep(testStepConfig, i);
	}

	@Override
	public String getName() {
		return testCase.getName();
	}

	@Override
	public String getId() {
		return testCase.getId();
	}

	@Override
	public ImageIcon getIcon() {
		return testCase.getIcon();
	}

	@Override
	public String getDescription() {
		return testCase.getDescription();
	}

	@Override
	public Settings getSettings() {
		return testCase.getSettings();
	}

	@Override
	public List<? extends ModelItem> getChildren() {
		return testCase.getChildren();
	}

	@Override
	public ModelItem getParent() {
		return testCase.getParent();
	}

	@Override
	public Project getProject() {
		return testCase.getProject();
	}

	@Override
	public void addPropertyChangeListener(final String s, final PropertyChangeListener propertyChangeListener) {
		testCase.addPropertyChangeListener(s, propertyChangeListener);
	}

	@Override
	public void addPropertyChangeListener(final PropertyChangeListener propertyChangeListener) {
		testCase.addPropertyChangeListener(propertyChangeListener);
	}

	@Override
	public void removePropertyChangeListener(final PropertyChangeListener propertyChangeListener) {
		testCase.removePropertyChangeListener(propertyChangeListener);
	}

	@Override
	public void removePropertyChangeListener(final String s, final PropertyChangeListener propertyChangeListener) {
		testCase.removePropertyChangeListener(s, propertyChangeListener);
	}

	@Override
	public String[] getPropertyNames() {
		return testCase.getPropertyNames();
	}

	@Override
	public void setPropertyValue(final String s, final String s1) {
		testCase.setPropertyValue(s, s1);
	}

	@Override
	public String getPropertyValue(final String s) {
		return testCase.getPropertyValue(s);
	}

	@Override
	public TestProperty getProperty(final String s) {
		return testCase.getProperty(s);
	}

	@Override
	public Map<String, TestProperty> getProperties() {
		return testCase.getProperties();
	}

	@Override
	public void addTestPropertyListener(final TestPropertyListener testPropertyListener) {
		testCase.addTestPropertyListener(testPropertyListener);
	}

	@Override
	public void removeTestPropertyListener(final TestPropertyListener testPropertyListener) {
		testCase.removeTestPropertyListener(testPropertyListener);
	}

	@Override
	public boolean hasProperty(final String s) {
		return testCase.hasProperty(s);
	}

	@Override
	public ModelItem getModelItem() {
		return testCase.getModelItem();
	}

	@Override
	public int getPropertyCount() {
		return testCase.getPropertyCount();
	}

	@Override
	public List<TestProperty> getPropertyList() {
		return testCase.getPropertyList();
	}

	@Override
	public TestProperty getPropertyAt(final int i) {
		return testCase.getPropertyAt(i);
	}

	@Override
	public String getPropertiesLabel() {
		return testCase.getPropertiesLabel();
	}

	@Override
	public int compareTo(final SuiTestCaseDependencyDecorator o) {
		return this.getId().compareTo(o.getId());
	}
}
