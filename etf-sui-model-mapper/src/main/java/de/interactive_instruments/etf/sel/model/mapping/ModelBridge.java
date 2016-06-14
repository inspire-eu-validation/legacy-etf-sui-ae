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

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.eviware.soapui.config.ServiceConfig.Type.Enum;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.environment.Environment;
import com.eviware.soapui.model.environment.Property;
import com.eviware.soapui.model.environment.Service;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestSuite;

import de.interactive_instruments.TimeUtils;
import de.interactive_instruments.concurrent.InvalidStateTransitionException;
import de.interactive_instruments.container.ContainerFactory;
import de.interactive_instruments.etf.dal.IReportDep;
import de.interactive_instruments.etf.dal.assembler.AssemblerException;
import de.interactive_instruments.etf.dal.basex.dao.BasexTestReportDao;
import de.interactive_instruments.etf.dal.dao.TestReportDao;
import de.interactive_instruments.etf.dal.dto.result.TestReportDto;
import de.interactive_instruments.etf.model.result.TestReport;
import de.interactive_instruments.etf.model.specification.TestStep;
import de.interactive_instruments.etf.sel.Utils;
import de.interactive_instruments.etf.sel.model.impl.result.SuiTestReport;
import de.interactive_instruments.etf.sel.model.impl.spec.SuiTestCase;
import de.interactive_instruments.etf.sel.model.impl.spec.SuiTestStep;
import de.interactive_instruments.exceptions.InitializationException;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;
import de.interactive_instruments.exceptions.config.ConfigurationException;
import de.interactive_instruments.properties.Properties;

