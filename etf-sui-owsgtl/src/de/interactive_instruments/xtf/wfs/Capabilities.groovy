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
package de.interactive_instruments.xtf.wfs

import com.eviware.soapui.support.XmlHolder
import de.interactive_instruments.xtf.*
import de.interactive_instruments.xtf.exceptions.FatalInternalException
import de.interactive_instruments.xtf.exceptions.RequiredDomNodeNotFoundException

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlTransient

@XmlAccessorType( XmlAccessType.FIELD )
@XmlRootElement
public class Capabilities {

	private ArrayList<FeatureType> featureTypes = new ArrayList<FeatureType>();
	
	private int numberOfFeatureTypes;
	
	// Operation - outputFormat
	private TreeMap<String, OutputFormat[]> outputFormats = new TreeMap<String, OutputFormat[]>();
	
	// AcceptVersions
	private String[] versions;
	
	@XmlTransient
	private List<PropertySchemaAnalyzer> schemaAnalyzers = new ArrayList<PropertySchemaAnalyzer>();
	
	@XmlTransient
	private ProjectHelper ph;
	
	@XmlTransient
	private def log;
	
	private String serviceTypeVersion;
	private String getServiceTypeVersion() { return serviceTypeVersion; }
	
	public static Capabilities loadInstance() { 
		return ObjectSaver.newInstance().load(
			de.interactive_instruments.xtf.wfs.Capabilities);
	}
	
	private OutputFormat[] getOutputFormatsForOperation(final String operation) {
		return outputFormats.get(operation);
	}
		
	/**
	 * Extract capabilities and create objects
	 */
	public void setCapabilities(XmlHolder capabilitiesXML) {	
		this.ph = new ProjectHelper();
		this.log = SOAPUI_I.getInstance().getLog();
		this.serviceTypeVersion = capabilitiesXML.getNodeValue("/*:WFS_Capabilities/@version");	
		ph.setTransferProperty("version", this.serviceTypeVersion);
		initOutputFormats(capabilitiesXML);
		this.versions = getOperationParameterValues(capabilitiesXML, "GetCapabilities", "AcceptVersions");
		initFeatureTypes(capabilitiesXML);
	}
		
	public def getFeatureTypes() {
		if(this.featureTypes==null || this.featureTypes.size()==0) {
			throw new FatalInternalException(this, "No FeatureTypes available");
		}
		return this.featureTypes;
	}

	public def getFirstFeatureType() {
		return getFeatureTypes().get(0);
	}
	

	public def getFeatureTypeByName(final String name) {
		for(featureType in this.featureTypes) {
			if(featureType.getPrefixAndName() == name)
				return featureType;
		}
		throw new FatalInternalException(this, 
			"FeatureType "+name+" is unknown!");
	}

		
	public boolean isVersionSupported(final String ver) {
		for(version in this.versions) { 
			if (version.equals(ver)) {
				return true;
			}
		}
		return true;
	}
	
	/* 
	 * Parse OutputFormats for Operations
	 */
	private void initOutputFormats(final XmlHolder capabilitiesXML) {
			
		final String operationNameXPath=
			"/*:WFS_Capabilities/*:OperationsMetadata/*:Operation/@name";
		
		for(operationName in capabilitiesXML.getNodeValues(operationNameXPath) ) {
				
				try {
					final List<String> allOutputFormats = 
						getOperationParameterValues(capabilitiesXML, 
							operationName, "outputFormat");
					
					final List<OutputFormat> ofList = new ArrayList<OutputFormat>();
					for(String o in allOutputFormats.sort()) {
						def blacklistedOutputFormats = Util.getProjectPropertyOrNull("blacklisted.outputFormats")
						if(blacklistedOutputFormats==null || !blacklistedOutputFormats.contains(o)) {
							ofList.add(new OutputFormat(o));
						}
					}

					this.outputFormats.put(operationName, ofList.toArray(new OutputFormat[0]));
				}catch(RequiredDomNodeNotFoundException e){
					//Operation does not Provide an outputFormat
				}
		}
	}
	
	private int requestNumberOfFeatures(final def featureType) {
		
		ph.setTransferProperty("featureTypeName", featureType.getName());
		ph.setTransferProperty("featureTypeNamespaceWfs2", "xmlns("+featureType.getPrefix()+","+featureType.getNamespaceURI()+")");

		def respDN = ph.runTestStep("Get number of Features in DB").getDomNode("/");
		
		def numberOfFeaturesAttribute=null;
		
		if(respDN!=null) {
			if(respDN.getFirstChild()!=null) {
				numberOfFeaturesAttribute = respDN.getFirstChild().getAttributes().getNamedItem("numberOfFeatures");
				
				if(numberOfFeaturesAttribute==null) {
					// WFS 2.0
					numberOfFeaturesAttribute = respDN.getFirstChild().getAttributes().getNamedItem("numberMatched");
				}
			}
		}
		if(numberOfFeaturesAttribute==null) {
			throw new RequiredDomNodeNotFoundException(this, "Attribute \"numberOfFeatures\" not found in reponse!" );
		}
		
		return numberOfFeaturesAttribute.getValue().toInteger();
	}
	
