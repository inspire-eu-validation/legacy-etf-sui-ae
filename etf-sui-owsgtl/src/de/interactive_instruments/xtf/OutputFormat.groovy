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

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement

enum FormatType { UNKNOWN, TEXT, XML, JSON, IMAGE };

@XmlAccessorType( XmlAccessType.FIELD )
@XmlRootElement
public class OutputFormat implements Comparable<OutputFormat> {

	private String outputFormat;
	private FormatType type;

	public OutputFormat() { }
	
	public OutputFormat(String outputFormat) {
		this.outputFormat = outputFormat;
		this.type = FormatType.UNKNOWN; 
		
		if(outputFormat.indexOf("xml")!=-1 || 
			outputFormat.indexOf("gml")!=-1)
		{
			type=FormatType.XML;
		}else if(outputFormat.indexOf("json")!=-1) {
			type=FormatType.JSON;
		}else if(outputFormat.indexOf("text")!=-1) {
			type=FormatType.TEXT;
		}
	}
	
	public boolean equals(OutputFormat outputFormat) {
		return this.getFormat().equals(outputFormat.getFormat());
	}
	
	public String toString() {
		return this.outputFormat;
	}
	
	public String getFormat() {
		return this.outputFormat;
	}
	
	public boolean isParsable() {
		// JSON is parsable as XML in SoapUI
		return (type==FormatType.XML || FormatType.JSON);
	}
	
	public boolean isText() {
		return (type==FORMAT.TEXT);
	}

	@Override
	int compareTo(OutputFormat o) {
		return outputFormat.compareTo(o.getFormat())
	}
}
