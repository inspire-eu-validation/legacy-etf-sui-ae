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

import com.eviware.soapui.impl.wsdl.teststeps.WsdlGroovyScriptTestStep
import de.interactive_instruments.xtf.Bbox
import de.interactive_instruments.xtf.ProjectHelper
import de.interactive_instruments.xtf.SOAPUI_I
import de.interactive_instruments.xtf.Util

class LoadTestRequestCreator extends SOAPUI_I {

	static String internal_version =  "1.1_20121213";
	
	static int request_id = 0;
	
	ProjectHelper ph;
	Capabilities capabilities;
	
	Bbox maxBBOX;
		
	String targetTestCase;
	
	private int numberOfTries;
	private double lastFactor;
	private Bbox minBBOX;
	private double minFactor;
		
	static LoadTestRequestCreator newInstance() { return new LoadTestRequestCreator(); }
		
	
	LoadTestRequestCreator() {
		ph = new ProjectHelper();
		capabilities = Capabilities.loadInstance();
		targetTestCase="LoadTest";
	}
		
	// Calculate a new BBOX for the expected number of features based on the actual
	// number of features in a BBOX
	private Bbox calcNewBBOX(int expectedFeatures, int featuresInResponse, Bbox bbox, int iteration) {
		if(iteration==0) {
			this.numberOfTries=1;
			this.minFactor=1.2;
			this.lastFactor=1.2;
			// minBBOX=bbox;
		}
		
		log.info("Expecting "+expectedFeatures+" features");
		log.info("Got "+featuresInResponse+" features");
		
		Bbox newBBOX;
		double factor = Math.sqrt(expectedFeatures/featuresInResponse);
		
		if(factor>1) {
			// Enlarge BBOX
			newBBOX = bbox.getScaledBbox(factor, this.maxBBOX);
			this.minFactor=this.lastFactor;
			minBBOX=bbox;
			this.lastFactor=factor;
			this.numberOfTries=1;
		}else{
			// Recover last BBOX and try to enlarge it again
			factor=Math.pow(minFactor,(1/++numberOfTries));
			this.minFactor=factor;
			newBBOX=minBBOX.getScaledBbox(this.minFactor, this.maxBBOX);
			
			if(minBBOX.equals(this.maxBBOX)) {
				throw new Exception("Expected to find "+expectedFeatures+
					 " in the user defined BBOX, but only found "+featuresInResponse);
			}
		}
		
		assert(this.maxBBOX.contains(bbox));
		
		// log.info("Scaling BBOX with factor "+factor);
		
		return newBBOX;
	}
	
	
	
	private createGetFeatureRequestScript(String scr, String testStepName) {
		String script="// Generated with LoadTestRequestCreator\n";
		script+="// version: "+this.internal_version+"\n";
		script+="// on: "+new Date()+"\n";
		script+="// with endpoint : "+testRunner.testCase.testSuite.project.getPropertyValue("serviceEndpoint")+"\n";
		script+="\n";
		script+="\n";
		script+="import de.interactive_instruments.xtf.SOAPUI_I;\n";
		script+="SOAPUI_I.init(log,context,testRunner);\n";
		script+="import de.interactive_instruments.xtf.wfs.GetFeatureRequest;\n";
		script+="import de.interactive_instruments.xtf.Bbox;\n";
		script+="import de.interactive_instruments.xtf.LoadTestSync;\n";
		script+="\n";
		script+="GetFeatureRequest getFeatureRequest = new GetFeatureRequest();\n";
		script+="getFeatureRequest.setDropResponse(true);\n";
		script+="\n";
		script+=scr+"\n";
		script+="\n";
		script+="getFeatureRequest.submit();\n";
		script+="LoadTestSync.TestCaseTearDown(context);\n";
		
		def loadTestCase=ph.getTestCase(targetTestCase,this.testRunner.testCase.testSuite,true);
		
		def newTestStep = loadTestCase.addTestStep("groovy", testStepName);
		newTestStep.setScript(script);
	}
	
