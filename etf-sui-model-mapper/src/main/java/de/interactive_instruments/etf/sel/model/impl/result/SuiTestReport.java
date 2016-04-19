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

import java.net.URI;
import java.util.*;

import com.eviware.soapui.SoapUI;

import de.interactive_instruments.container.ContainerFactory;
import de.interactive_instruments.etf.model.item.EID;
import de.interactive_instruments.etf.model.plan.Requirement;
import de.interactive_instruments.etf.model.plan.TestObject;
import de.interactive_instruments.etf.model.result.AbstractTestReport;
import de.interactive_instruments.etf.model.result.TestCaseResult;
import de.interactive_instruments.etf.model.result.TestReportEventListener;
import de.interactive_instruments.etf.model.specification.TestCase;
import de.interactive_instruments.etf.sel.Utils;
import de.interactive_instruments.etf.sel.model.impl.plan.SuiRequirement;
import de.interactive_instruments.etf.sel.model.impl.spec.SuiTestCase;
import de.interactive_instruments.etf.sel.model.mapping.SuiRepository;
import de.interactive_instruments.exceptions.ImmutableLockException;
import de.interactive_instruments.properties.*;
import de.interactive_instruments.properties.Properties;

/**
 * Maps a SoapUI TestReport to a model TestReport
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class SuiTestReport extends AbstractTestReport {

	private Map<String, SuiTestSuiteResult> testSuiteResultsMap;
	private SuiRepository repo;
	private Map<String, List<TestCaseResult>> queuedCaseResults;

	public SuiTestReport(ContainerFactory appendixFactory,
			String username, URI publicationLocation,
			EID id, String label, TestObject testObject) {
		super(id, username, appendixFactory);
		this.generator = "ETF-SEL-" + Utils.SEL_VERSION;
		this.testingTool = "SoapUI " + SoapUI.SOAPUI_VERSION;
		this.publicationLocation = publicationLocation;
		this.label = label;
		this.testObject = testObject;
		this.testSuiteResultsMap = new TreeMap<>();
		this.startTimestamp = new Date();
	}

	public synchronized void queueGeneratedTestCaseResult(final String testSuiteName, final TestCaseResult testCaseResult)
			throws ImmutableLockException {
		if (queuedCaseResults == null) {
			queuedCaseResults = new HashMap<>();
			queuedCaseResults.put(testSuiteName, new ArrayList<TestCaseResult>() {
				{
					add(testCaseResult);
				}
			});
		} else {
			final List<TestCaseResult> oldVal = queuedCaseResults.putIfAbsent(testSuiteName, new ArrayList<TestCaseResult>() {
				{
					add(testCaseResult);
				}
			});
			if (oldVal != null) {
				oldVal.add(testCaseResult);
			}
		}
	}

	public synchronized void addTestCaseResult(final String testSuiteName, final TestCaseResult testCaseResult)
			throws ImmutableLockException {
		lock.checkLock(this);
		SuiTestSuiteResult suite = testSuiteResultsMap.get(testSuiteName);
		if (suite == null) {
			suite = new SuiTestSuiteResult(testSuiteName);
			this.testSuiteResults.add(suite);
			this.testSuiteResultsMap.put(testSuiteName, suite);
		}
		suite.addResult(testCaseResult);

		// Add generated test case results
		if (queuedCaseResults != null && queuedCaseResults.containsKey(testSuiteName)) {
			final List<TestCaseResult> queuedCaseResultsList = queuedCaseResults.get(testSuiteName);
			for (int i = 0; i < queuedCaseResultsList.size(); i++) {
				suite.addResult(queuedCaseResultsList.get(i));

			}
			queuedCaseResultsList.clear();
			queuedCaseResults.remove(testSuiteName);
		}
		doFireAddedEvent(testCaseResult);
	}

	public void setSuiRepository(SuiRepository repo) {
		this.repo = repo;
	}

	@Override
	public SortedSet<Requirement> getRequirements() {
		return new TreeSet<Requirement>(repo.getRequirements());
	}

	@Override
	public SortedSet<TestCase> getTestCases() {
		return new TreeSet<TestCase>(repo.getTestCases());
	}

}
