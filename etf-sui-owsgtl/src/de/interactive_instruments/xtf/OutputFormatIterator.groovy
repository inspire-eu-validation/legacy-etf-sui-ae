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
package de.interactive_instruments.xtf;

import com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod;

@Deprecated
class OutputFormatIterator extends SIterator {

	private def ignoredOutputFormats;
	private String[] outputFormats;
	private def testStep;
	private def projectHelper;
	
	OutputFormatIterator(def testStepNames) {
		pos=0;
		projectHelper = new ProjectHelper();
		ObjectSaver os = new ObjectSaver();
		outputFormats = os.load(
			de.interactive_instruments.xtf.Capabilities).getOutputFormats();
		setTestSteps(testStepNames);
	}
	
	private boolean isOutputFormatAllowed(String format) {
		if(ignoredOutputFormats==null)
			return true;
		for(ignoredFormat in ignoredOutputFormats) { 
			if(ignoredFormat.equals(format))
				return false;
		}
		return true;
	}

	/**
	 * Set namespace fragments the OutputFormatIterator shall use
	 */
	public void setIgnoredOutputFormats(def formats) {
		this.ignoredOutputFormats=formats;
	}

	/**
	 * Set "outputFormat" property of test step
	 */
	public void setNext(boolean storeInPropertiesTestStep=false) {
		if(pos >= outputFormats.size()) {
			throw new Exception("OutputFormatIterator: out of bounds");
		}
		String name = outputFormats[pos];
		if(isOutputFormatAllowed(name)) {
			log.info(this.toString()+" step "+(int)(pos+1)+"/"+outputFormats.size()+
				": Setting parameters for FeatureType "+name);
			
			if(storeInPropertiesTestStep || testSteps==null) {
				testStep = projectHelper.setTransferProperty("outputFormat", outputFormats[pos]);
			}else{
				for(testStep in this.testSteps) {			
					if( testStep.getTestRequest().getMethod()==HttpMethod.POST && (
						testStep.getPropertyValue("namespace")==null || 
						testStep.getPropertyValue("typeName")==null )) {
						throw new Exception("Missing \"namespace\" or \"typeName\" property in teststep "+
							testStep.getLabel() );
					}
					testStep.setPropertyValue("outputFormat", outputFormats[pos]);
				}
			}
			run=true;
		}else{
			log.info(++pos+". Skipping OutputFormat "+name);
			setNext(storeInPropertiesTestStep);
			return;
		}
		pos++;
	}
	
	public boolean hasNext() {
		return (pos < outputFormats.size());
	}
}
