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

import com.eviware.soapui.support.XmlHolder
import groovyx.net.http.HTTPBuilder
import org.apache.log4j.Logger

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.POST;

class HttpRequest extends SOAPUI_I{
	
	protected def http;
	protected String request;
	protected boolean dropResponse;
	protected String resultType;
	protected Logger httpLog;
		
	public void setDropResponse(boolean drop) { this.dropResponse=drop; }
	
	public void resetRequest() {
		request="";
	}
	
	public void setResultType(String resultType) {
		this.resultType=resultType;
	}
	
	public XmlHolder sendRequest(String request) {

		def xmlResponse=null;
	
		http.request(POST,TEXT) { req ->
			headers.'Accept' = 'application/xml'
			body = request
			
			String username=Util.getProjectPropertyOrNull("basicAuthUser");
			if(username!=null) {
				def auth = username + ':' + Util.getProjectPropertyOrNull("basicAuthPwd");
                                def encodedAuth = auth.bytes.encodeBase64().toString();
				headers.'Authorization' = 'Basic ' + encodedAuth;
			}
						
			response.success = { resp, reader ->
			assert resp.statusLine.statusCode == 200
			

	
				Long respSize=0
				Reader r = new BufferedReader(reader)
	
				
				if(dropResponse)
				{
					while(r.read()!=-1)
						respSize++;
				}else{
					resp.headers.each { 
						httpLog.info( "  ${it.name} : ${it.value}" );
					}
					StringBuilder sb = new StringBuilder();
					char[] buf = new char[1024];
					int charsRead;
					while((charsRead = r.read(buf)) != -1) {
						sb.append(buf, 0, charsRead);
					}
				
					// System.out << reader
					// String line;
					// while ((line = reader.readLine()) != null)
					// {
					// 	log.info( line );
					// }
					
					log.info("Response received");
					httpLog.info("Received "+sb.toString() );
					
					def xml = null;					
					try {
						xml=new XmlHolder(sb.toString());
					} catch( e ) {
						log.warn("XML Parser is unable to parse response");
					}
					
					return xml;
				}
			
				log.info("Response received");
			
				if(respSize < 800 ) 
					throw new Exception("Response size is less than 800 bytes: This might be a wfs:exception!");
				
				return null;
			}

			response.failure = { resp -> 
				log.error("Unable to receive response. Server returned "
				+resp.statusLine.statusCode);
				log.error("Request: "+request);
				throw new Exception("Unable to receive response. Server returned "
				+resp.statusLine.statusCode);
			}
		}
	}
	
	public HttpRequest(String endpoint=null) {
		if(!endpoint)
			endpoint=testRunner.testCase.testSuite.project.getPropertyValue("serviceEndpoint");
		request="";
		dropResponse=false;
		http = new HTTPBuilder(endpoint);
		httpLog = log.getLogger("httpclient.wire");
	}
}
