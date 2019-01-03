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
package de.interactive_instruments.etf.sel;

import java.util.List;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.TestProperty;
import com.eviware.soapui.model.testsuite.TestRunner;
import com.eviware.soapui.model.testsuite.TestStepResult;
import com.eviware.soapui.support.log.Log4JMonitor;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.interactive_instruments.IFile;
import de.interactive_instruments.II_Constants;
import de.interactive_instruments.SUtils;

/**
 * Utility functions used in the SEL
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 *
 */
final public class Utils {

	private Utils() {

	}

	public static int translateStatus(final TestRunner.Status status) {
		switch (status) {
		case FINISHED:
			// PASSED
			return 0;
		case WARNING:
			// WARNING
			return 5;
		}
		// FAILED
		return 1;
	}

	public static int translateStatus(final boolean manual, final TestStepResult.TestStepStatus status) {
		switch (status) {
		case OK:
			// PASSED
			return 0;
		}
		if (manual) {
			return 7;
		}
		// FAILED
		return 1;
	}

	public static int translateStatus(final boolean manual, final Assertable.AssertionStatus status) {
		switch (status) {
		case VALID:
			// PASSED
			return 0;
		}
		if (manual) {
			return 7;
		}
		// FAILED
		return 1;
	}

	private static Logger log = SoapUI.log;

	public static final String SEL_VERSION = Utils.class.getPackage().getImplementationVersion();

	public final static IFile SEL_DS_DIR = System.getProperty("etf_sel_ds_dir") != null
			? new IFile(System.getProperty("etf_sel_ds_dir")) : null;

	public final static IFile SEL_REPORT_DIR = System.getProperty("etf_sel_report_dir") != null
			? new IFile(System.getProperty("etf_sel_report_dir")) : null;

	public final static IFile SEL_STYLING_DIR = System.getProperty("etf_sel_styling_dir") != null
			? new IFile(System.getProperty("etf_sel_styling_dir")) : null;

	private final static String GROOVY_DIR_ENV_VAR = "ETF_SEL_GROOVY";

	public final static IFile SEL_GROOVY_DIR = initGroovyDir();

	private static IFile initGroovyDir() {
		String groovyDir = System.getenv(GROOVY_DIR_ENV_VAR);
		if (groovyDir == null) {
			Utils.log("Environment variable " + GROOVY_DIR_ENV_VAR + " not set.");
			groovyDir = System.getProperty(GROOVY_DIR_ENV_VAR);
			if (groovyDir == null) {
				Utils.log("Java property variable " + GROOVY_DIR_ENV_VAR + " not set.");
				return null;
			}
		}
		return new IFile(groovyDir);
	}

	public final static boolean DISABLE_REPORTING = SEL_REPORT_DIR == null && !SoapUI.isCommandLine();

	public static String testPropsToString(List<TestProperty> testProperties, boolean newLineSep) {
		String str = "";
		for (TestProperty keyValuePair : testProperties) {
			str += keyValuePair.getName() + ": " +
					keyValuePair.getValue();
			if (newLineSep) {
				str += SUtils.ENDL;
			}
		}
		return str;
	}

	public static void initLogger() {
		final Log4JMonitor logMonitor = SoapUI.getLogMonitor();
		final String selLogName = "de.interactive_instruments.etf.sel";
		if (!SoapUI.isCommandLine() &&
				(logMonitor == null || !logMonitor.hasLogArea(selLogName))) {
			// There is no callback interface in SoapUI (free version)
			// which indicates that the gui is ready, so this really dirty
			// workaround is needed ... but hey it works
			new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(2600);
					} catch (InterruptedException e) {
						return;
					}
					for (int c = 0; c <= 30; c++) {
						final Log4JMonitor logMonitor = SoapUI.getLogMonitor();
						if (logMonitor == null) {
							// Wait for logMonitor
							try {
								Thread.sleep(1700);
								continue;
							} catch (InterruptedException e) {
								break;
							}
						}
						if (!logMonitor.hasLogArea(selLogName)) {
							logMonitor.addLogArea("etf log",
									selLogName,
									false).setLevel(Level.INFO);
							Logger.getLogger(selLogName).info(
									"Activating seperate log area for the ETF SoapUI Extension Library.");
							Logger.getLogger(selLogName).info(
									"Log messages during the initialisation phase were logged into the default SoapUI log.");
							log = Logger.getLogger(selLogName);
							log.info("ETF-SEL" + SEL_VERSION + " " +
									II_Constants.II_COPYRIGHT);
							break;
						}
					}
				}
			}.start();
		}
	}

	public static void log(String info) {
		log.log(Level.INFO, info);
	}

	public static void logError(Throwable e, String info) {
		// log.log("Error "+info+" : "+e.getMessage());
		log.error(info, e);
	}

	public static void logError(Throwable e) {
		// log.log("Error: "+e.getMessage());
		log.error(e);
	}

	public static void warn(String warning) {
		log.log(Level.WARN, warning);
	}
}
