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
package de.interactive_instruments.etf.sel.assertions;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.StringReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.support.actions.ShowOnlineHelpAction;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.ResponseAssertion;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.jgoodies.forms.builder.ButtonBarBuilder;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.SUtils;

/**
 * A simple Assertion for validating xml responses agains schemas
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */

public class SchemaAssertionImpl extends WsdlMessageAssertion implements SchemaAssertion, ResponseAssertion {
	public class LRUCache<K, V> {

		private static final float HASH_TABLE_LOAD_FACTOR = 0.75f;

		private LinkedHashMap<K, V> map;
		private int cacheSize;

		public LRUCache(int cacheSize) {
			this.cacheSize = cacheSize;
			int hashTableCapacity = (int) Math.ceil(cacheSize / HASH_TABLE_LOAD_FACTOR) + 1;
			map = new LinkedHashMap<K, V>(hashTableCapacity, HASH_TABLE_LOAD_FACTOR, true) {
				private static final long serialVersionUID = 1;

				@Override
				protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
					return size() > LRUCache.this.cacheSize;
				}
			};
		}

		public synchronized V get(K key) {
			return map.get(key);
		}

		public synchronized void put(K key, V value) {
			map.put(key, value);
		}
	}

	private final static Logger LOG = Logger.getLogger(SchemaAssertion.class);
	private static LRUCache<String, Schema> schemaCache = null;
	private String pathToXSD;
	private boolean configureResult;

	private JDialog configurationDialog;
	private JComboBox schemaList;

	public static final String ID = "Simple Schema Validator";
	public static final String LABEL = "Simple Schema Validator";
	public static final String DESCRIPTION = "XSD Validator";

	public static final String[] SCHEMA_SUGGESTIONS = {
			"xsi:schemaLocation",
			"http://schemas.opengis.net/wfs/1.0.0/WFS-basic.xsd",
			"http://schemas.opengis.net/wfs/1.0.0/OGC-exception.xsd",
			"http://schemas.opengis.net/wfs/1.0.0/WFS-capabilities.xsd",
			"http://schemas.opengis.net/wfs/1.0.0/WFS-transaction.xsd",
			"http://schemas.opengis.net/wfs/1.1.0/wfs.xsd",
			"http://schemas.opengis.net/wms/1.3.0/exceptions_1_3_0.xsd",
			"http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd"
	};

	public String getPathToXSD() {
		return pathToXSD;
	}

	public void setPathToXSD(String pathToXSD) {
		this.pathToXSD = pathToXSD;
	}

	public SchemaAssertionImpl(TestAssertionConfig assertionConfig, Assertable modelItem) {
		super(assertionConfig, modelItem, false, true, false, false);
		if (schemaCache == null)
			schemaCache = new LRUCache<>(10);
		XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader(getConfiguration());
		pathToXSD = reader.readString("pathToXSD", "");
	}

	protected String internalAssertRequest(MessageExchange messageExchange, SubmitContext context) throws AssertionException {
		if (!messageExchange.hasRequest(true))
			return "Missing Request";
		else
			return "Validation failed";
	}

	protected String internalAssertResponse(MessageExchange messageExchange, SubmitContext context)
			throws AssertionException {

		class errHandler implements org.xml.sax.ErrorHandler {
			public errHandler() {}

			public void error(SAXParseException e) throws SAXException {
				LOG.error("ERROR in line " + e.getLineNumber() + " column " + e.getColumnNumber() + " : " + e.toString());
				throw new SAXException("Fatal error: " + e.toString());
			}

			public void fatalError(SAXParseException e) throws SAXException {
				LOG.error("FATAL ERROR in line " + e.getLineNumber() + " column " + e.getColumnNumber() + " : " + e.toString());
				throw new SAXException("Fatal error: " + e.toString());
			}

			public void warning(SAXParseException e) throws SAXException {}
		}

		String schemaLocation = pathToXSD;
		try {

			if (SUtils.isNullOrEmpty(messageExchange.getResponseContentAsXml())) {
				throw new IllegalArgumentException("Response is empty");
			}

			if (SUtils.isNullOrEmpty(pathToXSD)) {
				throw new IllegalStateException("No schemaLocation set");
			}

			boolean dtd = messageExchange.getResponseContentAsXml().length() > 9 &&
					messageExchange.getResponseContentAsXml().substring(0, 9).equalsIgnoreCase("<!DOCTYPE");

			// Get Schamelocation and namespace as identifier for the cache if present
			if (schemaLocation.equals("xsi:schemaLocation")) {
				final XmlObject xml = XmlObject.Factory.parse(messageExchange.getResponseContentAsXml());
				XmlObject[] schemaLocFragment = xml
						.selectPath(
								"declare namespace xsi="
										+ "'http://www.w3.org/2001/XMLSchema-instance'"
										+ " /*/@xsi:schemaLocation");
				// Namespace + schemaLocation holen
				if (schemaLocFragment != null && schemaLocFragment[0] != null &&
						schemaLocFragment[0].getDomNode() != null && !SUtils.isNullOrEmpty(schemaLocFragment[0].getDomNode().getNodeValue())) {
					schemaLocation = schemaLocFragment[0].getDomNode().getNodeValue().trim();
				} else {
					throw new IllegalArgumentException("The xsi:schemaLocation attribute is missing in the response");
				}
			}

			// Lookup schema location in cache
			Schema schema = schemaCache.get(schemaLocation);
			if (schema == null) {
				SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
				if (pathToXSD.equals("xsi:schemaLocation")) {
					schema = sf.newSchema();
				} else {
					schema = sf.newSchema(new URL(schemaLocation));
				}
				schemaCache.put(schemaLocation, schema);
			}

			// LOG.info("Validating: "+schemaLocation);

			final SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			final XMLReader reader = spf.newSAXParser().getXMLReader();
			if (!dtd) {
				final ValidatorHandler vh = schema.newValidatorHandler();
				vh.setErrorHandler(new errHandler());
				reader.setContentHandler(vh);
			} else {
				reader.setErrorHandler(new errHandler());
			}
			reader.parse(new InputSource(new StringReader(messageExchange.getResponseContentAsXml())));
		} catch (SAXException e) {
			throw new AssertionException(new AssertionError(e.toString() + " Response did not meet schema \'"
					+ schemaLocation + "\'."));
		} catch (Exception e) {
			throw new AssertionException(new AssertionError(e.toString()));
		}

		return "Response meets schema.";
	}

	@Override
	public boolean configure() {
		if (configurationDialog == null)
			buildConfigurationDialog();

		schemaList.setSelectedItem(pathToXSD);

		UISupport.showDialog(configurationDialog);
		return configureResult;
	}

	protected void buildConfigurationDialog() {
		configurationDialog = new JDialog(UISupport.getMainFrame());
		configurationDialog.setTitle(LABEL);
		configurationDialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent event) {
				SwingUtilities.invokeLater(() -> schemaList.requestFocusInWindow());
			}
		});

		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(UISupport.buildDescription("Specify schema location",
				"\"xsi:schemaLocation\" will use the schema declared in response", null),
				BorderLayout.NORTH);

		// SchemaList
		schemaList = new JComboBox(SCHEMA_SUGGESTIONS);
		schemaList.setSelectedIndex(1);
		schemaList.setEditable(true);
		contentPanel.add(schemaList, BorderLayout.CENTER);

		// OK & Cancel Buttons
		ButtonBarBuilder builder = new ButtonBarBuilder();
		ShowOnlineHelpAction showOnlineHelpAction = new ShowOnlineHelpAction("http://interactive-instruments.de/etf");
		builder.addFixed(UISupport.createToolbarButton(showOnlineHelpAction));
		builder.addGlue();
		JButton okButton = new JButton(new OkAction());
		builder.addFixed(okButton);
		builder.addRelatedGap();
		builder.addFixed(new JButton(new CancelAction()));
		builder.setBorder(BorderFactory.createEmptyBorder(1, 5, 5, 5));
		contentPanel.add(builder.getPanel(), BorderLayout.SOUTH);

		configurationDialog.setContentPane(contentPanel);
		configurationDialog.setSize(400, 200);
		configurationDialog.setModal(true);
		UISupport.initDialogActions(configurationDialog, showOnlineHelpAction, okButton);
	}

	public XmlObject createConfiguration() {
		XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
		builder.add("pathToXSD", pathToXSD);
		return builder.finish();
	}

	public class OkAction extends AbstractAction {
		private static final long serialVersionUID = 1284978646697272311L;

		public OkAction() {
			super("Save");
		}

		public void actionPerformed(ActionEvent arg0) {

			setPathToXSD(((String) schemaList.getSelectedItem()).trim());
			setConfiguration(createConfiguration());
			configureResult = true;
			configurationDialog.setVisible(false);
		}
	}

	public class CancelAction extends AbstractAction {
		private static final long serialVersionUID = -6889601028651382507L;

		public CancelAction() {
			super("Cancel");
		}

		public void actionPerformed(ActionEvent arg0) {
			configureResult = false;
			configurationDialog.setVisible(false);
		}
	}

	@Override
	protected String internalAssertProperty(TestPropertyHolder arg0,
			String arg1, MessageExchange arg2, SubmitContext arg3)
					throws AssertionException {
		// TODO Auto-generated method stub
		return null;
	}
}
