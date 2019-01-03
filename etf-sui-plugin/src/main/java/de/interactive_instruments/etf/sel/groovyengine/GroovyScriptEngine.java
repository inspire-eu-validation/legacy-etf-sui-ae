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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import com.eviware.soapui.DefaultSoapUICore;
import com.eviware.soapui.SoapUI;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.tools.GroovyClass;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.sel.Utils;

/**
 * A Groovy script engine which (re-)compiles Groovy classes
 * from a source directory to an temporary jar and calls the
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
final public class GroovyScriptEngine {

	private IFile groovyScriptSourcesDir;
	private final CompilerConfiguration config = new CompilerConfiguration();
	private IFile tmpFile;
	private boolean initialized;
	private ReloadableClStandaloneSoapUICore exchangedCore;

	private static GroovyScriptEngine instance = new GroovyScriptEngine();

	private GroovyScriptEngine() {

		initialized = false;
		groovyScriptSourcesDir = Utils.SEL_GROOVY_DIR;
		if (groovyScriptSourcesDir == null) {
			Utils.log("Deactivating Compilation Unit.");
			return;
		}

		Utils.log("Searching directory " + groovyScriptSourcesDir.getPath() + " for groovy source files");
		if (groovyScriptSourcesDir != null) {
			config.setDebug(true);
			config.setVerbose(true);

			try {
				groovyScriptSourcesDir.expectDirIsReadable();

				final List<String> classPaths = new ArrayList<String>();
				classPaths.add(groovyScriptSourcesDir.getPath());

				if (!(SoapUI.getSoapUICore() instanceof DefaultSoapUICore)) {
					final ReloadableClStandaloneSoapUICore newCore = new ReloadableClStandaloneSoapUICore(
							SoapUI.getSoapUICore());
					exchangedCore = newCore;
					SoapUI.setSoapUICore(exchangedCore);
					for (URL url : newCore.getExtLibUrls()) {
						classPaths.add(url.getPath());
					}
				}

				config.setClasspathList(classPaths);

				initialized = true;
				tmpFile = null;
				Utils.log("Starting initial compiling of groovy source files");
				compile();
			} catch (Exception e) {
				initialized = false;
				Utils.log("Compilation Unit failed to initialize");
				Utils.logError(e);
				e.printStackTrace();
				return;
			}
			Utils.log("Compilation Unit successfully initialized");
		}
	}

	public boolean isInitialized() {
		return initialized;
	}

	public static GroovyScriptEngine getInstance() {
		return instance;
	}

	public void compile() throws Exception {
		if (!initialized) {
			Utils.log("Compilation call skipped due to " +
					"uninitialized Compilation unit!");
			return;
		}

		CompilationUnit compUnit = new CompilationUnit(config);

		groovyScriptSourcesDir.expectDirIsReadable();

		List<? extends IFile> sourceFiles = groovyScriptSourcesDir.getFilesInDirRecursiveByRegex(
				IFile.getRegexForExtension("groovy"), 15, false);

		if (sourceFiles != null) {
			final File[] files = (File[]) sourceFiles.toArray(new File[0]);
			compUnit.addSources(files);
		} else {
			Utils.log("Compilation skipped: No source files found!");
			return;
		}

		if (tmpFile != null) {
			tmpFile.delete();
		}
		tmpFile = IFile.createTempDir("etf_sel_groovy");
		tmpFile.setIdentifier("temporary compilation");
		tmpFile.deleteOnExit();
		config.setTargetDirectory(tmpFile.getPath());

		try {
			compUnit.compile();
		} catch (CompilationFailedException e) {
			Utils.log("Compilation failed!");
			Utils.logError(e);
			return;
		}

		for (Object o : compUnit.getClasses()) {
			Utils.log("Compiled class: " + ((GroovyClass) o).getName());
		}

		boolean coreNeedsReset = !(SoapUI.getSoapUICore() instanceof DefaultSoapUICore);

		if (coreNeedsReset) {
			if (!(SoapUI.getSoapUICore() instanceof ReloadableClStandaloneSoapUICore)) {
				throw new Exception("Modified SoapUI Core not accessible");
			}
			((ReloadableClStandaloneSoapUICore) SoapUI.getSoapUICore()).reset();
		}

		SoapUI.getSoapUICore().getExtensionClassLoader().addFile(tmpFile);

		if (coreNeedsReset) {
			// Selftest
			boolean found = false;
			for (URL url : SoapUI.getSoapUICore().getExtensionClassLoader().getURLs()) {
				if (url.getPath().equals(tmpFile.toURI().toURL().getPath())) {
					found = true;
					break;
				}
			}
			Utils.log("Lib " + tmpFile + " to ClassLoader " +
					SoapUI.getSoapUICore().getExtensionClassLoader() +
					" added. Selftest OK: " + found);
		}
	}

	@SuppressWarnings("unused")
	private void printClassPaths(final ClassLoader loader) {
		Utils.log(loader.toString());
		if (loader instanceof URLClassLoader) {
			for (URL url : ((URLClassLoader) loader).getURLs()) {
				Utils.log(" -" + url);
			}
		}
		if (loader.getParent() != null) {
			printClassPaths(loader.getParent());
		}
	}

}