/**
 * This Object acts as model bridge between the static
 * mirrored SoapUI/etf model, the TestResults that
 * will be generated at runtime and a TestRunner that might inject
 * the TestReport into this TestEnviornment.
 *
 * The ModelBridge is injected into a SoapUI Project object as
 * Environment object so it may be accessed via getActiveEnvironment()
 * from the Result model objects.
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
public class ModelBridge implements Environment, IReportDep {

	/**
	 * Cache for TestSteps
	 */
	final Map<String, TestStep> sTestSteps = new ConcurrentHashMap<String, TestStep>();

	/**
	 *  Associated project
	 */
	final Project project;

	/**
	 * lazy loaded if needed in GUI environment
	 */
	private TestReportDao testReportDao;

	/**
	 *  Lazy loaded in GUI environment via testReportDao or injected by TestRunner via
	 *  IReportDep interface
	 */
	private SuiTestReport report;

	/**
	 * Only lazy loaded if accessed via getSuiRepository() method.
	 */
	private SuiRepository suiRepository;

	/**
	 * Can be used to propagate the reports without a
	 * etf source until the source is available
	 */
	// for adding testcases if no source is available
	private Map<String, TestCase> presavedTestCases;

	private SuiTestReportAssembler asm;

	///////////////////////////////////////////////
	// ModelBridge creation and access
	///////////////////////////////////////////////

	private ModelBridge(final WsdlProject project) throws ObjectWithIdNotFoundException {
		this.project = project;
		this.testReportDao = null;
		this.report = null;
		this.suiRepository = null;
		this.presavedTestCases = new TreeMap<String, TestCase>();

		project.setActiveEnvironment(this);
	}

	/**
	 * Returns true if the the ModelBridge is available for the SoapUI Project
	 */

	public static boolean isBridgeInjected(Project proj) {
		final WsdlProject wsdlProj = (WsdlProject) proj;
		return (wsdlProj.getActiveEnvironment() instanceof ModelBridge);
	}

	/**
	 * Creates a local report file testReportDao and a new report
	 */
	private void lazyInitStoreAndReport() throws StoreException, AssemblerException, IOException {
		if (this.testReportDao == null && Utils.SEL_REPORT_DIR != null) {
			Utils.log("Initializing data source...");
			Properties config = new Properties();
			config.setProperty("etf.datasource.dir", Utils.SEL_REPORT_DIR.getAbsolutePath());
			config.setProperty("etf.reports.base.uri", Utils.SEL_REPORT_DIR.getAbsolutePath());
			config.setProperty("etf.appendices.dir", Utils.SEL_REPORT_DIR.getAbsolutePath());
			config.setProperty("etf.datasource.dir", Utils.SEL_DS_DIR.getAbsolutePath());
			try {
				this.testReportDao = new BasexTestReportDao();
				this.testReportDao.getConfigurationProperties().setPropertiesFrom(config, true);
				this.testReportDao.init();
			} catch (InitializationException | InvalidStateTransitionException | ConfigurationException e) {
				throw new StoreException(e.getMessage());
			}
			this.asm = new SuiTestReportAssembler();
			Utils.log("Initialized.");
		}

		if (this.report == null) {
			this.report = asm.assembleEntity(this.testReportDao.create(
					project.getName(), new SuiTestObject(project.getPropertyValue("serviceEndpoint"))));
			this.report.init();
		}
	}

	/**
	 * Creates a local etf Source
	 */
	private void lazyInitSource() {
		Utils.log("Mapping mirror model...");
		long start = System.currentTimeMillis();
		try {
			// Create a new source.
			this.suiRepository = new SuiRepository(project);
			Utils.log("Done. Duration: " +
					TimeUtils.currentDurationAsMinsSeconds(start));
			// Add eventually presaved TestCases
			for (TestCase tc : this.presavedTestCases.values()) {
				this.suiRepository.addTestCase(tc);
			}
			this.presavedTestCases = null;
			// And map these test cases
			this.mapTestSuites();
			this.report.setSuiRepository(this.suiRepository);
		} catch (ObjectWithIdNotFoundException e) {
			Utils.logError(e);
		}
	}

	/**
	 * Maps TestSuites and their test cases
	 */
	private void mapTestSuites() {
		for (final TestSuite ts : project.getTestSuiteList()) {
			for (final TestCase tc : ts.getTestCaseList()) {
				this.suiRepository.addTestCase(tc);
			}
		}
	}

	/**
	 * Returns a already injected bridge or creates a new one
	 *
	 * @param proj
	 * @return
	 */
	public static ModelBridge getOrCreateEnvBridge(final Project proj) {
		final WsdlProject wsdlProj = (WsdlProject) proj;
		if (wsdlProj.getActiveEnvironment() instanceof ModelBridge) {
			// Bridge injected, so return it
			return (ModelBridge) wsdlProj.getActiveEnvironment();
		} else {
			// Bridge not injected, so create a new one.
			try {
				Utils.log("Injecting model bridge...");
				final ModelBridge bridge = new ModelBridge(wsdlProj);
				Utils.log("Bridge injected.");
				return bridge;
			} catch (ObjectWithIdNotFoundException e) {
				Utils.logError(e);
			}
		}
		return null;
	}

	/**
	 * Sets the DefaultEnvironment
	 *
	 * @param proj
	 */
	public static void resetModelBridge(final Project proj) {
		final WsdlProject wsdlProj = (WsdlProject) proj;
		if (wsdlProj.getActiveEnvironment() instanceof ModelBridge) {
			wsdlProj.getActiveEnvironment().release();
			// Bridge not injected, so create a new one.
			try {
				Utils.log("Injecting model bridge...");
				final ModelBridge bridge = new ModelBridge(wsdlProj);
				Utils.log("Bridge injected.");
			} catch (ObjectWithIdNotFoundException e) {
				Utils.logError(e);
			}
		}
	}

	///////////////////////////////////////////////
	///////////////////////////////////////////////

	/**
	 * Adds a TestCase also if no source is available
	 */
	public void addTestCasePrxy(final TestCase testCase) {
		if (this.suiRepository != null) {
			this.suiRepository.addTestCase(testCase);
		} else {
			presavedTestCases.put(testCase.getId(), testCase);
		}
	}

	/**
	 * Deletes a TestCase also if no source is available
	 */
	public void deleteTestCaseByIdPrxy(final String id) {
		if (this.suiRepository != null) {
			this.suiRepository.deleteTestCaseById(id);
		} else {
			presavedTestCases.remove(id);
		}
	}

	/**
	 * Get a mirrored TestStep, identified by the SoapUI TestStep ID
	 *
	 * @param suiTestStep
	 * @return
	 * @throws ObjectWithIdNotFoundException
	 */
	public synchronized TestStep getTestStep(
			final com.eviware.soapui.model.testsuite.TestStep suiTestStep)
					throws ObjectWithIdNotFoundException {
		final TestStep ts = sTestSteps.get(suiTestStep.getId());
		if (ts == null) {
			throw new ObjectWithIdNotFoundException(suiTestStep.getId());
		}
		return ts;
	}

	/**
	 * Creates a model mirrored TestStep, identified by the SoapUI TestStep ID
	 *
	 * @param suiTestStep
	 * @return
	 */
	public TestStep cacheTestStep(
			SuiTestCase parentTestCase,
			final com.eviware.soapui.model.testsuite.TestStep suiTestStep) {
		final TestStep ts = new SuiTestStep(parentTestCase, suiTestStep);
		sTestSteps.put(suiTestStep.getId(), ts);
		return ts;
	}

	/**
	 * Removes a model mirrored TestStep, identified by the SoapUI TestStep ID
	 *
	 * @param suiTestStep
	 * @return
	 */
	public void removeTestStep(
			final com.eviware.soapui.model.testsuite.TestStep suiTestStep) {
		sTestSteps.remove(suiTestStep.getId());
	}

	/**
	 * Returns a factory for appendix items
	 *
	 * @return
	 */
	public ContainerFactory getAppendixFactory() throws StoreException, AssemblerException, IOException {
		return this.getReport().getAppendixFactory();
	}

	/**
	 * Returns the source for model items (mapped to SoapUI model items)
	 *
	 * @return
	 */
	public SuiRepository getSuiRepository() {
		if (suiRepository != null) {
			return suiRepository;
		}
		lazyInitSource();
		return this.suiRepository;
	}

	/**
	 * Test report that holds the results
	 *
	 * @return
	 */
	public SuiTestReport getReport() throws StoreException, IOException, AssemblerException {
		if (this.report != null) {
			return report;
		}
		lazyInitStoreAndReport();
		return report;
	}

	/**
	 * A Store for local test reports
	 *
	 * @return
	 */
	public TestReportDao getReportDao() {
		if (this.testReportDao != null) {
			return testReportDao;
		}
		// lazyInitStoreAndReport();
		return testReportDao;
	}

	/**
	 * The identifier for the model bridge
	 */
	@Override
	public String getName() {
		// a wonderful name..
		return "etf-sel.modelBridge";
	}

	/**
	 * The associated project
	 */
	@Override
	public Project getProject() {
		return project;
	}

	/**
	 * Interface for Setter Injection (will be injected by a runner)
	 */
	@Override
	public void injectReport(ContainerFactory factory, final TestReport report) {
		this.report = (SuiTestReport) report;
		this.testReportDao = null;
	}

	@Override
	public void release() {
		if (this.sTestSteps != null) {
			this.sTestSteps.clear();
		}
		if (testReportDao != null) {
			testReportDao.release();
			testReportDao = null;
		}
		if (suiRepository != null) {
			suiRepository.release();
			this.suiRepository = null;
		}
		this.report = null;
		if (this.presavedTestCases != null) {
			this.presavedTestCases.clear();
			this.presavedTestCases = null;
		}

		asm = null;
	}

	///////////////////////////////////////////////
	// DUMMIES
	///////////////////////////////////////////////

	@Override
	public Property addNewProperty(String arg0, String arg1) {
		return null;
	}

	@Override
	public Service addNewService(String arg0, Enum arg1) {
		return null;
	}

	@Override
	public void changePropertyName(String arg0, String arg1) {}

	@Override
	public void moveProperty(String arg0, int arg1) {}

	@Override
	public void removeProperty(Property arg0) {}

	@Override
	public void removeService(Service arg0) {}

	@Override
	public void setName(String arg0) {}

	@Override
	public void setProject(Project arg0) {}

	public void finalize() {
		release();
		Utils.log("Bridge finalized");
	}
}
