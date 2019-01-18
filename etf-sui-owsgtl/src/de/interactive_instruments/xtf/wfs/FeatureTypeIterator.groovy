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
package de.interactive_instruments.xtf.wfs;

import de.interactive_instruments.xtf.ObjectSaver
import de.interactive_instruments.xtf.ProjectHelper
import de.interactive_instruments.xtf.SIterator


class FeatureTypeIterator extends SIterator {

	private def featureTypes;

	/**
	 * C'tor.
	 * @param testStepName properties/test step that will be modified
	 */
	FeatureTypeIterator(def testStepNames=null) {
		reset();
		projectHelper = new ProjectHelper();
		
		def caps = new ObjectSaver().load(
			de.interactive_instruments.xtf.wfs.Capabilities);
		
		featureTypes = caps.getFeatureTypes();
		
		setTestSteps(testStepNames);
	}
	
	/**
	 * Set "typeName" and "namespace" properties of test step
	 */
	public FeatureType setNext() {
		if(pos >= featureTypes.size()) {
			throw new Exception("FeatureTypeIterator: out of bounds");
		}
		String name = featureTypes[pos].getName();
		log.info(this.toString()+" step "+(int)(pos+1)+"/"+featureTypes.size()+
			": Setting parameters for FeatureType "+name);
		
		featureTypes[pos].transferProperties();
		
		/*
		String namespacePost = "xmlns:"+featureTypes[pos].getNamespacePrefix()+"=\""+featureTypes[pos].getNamespace()+"\"";
		String namespaceGet = "xmlns("+featureTypes[pos].getNamespacePrefix()+"="+featureTypes[pos].getNamespace()+")";
		
		if(storeInPropertiesTestStep || testSteps==null) {
				projectHelper.setTransferProperty("namespacePOST", namespacePost);
				projectHelper.setTransferProperty("namespaceGET", namespaceGet);
				projectHelper.setTransferProperty("typeName", featureTypes[pos].getName());
		}else{
			for(testStep in this.testSteps) {
						if(testStep.getTestRequest().getMethod()==HttpMethod.GET) {
							if( testStep.getPropertyValue("namespace")==null || 
								testStep.getPropertyValue("typeName")==null ) {
								throw new Exception("Missing \"namespace\" or \"typeName\" property in teststep "+
									testStep.getLabel() );
							}
							testStep.setPropertyValue("namespace", namespaceGet);
							testStep.setPropertyValue("typeName", featureTypes[pos].getName());
						}else{
							projectHelper.setTransferProperty("namespace", namespacePost);
							projectHelper.setTransferProperty("typeName", featureTypes[pos].getName());
						}
			}
		}
		*/
		
		run=true;
		return featureTypes[pos++];
	}
	
	public boolean hasNext() {
		return ( pos < featureTypes.size() );
	}
	
	public void setIndex(int i) {
		if(i >= featureTypes.size()) {
			throw new Exception("FeatureTypeIterator: Index "+i+" is out of bounds");
		}
		this.pos=i;
	}
}
