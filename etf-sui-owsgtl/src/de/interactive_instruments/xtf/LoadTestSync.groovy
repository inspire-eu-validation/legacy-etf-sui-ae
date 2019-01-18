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

/**
 * Force threads to run a test step (in parallel!) until they
 * can advance to the next one.
 */
class LoadTestSync {

	// Call from Setup script of the LoadTest
	public static LoadTestSetup(def context) {
		context.monitor = new java.util.concurrent.atomic.AtomicInteger(0);
	}
	
	// Put in TearDown script of LoadTest!
	public static LoadTestTearDown(def context) {
		try{
			synchronized(context) {
			def monitor = context.monitor
			context.monitor = null
			monitor.notifyAll()
			}
		}catch(Throwable e) { }
	}
	
	// Put at the end of the test step or in TearDown script
	public static TestCaseTearDown(def context) {
		if(context?.LoadTestContext?.monitor != null) {
		   def monitor = context.LoadTestContext.monitor
		   def numWaiting = monitor.incrementAndGet()

		   synchronized(monitor) {
			  if(numWaiting >= context.LoadTestRunner.runningThreadCount) {
				 monitor.set(0)
				 monitor.notifyAll()
			  } else {
				 monitor.wait()
			  }
		   }
		}
	}
}
