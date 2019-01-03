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
import de.interactive_instruments.xtf.exceptions.NamespaceHolderException
import org.apache.xerces.xs.StringList

import javax.xml.bind.annotation.*
import java.util.Map.Entry

@XmlAccessorType( XmlAccessType.FIELD )
@XmlRootElement
public class NamespaceHolder {

	@XmlID
	@XmlAttribute(required=true)
	String id;
	Map<String, String> namespacePrefixMap;

	final static Set<String> forbiddenPrefixes = ['wfs', 'ows', 'ogc', 'xsi'] as Set<String>;

	public NamespaceHolder() {
		this.id="hc_"+this.hashCode();
		namespacePrefixMap = new TreeMap<String, String>();
	}

	public NamespaceHolder(StringList namespaceList) throws NamespaceHolderException {
		this.id="hc_"+this.hashCode();
		namespacePrefixMap = new TreeMap<String, String>();

		// Parse the namespace and build usable
		for(int i=0; i<=namespaceList.getLength(); i++) {

			String namespaceUri = namespaceList.item(i);
			// final String ignoredNamespaceUri="http://www.w3.org/";
			if(namespaceUri==null) {
				continue;
			}

			// Ignore w3.org namespace declarations
			if(namespaceUri.startsWith("http://www.w3.org") ) {
				if(namespaceUri.contains("namespace") ||
					namespaceUri.contains("XMLSchema") ||
					namespaceUri.contains("SMIL20") )
				{
					continue;
				}
			}

			// Try to find a usable prefix from the namespace uri
			final int lastSlashPos = namespaceUri.lastIndexOf("/");
			if(lastSlashPos!=-1) {
				// Is the last segment of the namespace URI:
				// 3 characters long and not a version
				final String lastSegment = namespaceUri.substring(
						lastSlashPos+1, namespaceUri.length());

				if(!lastSegment.contains(".") &&
						lastSegment.length()>=3 &&
						!forbiddenPrefixes.contains(lastSegment)
					)
				{
					setNamespaceUriAndPrefix(namespaceUri, lastSegment);
					continue;
				}else if(namespaceUri.indexOf("/")!=lastSlashPos){
					// Check the next to last segment
					final int nextToLastSlash =  namespaceUri.substring(0, lastSlashPos).lastIndexOf("/");
					final String nextToLastSegment = namespaceUri.substring(
							nextToLastSlash+1, lastSlashPos);
					if(!nextToLastSegment.contains(".") &&
							nextToLastSegment.length()>=3 &&
							!forbiddenPrefixes.contains(nextToLastSegment)
						)
					{
						// i.e. "gml_3.2"
						setNamespaceUriAndPrefix(namespaceUri, nextToLastSegment+"_"+lastSegment);
						continue;
					}
				}
			}

			// Not usable, just name it nsx
			String prefix="ns"+(namespacePrefixMap.size()+1);
			setNamespaceUriAndPrefix(namespaceUri, prefix);
		}
	}

	public String getPrefixForNamespaceUri(String namespaceUri) throws NamespaceHolderException {
		if(namespaceUri==null) {
			throw new NullPointerException("NamespaceURI is NULL");
		}
		final String prefix = namespacePrefixMap.get(namespaceUri);
		if(prefix==null) {
			throw new NamespaceHolderException("Prefix not found for namespace: "+namespaceUri);
		}
		return prefix;
	}

	public String getNamespaceUriForPrefix(String prefix) throws NamespaceHolderException {
		if(prefix==null) {
			throw new NullPointerException("prefix is NULL");
		}
		for(Entry<String, String> item : namespacePrefixMap.entrySet()) {
			if(item.getValue().equals(prefix)) {
				return item.getKey();
			}
		}
		throw new NamespaceHolderException("Prefix not found for namespaceUri: "+prefix);
	}


	public void setNamespaceUriAndPrefix(String namespaceUri, String prefix) throws NamespaceHolderException {
		if(namespaceUri==null || prefix==null) {
			throw new NullPointerException("NamespaceURI or Prefix is NULL");
		}
		if(namespacePrefixMap.containsValue(prefix)) {
			throw new NamespaceHolderException(
					"Cannot set prefix "+prefix+" for namespaceURI '"+namespaceUri+
				"'. NamespaceURI '"+this.getNamespaceUriForPrefix(prefix)+"' already stored for this prefix.");
		}
		namespacePrefixMap.put(namespaceUri, prefix);
	}

	/*
	* Namespaces are returned in the format:
	* xmlns:<namespacePrefix>='<namespaceURL>'
	*/
	public String getDeclarations() {
		String namespaceDeclaration=" ";
		for(Entry<String, String> item : namespacePrefixMap.entrySet()) {
			namespaceDeclaration+="xmlns:"+item.getValue()+"='"+item.getKey()+"' ";
		}
		return namespaceDeclaration;
	}

	/*
	 * Namespaces are returned in the format:
	 * declare namespace <namespacePrefix>='<namespaceURL>';
	 */
	public String getDeclarationsForXExpressions() {
		String namespaceDeclaration="";
		for(Entry<String, String> item : namespacePrefixMap.entrySet()) {
			namespaceDeclaration+="declare namespace "+item.getValue()+
			"='"+item.getKey()+"';"+System.getProperty("line.separator");
		}
		return namespaceDeclaration;
	}

	/*
	* Namespaces are returned in the format:
	* xmlns(<namespacePrefix>=<namespaceURL>)
	*/
	public String getDeclarationsForGetMethod() {
		String namespaceDeclaration="";
		for(Iterator<Map.Entry<String, String>> it =
			namespacePrefixMap.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<String, String> item = it.next();
			namespaceDeclaration+="xmlns("+item.getValue()+"="+item.getKey()+")";
			if(it.hasNext()) {
				namespaceDeclaration+=",";
			}
		}
		return namespaceDeclaration;
	}

	/*
	* Namespaces are returned in the format:
	* xmlns(<namespacePrefix>,<namespaceURL>)
	*/
	public String getDeclarationsForGetMethodWfs2() {
		String namespaceDeclaration="";
		for(Iterator<Map.Entry<String, String>> it =
			namespacePrefixMap.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<String, String> item = it.next();
			assert item.getValue() != ""
			assert item.getKey() != ""
			namespaceDeclaration+="xmlns("+item.getValue()+","+item.getKey()+")";
			if(it.hasNext()) {
				namespaceDeclaration+=",";
			}
		}
		return namespaceDeclaration;
	}

	public Set<Map.Entry<String, String>> getEntrySet() {
		return namespacePrefixMap.entrySet();
	}

	/*
	 * Declare Namespaces for a XmlHolder object
	 */
	public void declareNamespaces(XmlHolder holder) {
		for(Iterator<Map.Entry<String, String>> it =
			namespacePrefixMap.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<String, String> item = it.next();
			holder.declareNamespace(item.getValue(), item.getKey());
		}
	}
}