	def getProjectPropertyOrDefaultSettings(String propertyName) {
		def settings = ph.getTestStep("Settings");
		
		String projectPropertyValue = Util.getProjectPropertyOrNull(propertyName, testRunner);
		
		if(projectPropertyValue!=null)
		{
			return projectPropertyValue;
		}
		else if(settings!=null && settings.getPropertyValue(propertyName) && 
			!settings.getPropertyValue(propertyName).trim().equals(""))
		{
			return settings.getPropertyValue(propertyName);
		}else{
			return "";
		}
	}
	
	
	void create(String propertyTestStepName="Settings") {
		def settings = ph.getTestStep(propertyTestStepName);
		
		String[] featureTypeNamesList=null;
		
		def proj = testRunner.getTestCase().getTestSuite().getProject();
		
		if( settings.getPropertyValue("featureTypeNamesList") && 
			!settings.getPropertyValue("featureTypeNamesList").trim().equals("") ) {
			featureTypeNamesList = settings.getPropertyValue("featureTypeNamesList").split(',');
		}
		
		def bbox = getProjectPropertyOrDefaultSettings("maxBBOX").split(',');
		Bbox maxBBOX = new Bbox(bbox[0].toDouble(), bbox[1].toDouble(), bbox[2].toDouble(), bbox[3].toDouble());
		
		
		int minFeaturesForTestCreation;
		if( !getProjectPropertyOrDefaultSettings("minFeaturesForTestCreation").equals("") ) {
			minFeaturesForTestCreation = settings.getPropertyValue("minFeaturesForTestCreation").trim().toInteger();
		}
		
		String[] numberOfFeaturesInResponseList = 
			getProjectPropertyOrDefaultSettings("featuresInResponseList").split(',');
		
		String targetTestCase;
		if( settings.getPropertyValue("targetTestCase") 
			&& !settings.getPropertyValue("targetTestCase").trim().equals("") ) {
			this.targetTestCase = settings.getPropertyValue("targetTestCase").trim();
		}
			
		// Delete all test steps in target Testcase
		def loadTestCase=ph.getTestCase(this.targetTestCase);
		for(t in loadTestCase.getTestStepList()) {
			if(t instanceof WsdlGroovyScriptTestStep && t.getName().find("FsInResp")!=-1)
				 loadTestCase.removeTestStep(t);
		}
		
		
		
			int numberOfFeatureTypes=0;
			if(!featureTypeNamesList) {
				numberOfFeatureTypes = capabilities.getFeatureTypes().size();
				log.info("Creating LoadTest requests for all "
					+numberOfFeatureTypes+" feature types taken from the Capabilites.");
			}else{
				numberOfFeatureTypes = featureTypeNamesList.size();
				log.info("Creating LoadTest requests for "+ numberOfFeatureTypes+" feature types.");
			}

			for(i in 0..numberOfFeatureTypes-1)
			{
				FeatureType featureType;
				
				if(featureTypeNamesList==null) {
					featureType = capabilities.getFeatureTypes()[i];
					// featureTypeName = capabilities.getFeatureTypes()[i].getName();
				}else{
					featureType = capabilities.getFeatureTypeByName(featureTypeNamesList[i]);
				}
				
				log.info( i+". Attempt to create LoadTest requests for FeatureType "+featureType.getName());
				ph.setTransferProperty("actFeatureTypeName", featureType.getName());

				int numberOfFeaturesInDB = featureType.getNumberOfFeaturesInDatabase();
				log.info("DB contains "+numberOfFeaturesInDB+" features of FeatureType: "+featureType.getName())

				if(numberOfFeaturesInDB < minFeaturesForTestCreation) {
					log.info("");
					log.warn("Skipping test step creation for \""+featureType.getName()+"\" due to insufficient Features in DB")
					sleep(750); log.info(""); sleep(500); log.info(""); sleep(500);
					continue
				}

			
				Bbox calculatedBBOX;									

				if(featureType.hasPropertyType(PropertyType.GEOMETRY)) {
					
					for(geoProperty in featureType.getPropertiesByType(PropertyType.GEOMETRY)) {
						GetFeatureRequest gf = new GetFeatureRequest();
						gf.setMaxFeatures(1);
						// gf.addFeatureTypeQuery(featureType);
						gf.addFeatureTypeQuery(featureType, geoProperty.getName(), featureType.getBBOX() );
						def getOneFeatureResponse = gf.submit();
						if( getOneFeatureResponse.getDomNode("/*/*")==null ){
							// Empty feature collection
							log.warn("FeatureType "+featureType.getName()+" does not serve GeoProperty "+
								geoProperty.getName() );
							break;
						}
						
						
						this.maxBBOX = maxBBOX;								
						
						Bbox minBBOX = new Bbox(getOneFeatureResponse, "/*:FeatureCollection/*:boundedBy");
						if(minBBOX.getLX()==minBBOX.getUX() || minBBOX.getLY()==minBBOX.getUY()) {
							minBBOX = new Bbox(minBBOX.getLX()-0.008,
								minBBOX.getLY()-0.008,
								minBBOX.getUX()+0.008,
								minBBOX.getUY()+0.008,  minBBOX.getEpsgCode());
						}
						this.maxBBOX.setEpsgCode(minBBOX.getEpsgCode());
						
						if(numberOfFeaturesInResponseList[0].toInteger()>numberOfFeaturesInDB)
						{
							log.info("");
							log.warn("Skipping \""+featureType.getName()+
								"\" test step creation due to insufficient Features in DB")
							sleep(750); log.info(""); sleep(500); log.info(""); sleep(500);
						}else{
							calculatedBBOX = calcNewBBOX(numberOfFeaturesInResponseList[0].toInteger(), 1, minBBOX, 0);
							for(j in 0..numberOfFeaturesInResponseList.size()-1)
							{
								if(numberOfFeaturesInResponseList[j].toInteger()>numberOfFeaturesInDB)
								{
									
									log.info("");
									log.warn("Skipping \""+featureType.getName()+
										"\" test step creation due to insufficient Features in DB")
									sleep(750); log.info(""); sleep(500); log.info(""); sleep(500);
									break;
								}
								findBBOX(featureType, geoProperty, 
									numberOfFeaturesInResponseList[j].toInteger(), calculatedBBOX);
							}
						}
					}
					
				}else{
					for(l in 0..numberOfFeaturesInResponseList.size()-1)
					{
						if(numberOfFeaturesInResponseList[l].toInteger()>numberOfFeaturesInDB)
						{
							log.info("");
							log.warn("Skipping \""+featureType.getName()+
								"\" test step creation due to insufficient Features in DB")
							sleep(750); log.info(""); sleep(500); log.info(""); sleep(500);
							break;
						}

						log.info("")
						log.info("### Creating maxFeatures requests ###")
						log.info("")

						String script="getFeatureRequest.addFeatureTypeQuery(\""+ featureType.getName()+"\", \"xmlns:"+
							featureType.getNamespacePrefix()+"=\\\""+featureType.getNamespace()+"\\\"\");\n";
						script+="getFeatureRequest.setMaxFeatures("+numberOfFeaturesInResponseList[l]+");\n";
						script+="getFeatureRequest.setHandle(\"LoadTest_"+request_id+++"_"+featureType.getName()+"_"+
							numberOfFeaturesInResponseList[l]+"_FeaturesInResponse\");\n";
						createGetFeatureRequestScript( script, featureType.getName()+" "+
							"none"+" "+numberOfFeaturesInResponseList[l].trim()+" FsInResp" );
						
					}
				}
			}
			

			log.info("")
			log.info("")
			log.info("#####  ALL  DONE  #####")
			log.info("")
			log.info("")
			/*
			try{
				testRunner.testCase.setDisabled(true);
				loadTestCase.setDisabled(true);
			}catch(Throwable e){}
			*/
		
		
	}
	
		
	private findBBOX(FeatureType featureType, FeatureTypeProperty geoProperty, 
		int maxNumberOfFeatures, Bbox calculatedBBOX) {
		
		// Try to find matching requests for every geo property
		int k=1;
		final int giveUp=500;
		while(true)
		{
			GetFeatureRequest gf = new GetFeatureRequest();
			gf.setResultType("hits");
			gf.addFeatureTypeQuery(featureType, geoProperty.getName(), calculatedBBOX);
			def getNumberOfFeaturesResponse = gf.submit();
			
			def numberOfFeaturesInResponse =
				getNumberOfFeaturesResponse.getNodeValue("/wfs:FeatureCollection/@numberOfFeatures");
				
			calculatedBBOX = calcNewBBOX(maxNumberOfFeatures,
					numberOfFeaturesInResponse.toInteger(), calculatedBBOX, k);
		
				
			if(numberOfFeaturesInResponse.toInteger()>maxNumberOfFeatures-10
				&& numberOfFeaturesInResponse.toInteger()<maxNumberOfFeatures+10 )
			{
				log.info("Iterations: "+k)
				log.info("")
				log.info("### Creating BBOX request ###")
				log.info("")

				String script="getFeatureRequest.addFeatureTypeQuery(\""+ featureType.getName()+
						"\", \"xmlns:"+featureType.getNamespacePrefix()+"=\\\""+featureType.getNamespace()+"\\\"\""+
						"\n\t, \""+geoProperty.getName()+"\", "+
						"\n\t new Bbox("+calculatedBBOX.getLX()+"," +calculatedBBOX.getLY()+","+
						calculatedBBOX.getUX()+","+calculatedBBOX.getUY()+", "+
						"\n\t "+calculatedBBOX.getEpsgCode()+") );\n";
				script+="getFeatureRequest.setHandle(\"LoadTest_"+request_id+++"_"+featureType.getName()+
					"_"+maxNumberOfFeatures+"_FeaturesInResponse\");\n";
				createGetFeatureRequestScript( script, featureType.getName()+" "+geoProperty.getName()+" "+
					maxNumberOfFeatures+" FsInResp" );
				
				break;
			}

			if(k++==giveUp)
			{
				throw new Exception("Cancelling after "+k+" iterations...")
				log.info("")
				log.warn("!!!Cancelling after "+k+" iterations... !!!")
				log.info("")
				sleep(7000)
				break;
			}
		}
	}
	
};
