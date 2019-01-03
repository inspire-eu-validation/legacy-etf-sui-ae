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

import com.eviware.soapui.config.ServiceConfig;
import com.eviware.soapui.model.environment.Environment;
import com.eviware.soapui.model.environment.Property;
import com.eviware.soapui.model.environment.Service;
import com.eviware.soapui.model.project.Project;

import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.etf.testdriver.TestResultCollectorInjector;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public final class CollectorInjectionAdapter implements Environment, TestResultCollectorInjector {

	private Project project;
	private TestResultCollector collector;

	public CollectorInjectionAdapter(final Project project, final TestResultCollector collector) {
		this.project = project;
		this.collector = collector;
	}

	public CollectorInjectionAdapter() {}

	@Override
	public void setProject(final Project project) {
		this.project = project;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public String getName() {
		return getClass().getName() + "." + this.hashCode();
	}

	public TestResultCollector getTestResultCollector() {
		return collector;
	}

	@Override
	public void setTestResultCollector(final TestResultCollector testResultCollector) {
		collector = testResultCollector;
	}

	///////////////////////////////////////////////
	// DUMMIES
	///////////////////////////////////////////////

	@Override
	public Service addNewService(final String s, final ServiceConfig.Type.Enum anEnum) {
		return null;
	}

	@Override
	public void removeService(final Service service) {

	}

	@Override
	public Property addNewProperty(final String s, final String s1) {
		return null;
	}

	@Override
	public void removeProperty(final Property property) {

	}

	@Override
	public void changePropertyName(final String s, final String s1) {

	}

	@Override
	public void moveProperty(final String s, final int i) {

	}

	@Override
	public void setName(final String s) {

	}

	@Override
	public void release() {

	}

}
