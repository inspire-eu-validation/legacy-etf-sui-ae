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

import org.w3c.dom.Element
import org.w3c.dom.Node

import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class XmlUtil {
	
	public static Node getFirstChildNodeOfType(Node node, short nodeType) {
		Node childNode = node.getFirstChild();
		while (childNode != null && childNode.getNodeType() != nodeType) {
			childNode = childNode.getNextSibling();
		}
		return childNode;
	}
	
	// Returns the attribute of a given node or a default value
	public static String getAttribValue(Node node, String item, String defaultValue=null) {
		if(node==null || node.getAttributes()==null || 
			node.getAttributes().getNamedItem(item)==null) {
			if(defaultValue!=null) {
				return defaultValue;
			}
			return null;
		}
		String value = node.getAttributes().getNamedItem(item).getNodeValue();
		if(value==null)
			return defaultValue;
		return value;
	}
	
	// Return the attribute of a node without namespaces
	public static String getAttribValWithoutUrn(Node node, String item) {
		String attribute = getAttribValue(node, item);
		if(attribute==null) {
			return null;
		}
		if (attribute.indexOf(":")==-1)
			return attribute;
		return attribute.substring(attribute.indexOf(":")+1);
	}
	
	// Return the attribute of a node. If the attribute does not have a namespace
	// add one
	public static String getAttribValWithUrn(Node node, String item, String ns) {
		String attribute = getAttribValue(node, item);
		if(attribute==null) {
			return null;
		}
		if (attribute.indexOf(":")==-1) {
			return ns+":"+attribute;
		}else{
			return attribute;
		}
	}
	
	// Replaces a prefix with a '*'
	public static String getWildcardNS(String xpath) {
		// replace namespace with '*'
		def matcher = (xpath =~ /\w+:/);
		return matcher.replaceAll("*:");
	}
	
	public static String getQualifiedName(String xpath) {
		// replace namespace with ''
		def matcher = (xpath =~ /\w+:/);
		return matcher.replaceAll("");
	}
	
	public static String getNamespacePrefix(String xpath) {
		final int pos = xpath.indexOf(":");
		if(pos>0) {
			return xpath.substring(0, pos);
		}
		return null;
	}
	
	// Change multiple attributes
	public static void setAttributes(Node[] domNodes, String itemName, String value) {
		if(domNodes!=null && domNodes.length!=0) {
			for (domNode in  domNodes) {
				setAttribute(domNode, itemName, value);
			}
		}
	}
	
	// Lookup namespace for attribute
	public static void setAttribute(Node domNode, String itemName, String value) {
		Element element = domNode;
		String ns = lookupNamespaceURI(getNamespacePrefix(itemName),domNode);
		element.setAttributeNS(ns, itemName, value);
	}
	
	// Delete multiple nodes
	public static void delAttributes(Node[] domNodes, String itemName) {
		if(domNodes!=null && domNodes.length!=0) {
			for (domNode in  domNodes) {
				Element element = domNode;
				element.removeAttribute(itemName);
			}
		}
	}
	
	// Change multiple nodes. The second argument will select the targets based on list 
	// of nodes passed with the first arguments. Nodes in path that do not exist will be
	// created!
	public static void setNodesFromPaths(Node[] domNodes, String[] paths, String value) {
		if(domNodes!=null && domNodes.length!=0 &&
			paths!=null && paths.length!=0 ) {
			for (domNode in  domNodes) {
				for (path in  paths) {
					setNodeFromPath(domNode, path, value);
				}
			}
		}
	}
	
	public static void setNodeFromPath(Node domNode, String path, String value) {
		// Disassemble simple xpath components
		int lastPos = path.indexOf("/");
		String remPath=null;
		if(lastPos==-1) {
			lastPos=path.size();
		}else{
			remPath=path.substring(lastPos+1, path.size());
		}
		String nodeName = path.substring(0, lastPos);
		
		if(nodeName.indexOf("@")==0) {
			// Last node in path is an attribute, -create it
			nodeName=nodeName.substring(1, nodeName.size());
			
			setAttribute(domNode, nodeName, value);
		}else{	
			Node ignNode = getOrCreateChildNodeByName(domNode, nodeName);
				
			if(remPath==null) {
				// End of path, -create or overwrite a text node
				Node textNode = getFirstChildNodeOfType(ignNode, Node.TEXT_NODE );
				if(textNode==null) {
					ignNode.appendChild( 
						ignNode.getOwnerDocument().createTextNode(value)
						);
				}else{
					textNode.setNodeValue(value);
				}
			}else{
				// Interpret next node
				setNodeFromPath(ignNode, remPath, value);
			}
		}
	}
	
	public static Node getOrCreateChildNodeByName(Node curNode, String name) {
		Node childNode = curNode.getFirstChild();
		while(childNode!=null) {
			if(getQualifiedName(childNode.getNodeName()).equals(getQualifiedName(name))) {
				return childNode;
			}
			childNode = childNode.getNextSibling();
		}
		
		// Not found, create a new node
		childNode=createNodeWithNS(name, curNode);
		curNode.appendChild(childNode);
		return childNode;
	}
	
	// Create and lookup namespace for node from prefix
	public static Node createNodeWithNS(String name, Node node) {
		String ns = lookupNamespaceURI(getNamespacePrefix(name),node);
		if(ns==null) {
			ns=node.getNamespaceURI();
		}
		Node newNode=node.getOwnerDocument().createElementNS(ns, getQualifiedName(name));
		return newNode;
	}
	
	// Simple Dom2 Level implementation of org.w3c.dom.Node.lookupNamespaceURI()
	// Returns null or the namespace found
	public static String lookupNamespaceURI(String prefix, Node node) {
		String namespace=null;
		if(node.getPrefix()!= null && node.getPrefix().equals(prefix)) {
			namespace = node.getNamespaceURI();
		}else{
			namespace = getAttribValue(node, "xmlns:"+prefix);
		}
		if(namespace==null && node.getParentNode()!=null) {
			namespace = lookupNamespaceURI(prefix, node.getParentNode());
		}
		return namespace;
	}
	
	/**
	 * NodeToString
	 */
	public static String nodeToString(Node node) {
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		StringWriter sw = new StringWriter();
		t.transform(new DOMSource(node), new StreamResult(sw));
		return sw.toString();
	}
}
