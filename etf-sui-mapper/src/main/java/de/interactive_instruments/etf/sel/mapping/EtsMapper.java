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

import static de.interactive_instruments.etf.EtfConstants.*;
import static de.interactive_instruments.etf.sel.mapping.Types.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.support.http.HttpRequestTestStep;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlGroovyScriptTestStep;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.basic.*;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.testsuite.TestAssertion;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestStep;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.security.assertion.InvalidHttpStatusCodesAssertion;
import com.eviware.soapui.security.assertion.ValidHttpStatusCodesAssertion;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;

import org.slf4j.LoggerFactory;

import de.interactive_instruments.LogUtils;
import de.interactive_instruments.SUtils;
import de.interactive_instruments.TimeUtils;
import de.interactive_instruments.UriUtils;
import de.interactive_instruments.container.Pair;
import de.interactive_instruments.etf.dal.dto.MetaDataItemDto;
import de.interactive_instruments.etf.dal.dto.RepositoryItemDto;
import de.interactive_instruments.etf.dal.dto.capabilities.TestObjectTypeDto;
import de.interactive_instruments.etf.dal.dto.test.*;
import de.interactive_instruments.etf.model.EID;
import de.interactive_instruments.etf.model.EidFactory;
import de.interactive_instruments.etf.sel.assertions.OwsExceptionReportAssertion;
import de.interactive_instruments.etf.sel.assertions.SchemaAssertion;
import de.interactive_instruments.etf.sel.teststeps.SuiTestCaseDependencyDecorator;
import de.interactive_instruments.etf.sel.teststeps.TestCaseDependency;
import de.interactive_instruments.exceptions.ExcUtils;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class EtsMapper {

	public static String DEFAULT_USERNAME = SoapUI.isCommandLine() ? "unknown" : System.getProperty("user.name");
	public static String DEFAULT_VERSION = "1.0.0";
	private Date fallbackDate = new Date();

	private final WsdlProject project;

	public EtsMapper(final WsdlProject project) {
		this.project = project;
	}

	private void setFromProperties(final RepositoryItemDto item, final TestPropertyHolder suiItem) {
		item.setAuthor(SUtils.nonNullEmptyOrDefault(suiItem.getPropertyValue(ETF_AUTHOR_PK), DEFAULT_USERNAME));
		item.setLastEditor(SUtils.nonNullEmptyOrDefault(suiItem.getPropertyValue(ETF_LAST_EDITOR_PK), DEFAULT_USERNAME));
		if (suiItem.hasProperty(ETF_CREATION_DATE_PK)) {
			try {
				item.setCreationDate(TimeUtils.string8601ToDate(suiItem.getPropertyValue(ETF_CREATION_DATE_PK)));
			} catch (IllegalArgumentException e) {
				item.setCreationDate(fallbackDate);
			}
		} else {
			item.setCreationDate(fallbackDate);
		}
		if (suiItem.hasProperty(ETF_LAST_UPDATE_DATE_PK)) {
			try {
				item.setLastUpdateDate(TimeUtils.string8601ToDate(suiItem.getPropertyValue(ETF_LAST_UPDATE_DATE_PK)));
			} catch (IllegalArgumentException e) {
				item.setLastUpdateDate(fallbackDate);
			}
		} else {
			item.setLastUpdateDate(fallbackDate);
		}
		item.setVersionFromStr(SUtils.nonNullEmptyOrDefault(suiItem.getPropertyValue(ETF_VERSION_PK), DEFAULT_VERSION));
	}

	private void setFromModelItem(final MetaDataItemDto item, final ModelItem suiItem) {
		item.setId(EidFactory.getDefault().createUUID(suiItem.getId()));
		item.setLabel(suiItem.getName());
		item.setDescription(suiItem.getDescription());
		if (suiItem instanceof TestPropertyHolder) {
			item.setReference(((TestPropertyHolder) suiItem).getPropertyValue(ETF_REFERENCE_PK));
		}
	}

	public ExecutableTestSuiteDto toTestTaskResult() {
		final ExecutableTestSuiteDto etsDto = new ExecutableTestSuiteDto();
		setFromProperties(etsDto, project);
		setFromModelItem(etsDto, project);
		etsDto.setLocalPath(project.getPath());
		final File projectFile = new File(project.getPath());
		try {
			final String hash = UriUtils.hashFromTimestampOrContent(projectFile.toURI());
			etsDto.setItemHash(hash);
		} catch (IOException e) {
			ExcUtils.suppress(e);
			etsDto.setItemHash("0");
		}

		if (project.hasProperty(ETF_SUPPORTED_TESTOBJECT_TYPE_IDS_PK)) {
			final String[] ids = project.getPropertyValue(ETF_SUPPORTED_TESTOBJECT_TYPE_IDS_PK).split(",");
			for (int i = 0; i < ids.length; i++) {
				final EID eid = EidFactory.getDefault().createUUID(ids[i].trim());
				final TestObjectTypeDto testObjectTypeDto = SUI_SUPPORTED_TEST_OBJECT_TYPES.get(eid);
				if (testObjectTypeDto == null) {
					LoggerFactory.getLogger(EtsMapper.class).error(
							LogUtils.FATAL_MESSAGE, "Could not load Test Object Type  {} for Executable Test Suite {}",
							eid, etsDto.getId());
				} else {
					etsDto.addSupportedTestObjectType(testObjectTypeDto);
				}
			}
		} else {
			// SIMPLE_WEB_SERVICE_TOT
			etsDto.addSupportedTestObjectType(SUI_SUPPORTED_TEST_OBJECT_TYPES.get("88311f83-818c-46ed-8a9a-cec4f3707365"));
		}

		final Map<String, TestCaseDto> allTestCases = new HashMap<>();
		final List<Pair<TestCaseDto, Collection<TestCaseDependency>>> testCasesRequiringDeps = new ArrayList<>();

		if (project.getTestSuiteList() != null) {
			for (int tsi = 0; tsi < project.getTestSuiteCount(); tsi++) {
				final TestSuite testSuite = project.getTestSuiteAt(tsi);
				final TestModuleDto testModuleDto = new TestModuleDto();
				setFromModelItem(testModuleDto, testSuite);
				if (testSuite.getTestCaseList() != null) {
					for (int tci = 0; tci < testSuite.getTestCaseCount(); tci++) {
						final TestCase testCase = testSuite.getTestCaseAt(tci);
						final TestCaseDto testCaseDto = new TestCaseDto();
						setFromModelItem(testCaseDto, testCase);
						testCaseDto.setTestSteps(testStepsToDto(testCase.getTestStepList()));
						testModuleDto.addTestCase(testCaseDto);
						allTestCases.put(testCase.getId(), testCaseDto);
						final Collection<TestCaseDependency> deps = SuiTestCaseDependencyDecorator.getDependencies(testCase);
						if (deps != null) {
							testCasesRequiringDeps.add(new Pair<>(testCaseDto, deps));
						}
					}
				}
				etsDto.addTestModule(testModuleDto);
			}
		}
		for (final Pair<TestCaseDto, Collection<TestCaseDependency>> testCasePair : testCasesRequiringDeps) {
			final Collection<TestCaseDependency> deps = testCasePair.getRight();
			for (final TestCaseDependency dep : deps) {
				final TestCaseDto testCaseDto = allTestCases.get(dep.getId());
				if (testCaseDto != null) {
					testCasePair.getLeft().addDependency(testCaseDto);
				}
			}
		}

		return etsDto;
	}

	private List<TestStepDto> testStepsToDto(final List<TestStep> testStepList) {
		if (testStepList == null || testStepList.isEmpty()) {
			return null;
		}
		final List<TestStepDto> testSteps = new ArrayList<>(testStepList.size());
		for (int i = 0, testStepListSize = testStepList.size(); i < testStepListSize; i++) {
			final TestStep testStep = testStepList.get(i);
			if (testStep instanceof HttpRequestTestStep || testStep instanceof WsdlGroovyScriptTestStep) {
				final TestStepDto testStepDto = new TestStepDto();
				testStepDto.setId(EidFactory.getDefault().createUUID(testStep.getId()));
				testStepDto.setLabel(testStep.getName());
				testStepDto.setDescription(testStep.getDescription());

				if (testStep instanceof HttpRequestTestStep) {
					testStepDto.setType(HTTP_REQUEST_STEP_IT);
					final HttpRequestTestStep testRequest = (HttpRequestTestStep) testStep;
					testStepDto.setTestAssertions(assertionsToDto(testRequest.getAssertionList()));
					testStepDto.setStatementForExecution("NOT_APPLICABLE");
				} else if (testStep instanceof WsdlGroovyScriptTestStep) {
					testStepDto.setType(GROOVY_STEP_IT);
					testStepDto.setStatementForExecution(
							((WsdlGroovyScriptTestStep) testStep).getScript());
				}
				testSteps.add(testStepDto);
			}
		}
		return testSteps;
	}

	private List<TestAssertionDto> assertionsToDto(final List<TestAssertion> assertionList) {
		if (assertionList == null || assertionList.isEmpty()) {
			return null;
		}
		final List<TestAssertionDto> testAssertions = new ArrayList<>(assertionList.size());
		for (int i = 0, assertionListSize = assertionList.size(); i < assertionListSize; i++) {
			final TestAssertion assertion = assertionList.get(i);
			final TestAssertionDto testAssertionDto = new TestAssertionDto();
			setFromModelItem(testAssertionDto, assertion);

			if (assertion.isDisabled()) {
				testAssertionDto.setType(DISABLED_ASSERTION_IT);
				testAssertionDto.setExpression("NOT_APPLICABLE");
				testAssertionDto.setExpectedResult("NOT_APPLICABLE");
			} else if (assertion instanceof GroovyScriptAssertion) {
				final GroovyScriptAssertion gassert = (GroovyScriptAssertion) assertion;
				testAssertionDto.setType(GROOVY_ASSERTION_IT);
				testAssertionDto.setExpression(gassert.getScriptText());
				testAssertionDto.setExpectedResult("NOT_APPLICABLE");
			} else if (assertion instanceof XPathContainsAssertion) {
				final XPathContainsAssertion xpassert = (XPathContainsAssertion) assertion;
				testAssertionDto.setType(XPATH_MATCH_ASSERTION_IT);
				testAssertionDto.setExpression(xpassert.getPath());
				testAssertionDto.setExpectedResult(xpassert.getExpectedContent());
			} else if (assertion instanceof XQueryContainsAssertion) {
				final XQueryContainsAssertion xqassert = (XQueryContainsAssertion) assertion;
				testAssertionDto.setType(XQUERY_MATCH_ASSERTION_IT);
				testAssertionDto.setExpression(xqassert.getPath());
				testAssertionDto.setExpectedResult(xqassert.getExpectedContent());
			} else if (assertion instanceof SimpleContainsAssertion) {
				final SimpleContainsAssertion scassert = (SimpleContainsAssertion) assertion;
				testAssertionDto.setType(BASIC_ASSERTION_IT);
				testAssertionDto.setExpression(scassert.getToken());
				testAssertionDto.setExpectedResult("exists");
			} else if (assertion instanceof SimpleNotContainsAssertion) {
				final SimpleNotContainsAssertion scassert = (SimpleNotContainsAssertion) assertion;
				testAssertionDto.setType(BASIC_ASSERTION_IT);
				testAssertionDto.setExpression(scassert.getToken());
				testAssertionDto.setExpectedResult("not exists");
			} else if (assertion instanceof ResponseSLAAssertion) {
				final ResponseSLAAssertion slaassert = (ResponseSLAAssertion) assertion;
				testAssertionDto.setType(BASIC_ASSERTION_IT);
				testAssertionDto.setExpression(slaassert.getSLA());
				testAssertionDto.setExpectedResult("NOT_APPLICABLE");
			} else if (assertion instanceof ValidHttpStatusCodesAssertion ||
					assertion instanceof InvalidHttpStatusCodesAssertion) {
				XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(
						((WsdlMessageAssertion) assertion).getConfiguration());
				if (assertion instanceof ValidHttpStatusCodesAssertion) {
					testAssertionDto.setExpectedResult("exist");
				} else {
					testAssertionDto.setExpectedResult("not exist");
				}
				testAssertionDto.setType(BASIC_ASSERTION_IT);
				testAssertionDto.setExpression(reader.readString("codes", ""));
			} else if (assertion instanceof SchemaAssertion) {
				final SchemaAssertion schassert = (SchemaAssertion) assertion;
				testAssertionDto.setType(SCHEMA_ASSERTION_IT);
				testAssertionDto.setExpression(schassert.getPathToXSD());
				testAssertionDto.setExpectedResult("NOT_APPLICABLE");
			} else if (assertion instanceof OwsExceptionReportAssertion) {
				final OwsExceptionReportAssertion erassert = (OwsExceptionReportAssertion) assertion;
				testAssertionDto.setType(XPATH_MATCH_ASSERTION_IT);
				testAssertionDto.setExpression(erassert.XPATH_EXPRESSION);
				testAssertionDto.setExpectedResult("false");
			} else {
				testAssertionDto.setType(DISABLED_ASSERTION_IT);
				testAssertionDto.setExpression("NOT_APPLICABLE");
				testAssertionDto.setExpectedResult("NOT_APPLICABLE");
			}
			testAssertions.add(testAssertionDto);

		}
		return testAssertions;
	}
}
