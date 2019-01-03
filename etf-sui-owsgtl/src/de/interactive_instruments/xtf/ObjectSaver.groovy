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
package de.interactive_instruments.xtf

import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep
import de.interactive_instruments.xtf.exceptions.FatalInternalException
import de.interactive_instruments.xtf.exceptions.NullObjectLoadingException

import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller

/**
 * Used to persist Java objects as XML in the test project.
 */
class ObjectSaver extends SOAPUI_I {

	private WsdlTestStep storeTestStep;
		
	public ObjectSaver(boolean useTransientStore=true) {
		ProjectHelper h = new ProjectHelper();
		def testCase = h.getTestCase("ObjectSaver",h.getTestSuite("Initialization and basic checks", true), true);
		if(useTransientStore) {
			this.storeTestStep = h.getTestStep("TransientObjects", testCase, "properties");
			this.storeTestStep.setDiscardValuesOnSave(true);
		}else{
			this.storeTestStep = h.getTestStep("PersistentObjects", testCase, "properties");
			this.storeTestStep.setDiscardValuesOnSave(false);
		}
		final String desc = 
			"This test step stores objects that can be accessed via the de.interactive_instruments.ObjectSaver class."
		this.storeTestStep.setDescription(desc);
	}
	
	public void save(Object object, String objectName=null) {
		if(!object)
			throw new FatalInternalException(this, "attempt to save null object!");
		if(object.getClass()==java.lang.String) {
			if(objectName==null || objectName.equals("")) {
				throw new FatalInternalException(this, 
					"attempt to save String value without a name!");
			}
			storeTestStep.setPropertyValue(objectName, object);
			return;
		}
		if(object.getClass()==java.lang.Class) {
			throw new FatalInternalException(this, 
					"useless attempt to save the java.lang.class object!");
		}
		
		java.io.StringWriter sw = new StringWriter();
		JAXBContext context = JAXBContext.newInstance( object.getClass() ); 
		Marshaller m = context.createMarshaller(); 	
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
		// Using the file.encoding system property will not work here
		if(System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
			m.setProperty(Marshaller.JAXB_ENCODING, "ISO-8859-1");
		}else{
			m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		}
		m.marshal(object, sw);
		if(!objectName || objectName.equals("")) {
			objectName=object.getClass().getName();
		}
		storeTestStep.setPropertyValue(objectName, sw.toString());
	}
	
	public <T> T load(Class clasz, String objectName=null) {
		if(!this.storeTestStep)
			throw new FatalInternalException(this, "empty properties testStep!");
		if(!clasz)
			throw new FatalInternalException(this, 
				"unable to load object due to empty class parameter!");
		if(clasz==java.lang.String) {
			if(objectName==null || objectName.equals("")) {
				throw new FatalInternalException(this,
					"attempt to load a String value without a name!");
			}
			return storeTestStep.getPropertyValue(objectName);
		}
		
		JAXBContext context = JAXBContext.newInstance(clasz); 
		Unmarshaller um = context.createUnmarshaller();
		def name=objectName;
		if(!objectName || objectName=="")
			name=clasz.getName();
		String value = storeTestStep.getPropertyValue(name);
		if(!value) {

			throw new NullObjectLoadingException(this,
					"attempt to load null object! "
							+"Requested object of type " + clasz + " with name " + name+". "
							+"This error might be a subsequent error due to failures in the 'Initialization and basic checks' test case!");
		}
		ByteArrayInputStream xmlBytes = new ByteArrayInputStream(
			value.getBytes());	
		return (T) um.unmarshal(xmlBytes);
	}
	
	public boolean isObjectStored(Class clasz, String objectName=null) {
		if(!this.storeTestStep)
			throw new FatalInternalException(this, "empty properties testStep!");
		if(!clasz)
			throw new FatalInternalException(this, 
				"class not specified!");
		if(clasz==java.lang.String) {
			if(objectName==null || objectName.equals("")) {
				throw new FatalInternalException(this,
					"String value not specified!");
			}
		}
		def name=objectName;
		if(!objectName || objectName=="") {
			name=clasz.getName();
		}
		String value = storeTestStep.getPropertyValue(name);
		if(!value) {
			return false;
		}
		return true;
	}
	
	public def getStoreTestStep() {
		return this.storeTestStep;
	}
 }
