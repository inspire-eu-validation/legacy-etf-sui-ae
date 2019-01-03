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
package de.interactive_instruments.etf.sel.assertions;

import static de.interactive_instruments.etf.sel.assertions.SchemaAssertionImpl.*;

import java.io.StringReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoryMapping;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.ResponseAssertion;
import com.eviware.soapui.plugins.auto.PluginTestAssertion;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormDialogBuilder;
import com.eviware.x.form.XFormFactory;

import org.apache.xmlbeans.XmlObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import de.interactive_instruments.SUtils;
import de.interactive_instruments.etf.sel.Utils;

/**
 * A simple Assertion for validating xml responses agains schemas
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */

@PluginTestAssertion(id = ID, label = LABEL, description = DESCRIPTION, category = AssertionCategoryMapping.VALIDATE_RESPONSE_CONTENT_CATEGORY)
public class SchemaAssertionImpl extends WsdlMessageAssertion implements SchemaAssertion, ResponseAssertion {
	public static final String ID = "Simple Schema Validator";
	public static final String LABEL = "Simple Schema Validator";
	public static final String DESCRIPTION = "XSD Validator";
	private static final String SCHEMA_LOCATION = "pathToXSD";
	private static final String SCHEMA_LOCATION_FIELD = "Schema Location";
	private static final String SCHEMA_LOCATION_REWRITE_FILE = System.getProperty("ets.sel.schemalocation.rewrite.file", null);

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

	private static LRUCache<String, Schema> schemaCache = null;
	private String pathToXSD;

	private XFormDialog configurationDialog;

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
		pathToXSD = reader.readString(SCHEMA_LOCATION, "");
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
				Utils.logError(e,
						"ERROR in line " + e.getLineNumber() + " column " + e.getColumnNumber() + " : " + e.toString());
				throw new SAXException("Fatal error: " + e.toString());
			}

			public void fatalError(SAXParseException e) throws SAXException {
				Utils.logError(e,
						"FATAL ERROR in line " + e.getLineNumber() + " column " + e.getColumnNumber() + " : " + e.toString());
				throw new SAXException("Fatal error: " + e.toString());
			}

			public void warning(SAXParseException e) throws SAXException {

			}
		}

		String schemaLocation = pathToXSD;
		try {

			if (SUtils.isNullOrEmpty(messageExchange.getResponseContentAsXml())) {
				throw new IllegalArgumentException("Response is empty");
			}

			if (SUtils.isNullOrEmpty(pathToXSD)) {
				schemaLocation = "xsi:schemaLocation";
			}

			boolean dtd = messageExchange.getResponseContentAsXml().length() > 9 &&
					messageExchange.getResponseContentAsXml().substring(0, 9).equalsIgnoreCase("<!DOCTYPE");

			// Get Schamelocation and namespace as identifier for the cache if present
			if (schemaLocation.equals("xsi:schemaLocation")) {
				try {
					final XmlObject xml = XmlObject.Factory.parse(messageExchange.getResponseContentAsXml());
					XmlObject[] schemaLocFragment = xml
							.selectPath(
									"declare namespace xsi="
											+ "'http://www.w3.org/2001/XMLSchema-instance'"
											+ " /*/@xsi:schemaLocation");
					// Namespace + schemaLocation holen
					if (schemaLocFragment != null && schemaLocFragment[0] != null &&
							schemaLocFragment[0].getDomNode() != null
							&& !SUtils.isNullOrEmpty(schemaLocFragment[0].getDomNode().getNodeValue())) {
						schemaLocation = schemaLocFragment[0].getDomNode().getNodeValue().trim();
					} else {
						throw new IllegalArgumentException("Missing xsi:schemaLocation attribute in the response");
					}
				} catch (Exception e) {
					throw new IllegalArgumentException("Missing xsi:schemaLocation attribute in response");
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
			throw new AssertionException(new AssertionError(e.toString() + " Response did not validate against schema \'"
					+ schemaLocation + "\'."));
		} catch (Exception e) {
			throw new AssertionException(new AssertionError("Could not validate response: " + e.getMessage()));
		}

		return "Response meets schema.";
	}

	@Override
	public boolean configure() {
		if (configurationDialog == null)
			buildConfigurationDialog();

		StringToStringMap values = new StringToStringMap();
		values.put(SCHEMA_LOCATION_FIELD, pathToXSD);

		values = configurationDialog.show(values);
		if (configurationDialog.getReturnValue() == XFormDialog.OK_OPTION) {
			setPathToXSD(values.get(SCHEMA_LOCATION_FIELD));
		}
		setConfiguration(createConfiguration());
		return true;
	}

	protected void buildConfigurationDialog() {
		XFormDialogBuilder builder = XFormFactory.createDialogBuilder("Schema Validation");
		XForm mainForm = builder.createForm("Basic");

		mainForm.addTextField(SCHEMA_LOCATION_FIELD, "Schema Location", XForm.FieldType.URL).setWidth(40);

		configurationDialog = builder.buildDialog(builder.buildOkCancelActions(),
				"Set a custom schema location or leave empty to use the xsi:schemaLocation attribute found in the response",
				UISupport.OPTIONS_ICON);
	}

	public XmlObject createConfiguration() {
		XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
		builder.add("pathToXSD", pathToXSD);
		return builder.finish();
	}

	@Override
	protected String internalAssertProperty(TestPropertyHolder arg0,
			String arg1, MessageExchange arg2, SubmitContext arg3)
			throws AssertionException {
		// TODO Auto-generated method stub
		return null;
	}
}
