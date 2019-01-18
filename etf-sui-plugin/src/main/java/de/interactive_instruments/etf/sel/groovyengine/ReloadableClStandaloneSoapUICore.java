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

import java.io.File;
import java.net.URL;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.SoapUICore;
import com.eviware.soapui.SoapUIExtensionClassLoader;
import com.eviware.soapui.StandaloneSoapUICore;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.monitor.MockEngine;
import com.eviware.soapui.security.registry.SecurityScanRegistry;
import com.eviware.soapui.support.action.SoapUIActionRegistry;
import com.eviware.soapui.support.factory.SoapUIFactoryRegistry;
import com.eviware.soapui.support.listener.SoapUIListenerRegistry;

/**
 * This core will be injected as root core and acts as a proxy for
 * a StandaloneSoapUICore (used in the GUI version).
 * It exchanges the SoapUIExtensionClassLoader.
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class ReloadableClStandaloneSoapUICore extends StandaloneSoapUICore {

	private StandaloneSoapUICore exchangedCore;
	private ReloadableSoapUIExtensionClassLoader exchangedExtClassLoader;
	private final URL[] extLibs = SoapUI.getSoapUICore().getExtensionClassLoader().getURLs();

	public ReloadableClStandaloneSoapUICore(SoapUICore soapUICore) {
		super(false);
		exchangedExtClassLoader = null;
		if (soapUICore instanceof StandaloneSoapUICore) {
			exchangedCore = (StandaloneSoapUICore) soapUICore;
		}
		// This might be the wrong GUI core:
		// exchangedCore=(DefaultSoapUICore) soapUICore;
		init();
	}

	public void reset() {
		exchangedExtClassLoader.reset();
	}

	private void init() {
		final SoapUIExtensionClassLoader parentCl = exchangedCore.getExtensionClassLoader();
		exchangedExtClassLoader = new ReloadableSoapUIExtensionClassLoader(
				parentCl.getURLs(),
				parentCl.getParent());
		System.out.println("Injecting ClassLoader " +
				exchangedExtClassLoader + " -> " + parentCl);
	}

	@Override
	public SoapUIExtensionClassLoader getExtensionClassLoader() {
		return exchangedExtClassLoader;
	}

	@Override
	public synchronized void loadExternalLibraries() {
		exchangedCore.loadExternalLibraries();
	}

	public URL[] getExtLibUrls() {
		return this.extLibs;
	}

	@Override
	public boolean getInitialImport() {
		return exchangedCore.getInitialImport();
	}

	@Override
	public void setInitialImport(boolean initialImport) {
		exchangedCore.setInitialImport(initialImport);
	}

	@Override
	public String getRoot() {
		return exchangedCore.getRoot();
	}

	@Override
	public Settings importSettings(File file) throws Exception {
		return exchangedCore.importSettings(file);
	}

	@Override
	public Settings getSettings() {
		return exchangedCore.getSettings();
	}

	@Override
	public String saveSettings() throws Exception {
		return exchangedCore.saveSettings();
	}

	@Override
	public String getSettingsFile() {
		return exchangedCore.getSettingsFile();
	}

	@Override
	public void setSettingsFile(String settingsFile) {
		exchangedCore.setSettingsFile(settingsFile);
	}

	@Override
	public MockEngine getMockEngine() {
		return exchangedCore.getMockEngine();
	}

	@Override
	public SoapUIListenerRegistry getListenerRegistry() {
		return exchangedCore.getListenerRegistry();
	}

	@Override
	public SoapUIActionRegistry getActionRegistry() {
		return exchangedCore.getActionRegistry();
	}

	@Override
	public SoapUIFactoryRegistry getFactoryRegistry() {
		return exchangedCore.getFactoryRegistry();
	}

	@Override
	public SecurityScanRegistry getSecurityScanRegistry() {
		return exchangedCore.getSecurityScanRegistry();
	}

}
