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

import com.eviware.soapui.model.environment.Environment;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.mock.MockService;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.project.ProjectListener;
import com.eviware.soapui.model.testsuite.TestSuite;

import de.interactive_instruments.etf.sel.Utils;

/**
 * The object initializes the result connection mechanisms through
 * the project listener, after the project has been loaded.
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
public class ModelMapperPL implements ProjectListener {

	public ModelMapperPL() {}

	/**
	 * Initialize
	 */
	@Override
	public void afterLoad(final Project project) {
		if (Utils.DISABLE_REPORTING) {
			project.removeProjectListener(this);
		} else {
			Utils.initLogger();
			ModelBridge.getOrCreateEnvBridge(project);
		}
	}

	/**
	 * Resets the bridge and a mapped model
	 */
	@Override
	public void beforeSave(final Project project) {
		// Just delete the model bridge
		Utils.log("Deleting model bridge.");
		ModelBridge.resetModelBridge(project);
	}

	///////////////////////////////////////////////
	// DUMMIES
	///////////////////////////////////////////////

	@Override
	public void testSuiteAdded(TestSuite arg0) {}

	@Override
	public void testSuiteMoved(TestSuite arg0, int arg1, int arg2) {}

	@Override
	public void testSuiteRemoved(TestSuite arg0) {}

	@Override
	public void environmentAdded(Environment arg0) {}

	@Override
	public void environmentRemoved(Environment arg0, int arg1) {}

	@Override
	public void environmentRenamed(Environment arg0, String arg1, String arg2) {}

	@Override
	public void environmentSwitched(Environment arg0) {}

	@Override
	public void interfaceAdded(Interface arg0) {}

	@Override
	public void interfaceRemoved(Interface arg0) {}

	@Override
	public void interfaceUpdated(Interface arg0) {}

	@Override
	public void mockServiceAdded(MockService arg0) {}

	@Override
	public void mockServiceRemoved(MockService arg0) {}
}
