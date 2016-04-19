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
package de.interactive_instruments.etf.sel;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;

import com.eviware.soapui.model.ModelItem;
import com.eviware.soapui.model.project.Project;
import com.eviware.soapui.support.Tools;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;

import de.interactive_instruments.IFile;
import de.interactive_instruments.TimeUtils;
import de.interactive_instruments.UriUtils;
import de.interactive_instruments.etf.dal.assembler.AssemblerException;
import de.interactive_instruments.etf.dal.dao.TestReportDao;
import de.interactive_instruments.etf.model.result.transformation.ReportTransformer;
import de.interactive_instruments.etf.model.result.transformation.ReportTransformerException;
import de.interactive_instruments.etf.model.result.transformation.XslReportTransformer;
import de.interactive_instruments.etf.sel.model.impl.result.SuiTestReport;
import de.interactive_instruments.etf.sel.model.mapping.ModelBridge;
import de.interactive_instruments.exceptions.ObjectWithIdNotFoundException;
import de.interactive_instruments.exceptions.StoreException;

/**
 * SoapUI menu action for generating a report
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
public class ReportGeneratorAction extends AbstractSoapUIAction {

	private long xslChangeTimestamp;
	private static final String TRANSFORMER_NAME = "XSLT-DEFAULT";
	private static final String XSLT_FILE_NAME = "Report.xsl";
	private final IFile xsltFile;
	private ReportTransformer transformer;

	public ReportGeneratorAction() {
		super("Generate TestReport", "Generates a ETF test report");
		if (Utils.SEL_STYLING_DIR != null) {
			xsltFile = Utils.SEL_STYLING_DIR.expandPath(XSLT_FILE_NAME);
			xsltFile.setIdentifier("XSL Stylesheet");
		} else {
			xsltFile = null;
		}
		xslChangeTimestamp = 0;
	}

	private void ensureTransformer(final ModelBridge bridge) throws TransformerConfigurationException, IOException {
		if (transformer == null) {
			transformer = new XslReportTransformer(TRANSFORMER_NAME, xsltFile);
			bridge.getReportDao().registerTransformer(transformer);
			xslChangeTimestamp = xsltFile.lastModified();
		} else if (!bridge.getReportDao().isTransformerAvailable(TRANSFORMER_NAME)) {
			// user saved project and reseted the bridge and the source
			bridge.getReportDao().registerTransformer(transformer);
			xslChangeTimestamp = xsltFile.lastModified();
		} else if (xsltFile.lastModified() != xslChangeTimestamp) {
			Utils.log("Detected change in stylesheet, resetting xsl!");
			bridge.getReportDao().registerTransformer(transformer);
			xslChangeTimestamp = xsltFile.lastModified();
		}
	}

	@Override
	public void perform(ModelItem projectModel, Object arg1) {
		try {
			final ModelBridge bridge = ModelBridge.getOrCreateEnvBridge((Project) projectModel);

			final SuiTestReport testReport = bridge.getReport();

			if (testReport == null) {
				if (Utils.SEL_REPORT_DIR == null) {
					throw new TransformerConfigurationException(
							"ETF sel report dir not configured");
				}
				throw new TransformerConfigurationException();
			}

			TestReportDao dao = bridge.getReportDao();
			dao.update(dao.getDtoAssembler().assembleDto(testReport));

			Utils.log("Generating report " + testReport.toString());

			ensureTransformer(bridge);

			long startTime = System.currentTimeMillis();
			final IFile reportFile = new IFile(UriUtils.getParent(
					testReport.getPublicationLocation()).getPath() +
					"Report.html", "HTML-Report");
			reportFile.expectFileIsWritable();

			final FileOutputStream fileStream = new FileOutputStream(reportFile);

			Utils.log("Saving report to " + reportFile.getAbsolutePath() + " ...");
			bridge.getReportDao().transformReportTo(testReport.getId(), TRANSFORMER_NAME, fileStream);
			Utils.log("Report saved. Duration: " + TimeUtils.currentDurationAsMinsSeconds(
					startTime));

			Tools.openURL(reportFile.getAbsolutePath());

		} catch (ObjectWithIdNotFoundException | AssemblerException | StoreException | TransformerConfigurationException | IOException | ReportTransformerException e) {
			Utils.logError(e);
			UISupport.showErrorMessage("An error occurred! See error log for details.");
		}
	}

}
