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
import de.interactive_instruments.xtf.Bbox
import de.interactive_instruments.xtf.HttpRequest

class GetFeatureRequest extends HttpRequest{
	
	private String handle;
	private int maxFeatures;
	private String outputFormat;
	
	public void setOutputFormat(String outputFormat) {
		this.outputFormat=outputFormat;
	}
	
	public void setHandle(String handle) {
		this.handle=handle;
	}

	public setMaxFeatures(int maxFeatures) {
		this.maxFeatures=maxFeatures;
	}
	
	
	public addFeatureTypeQuery(FeatureType featureType, String propertyName=null, Bbox bbox=null) {
		// addFeatureTypeQuery(featureType.getPrefixAndName(), 
		//	"xmlns:"+featureType.getPrefix()+"=\""+featureType.getNamespaceURI()+"\"",
		//	propertyName, bbox);
		addFeatureTypeQuery(featureType.getName(), 
			"xmlns:"+featureType.getNamespacePrefix()+"=\""+featureType.getNamespace()+"\"",
			propertyName, bbox);
	}
	
	
	public addFeatureTypeQuery(String featureTypeName, String namespace, String propertyName=null, Bbox bbox=null) {
		request+="<wfs:Query typeName=\""+featureTypeName+"\" "+namespace+" ";
		if(bbox!=null) {
			request+='''>
			<ogc:Filter>
				<ogc:BBOX>
			'''
				if(propertyName!=null)
				{
					request+="<ogc:PropertyName>"+propertyName.trim()+"</ogc:PropertyName>\n";
				}
				request+=bbox.getGmlEnvelope();
				
				request+='''
				</ogc:BBOX>
			</ogc:Filter>
		</wfs:Query>
			'''
		}else{
			request+="/>";
		}
	}

	XmlHolder submit() {
		def requestHeader = '''<wfs:GetFeature version="1.1.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:gml="http://www.opengis.net/gml" xmlns:ogc="http://www.opengis.net/ogc" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" '''
		
		if(handle!=null)
			requestHeader+=" handle=\"XTF_"+handle+"\" ";
		if(maxFeatures!=0)
			requestHeader+=" maxFeatures=\""+maxFeatures+"\" ";
		if(resultType!=null)
			requestHeader+=" resultType=\""+resultType+"\" ";
		if(outputFormat!=null)
			requestHeader+=" outputFormat=\""+outputFormat+"\" ";
		requestHeader+=">\n";
		
		String request = requestHeader+request+"</wfs:GetFeature>"
		log.info("Sending request");
		httpLog.info("Sending "+request);
		return sendRequest(request);
	}
}
