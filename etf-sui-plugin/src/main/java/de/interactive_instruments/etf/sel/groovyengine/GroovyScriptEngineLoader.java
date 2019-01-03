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
package de.interactive_instruments.etf.sel.groovyengine;

import com.eviware.soapui.config.TestCaseConfig;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.WsdlTestSuite;
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase;
import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.environment.Environment;
import com.eviware.soapui.model.iface.Interface;
import com.eviware.soapui.model.mock.MockService;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.model.project.ProjectListener;
import com.eviware.soapui.model.testsuite.TestCase;
import com.eviware.soapui.model.testsuite.TestSuite;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;

import org.apache.xmlbeans.XmlObject;
import org.mortbay.log.Log;

/**
 * The class adds a button in the SoapUI Project menu to recompile the Groovy scripts.
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 *
 */
public class GroovyScriptEngineLoader extends AbstractSoapUIAction implements ProjectListener {

	public GroovyScriptEngineLoader() {
		super("Rebuild scripts", "Rebuilds the external groovy script classes");
	}

	/**
	 * Initializes the ScriptEngine by injecting the ReloadableSoapUIExtensionClassLoader
	 */
	@Override
	public void afterLoad(Project project) {
		GroovyScriptEngine.getInstance();
		reloadTestCases(project);
	}

	@Override
	public void perform(ModelItem target, Object param) {
		try {
			GroovyScriptEngine.getInstance().compile();
			reloadTestCases((Project) target);
		} catch (Exception e) {
			UISupport.showErrorMessage(e.getMessage());
		}
	}

	/**
	 * Reload the Testcases by copying them
	 * @param project
	 */
	private void reloadTestCases(Project project) {
		if (GroovyScriptEngine.getInstance().isInitialized()) {
			Log.debug("Reloading all TestCases for using the exchanged ClassLoader");
			for (TestSuite ts : ((WsdlProject) project).getTestSuiteList()) {
				final WsdlTestSuite wts = (WsdlTestSuite) ts;
				for (TestCase tc : wts.getTestCaseList()) {
					final WsdlTestCase wtc = (WsdlTestCase) tc;
					XmlObject config = wtc.getConfig().copy();
					wts.replace(wtc, ((TestCaseConfig) config));
				}
			}
		}
	}

	@Override
	public void beforeSave(Project project) {}

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

	@Override
	public void testSuiteAdded(TestSuite arg0) {}

	@Override
	public void testSuiteMoved(TestSuite arg0, int arg1, int arg2) {}

	@Override
	public void testSuiteRemoved(TestSuite arg0) {}
}
