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

import java.util.Objects;

import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.model.testsuite.*;

import de.interactive_instruments.etf.testdriver.TestResultCollector;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */

// @ListenerConfiguration
public class ProjectRunCollector implements ProjectRunListener {

	private TestResultCollector collector;

	// called by SoapUI GUI
	public ProjectRunCollector() {}

	// called by Test Driver
	public ProjectRunCollector(final TestResultCollector collector) {
		this.collector = collector;
	}

	@Override
	public void beforeRun(final ProjectRunner projectRunner, final ProjectRunContext projectRunContext) {
		// init if created by SoapUI GUI
		if (collector == null) {
			if (projectRunner.getProject() instanceof WsdlProject
					&& ((WsdlProject) projectRunner.getProject()).getActiveEnvironment() instanceof CollectorInjectionAdapter) {
				collector = ((CollectorInjectionAdapter) ((WsdlProject) projectRunner.getProject()).getActiveEnvironment())
						.getTestResultCollector();
			} else {
				collector = new DummyCollector();
			}
		}
		collector.startTestTask(projectRunner.getProject().getId());
	}

	@Override
	public void afterRun(final ProjectRunner projectRunner, final ProjectRunContext projectRunContext) {
		Objects.requireNonNull(collector, "Collector not initialized after project run")
				.end(projectRunner.getProject().getId());
	}

	@Override
	public void beforeTestSuite(final ProjectRunner projectRunner, final ProjectRunContext projectRunContext,
			final TestSuite testSuite) {
		Objects.requireNonNull(collector, "Collector not initialized before test suite run").startTestModule(testSuite.getId());
	}

	@Override
	public void afterTestSuite(final ProjectRunner projectRunner, final ProjectRunContext projectRunContext,
			final TestSuiteRunner testSuiteRunner) {
		Objects.requireNonNull(collector, "Collector not initialized after test suite run")
				.end(testSuiteRunner.getTestSuite().getId());
	}
}