	private String[] getOperationParameterValues(final XmlHolder capabilitiesXML, 
		final String operation, 
		final String parameter)
	{
		final String operationXPath=
			"/*:WFS_Capabilities/*:OperationsMetadata/*:Operation[@name=\""+
			operation+"\"]";
		
		String allowedValue="";
		if(this.serviceTypeVersion=="2.0.0")
			allowedValue = "/*:AllowedValues";
		final String owsValueXPath="/*:Parameter[@name=\""+parameter+"\"]"+
			allowedValue+"/*:Value";

		String[] values = capabilitiesXML.getNodeValues(operationXPath+owsValueXPath);
		
		if(values.size()==0) {
			final String paramXpath=
					"/*:WFS_Capabilities/*:OperationsMetadata";
			values = capabilitiesXML.getNodeValues(paramXpath+owsValueXPath);
			if(values.size()==0) {
				throw new RequiredDomNodeNotFoundException(this, "Unable to find \"" + operation + "\"");
			}
		}
		return values;
	}
	
	private void initFeatureTypes(final XmlHolder capabilitiesXML) {
		String featureTypeXPath = "/*:WFS_Capabilities/*:FeatureTypeList/*:FeatureType";
		this.numberOfFeatureTypes = capabilitiesXML.getNodeValue(
			"count("+featureTypeXPath+")").toInteger();
		if(this.numberOfFeatureTypes==0) {
			throw new RequiredDomNodeNotFoundException(this, 
				"Unable to find FeatureType definition in Capabilities Document");
		}
		
		// Select all or only random selected FeatureTypes
		if( !TestSetup.isTestIntensive() ) {
			log.info("Selecting random FeatureTypes for every outputFormat");
		}
		
		
		// Init PropertySchemaAnalyzer for every OutputFormat
		log.info("Initializing schema analyzer with outputFormats:");
		for(OutputFormat of in getOutputFormatsForOperation("DescribeFeatureType") ) {
			// Request DescribeFeatureType and analyze the output with the SchemaAnalyzer
			log.info(" - \""+of.getFormat()+"\"");
			getAndAddSchema(of);
		}
			
		// Init (random selected) FeatureTypes
		def range = 1..this.numberOfFeatureTypes;
		def featureTypePosList = Util.genRandomTestListOnIntesiveTests(range);
		log.info("Initializing FeatureTypes");
		for(pos in featureTypePosList) {
			FeatureType ft = initFeatureType(capabilitiesXML, featureTypeXPath, pos);
			this.featureTypes.add(ft);
		}
			
		log.info(this.featureTypes.size()+" FeatureTypes configured for testing");
		if(TestSetup.isTestIntensive() &&
			this.numberOfFeatureTypes!=this.featureTypes.size())
		{
			throw new FatalInternalException(this, "Found "+this.numberOfFeatureTypes+
				" FeatureTypes in Capabilities but only configured "+this.featureTypes.size()+
				" for testing!");
		}
	}
	
	private void getAndAddSchema(OutputFormat outputFormat) {
		ph.setTransferProperty("outputFormat", outputFormat.getFormat());
		final String schemaXml = ph.runTestStep("Get Schema Definition").getXml();
		schemaAnalyzers.add(new PropertySchemaAnalyzer(schemaXml, outputFormat));
	}
	
	/**
	 * Configure a FeatureType from a Capabilites Document structure
	 * Returns null if the requested OutputFormat is not supported by the FeatureType
	 **/
	private FeatureType initFeatureType(final XmlHolder capabilitiesXML, 
		String featureTypeXPath,
		int i)
	{		
		String ftXpath = featureTypeXPath+"["+i+"]";
		
		String name = capabilitiesXML.getNodeValue(ftXpath+"/*:Name");
		log.info("Configuring FeatureType \""+name+"\"");
		
		String localPart = name.substring(name.indexOf(':')+1, name.length());
		String prefix = name.substring(0, name.indexOf(':'));
		String namespaceURI = capabilitiesXML.getNodeValue("namespace-uri-for-prefix("
			+"'"+prefix+"'"
			+", "+ftXpath+"/*:Name)");
		
		FeatureType featureType = new FeatureType(namespaceURI, localPart, prefix);
		final int noOfFeatureTypes = requestNumberOfFeatures(featureType);
		
				
		// Check which output formats are supported by the FeatureType
		List<PropertySchemaAnalyzer> schAnalyzers = this.schemaAnalyzers;
		if( capabilitiesXML.getDomNode(ftXpath+"/*:OutputFormats") )
		{
			schAnalyzers=new ArrayList<PropertySchemaAnalyzer>();
			for(String of in capabilitiesXML.getNodeValues(ftXpath+"/*:OutputFormats/*:Format")) {
				OutputFormat oFormat = new OutputFormat(of); 
				boolean found=false;
				for( a in this.schemaAnalyzers) {
					if(a.getOutputFormat() == oFormat) {
						schAnalyzers.add(a);
						found=true;
						break;
					}
				}
				if(!found) {
					log.info("Initializing an additional SchemaAnalyzer for FeatureType "+
						featureType.getPrefixAndName());
					getAndAddSchema(oFormat);
				}
			}
		}
		
		String defaultSRS = capabilitiesXML.getNodeValue(ftXpath+"/*:DefaultSRS");
		String[] ortherSRS = capabilitiesXML.getNodeValues(ftXpath+"/*:OtherSRS");
		if(defaultSRS==null || defaultSRS == "") {
			defaultSRS = capabilitiesXML.getNodeValue(ftXpath+"/*:DefaultCRS");
			ortherSRS = capabilitiesXML.getNodeValues(ftXpath+"/*:OtherSRS");
		}
		featureType.setDefaultSRS(defaultSRS);
		featureType.setOtherSRS(ortherSRS);
				
		featureType.setBBOX(new Bbox(capabilitiesXML, ftXpath+"/*:WGS84BoundingBox"));
		featureType.setNumberOfFeaturesInDatabase(noOfFeatureTypes);
		featureType.analyzeAndSetProperties(schAnalyzers);
		
		return featureType;
	}
};
