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

import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStepWithProperties;

public class SIterator extends SOAPUI_I {

	protected int pos;
	protected long counter=0;
	protected static boolean run;
	protected WsdlTestStepWithProperties[] testSteps;
	protected def projectHelper;


	public void reset() {
		pos=0;
	}
	
	public runTestSteps(def testStepNames=null, boolean abortOnError=true) {
		if(run) {
			if(testStepNames!=null) {
					projectHelper.runTestSteps(testStepNames, abortOnError);
			}else{
				if(testSteps==null) {
					throw new Exception("List of testSteps to run is empty!");
				}
				for(testStep in testSteps) {
					projectHelper.runTestStep(testStep.getName(), abortOnError);
				}
			}
		}else{
			log.info("-- Skipped --");
		}
	}
	
	public generateTestSteps(ProjModel target=null) {
		if(!target) {
			target=new ProjModel();
		}
		if(testSteps==null) {
			throw new Exception("List of testSteps to run is empty!");
		}
		for(testStep in testSteps) {
			target.testStep=++counter+"."+pos+" "+testStep.getName()+" (generated)";
			projectHelper.generateStandaloneHttpRequest(testStep.getName(), target)
				.setDisabled(false);
		}
	}
	
	public setTestSteps(def testStepNames) {
		testSteps=null;
		if(testStepNames) {
			testSteps = new WsdlTestStepWithProperties[testStepNames.size()];
			int i=0;
			for(name in testStepNames) {
				testSteps[i++] = projectHelper.getTestStep(name);
			}
			projectHelper.disableTestSteps(testStepNames);
		}
	}
}
