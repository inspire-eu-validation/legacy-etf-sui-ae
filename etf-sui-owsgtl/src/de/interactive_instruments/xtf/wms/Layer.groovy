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
package de.interactive_instruments.xtf.wms

import com.eviware.soapui.support.XmlHolder
import de.interactive_instruments.xtf.Bbox
import de.interactive_instruments.xtf.ProjectHelper
import de.interactive_instruments.xtf.TransferableRequestParameter

import javax.xml.bind.Unmarshaller
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlTransient

@XmlAccessorType( XmlAccessType.FIELD )
@XmlRootElement
public class Layer implements TransferableRequestParameter {
	
	@XmlTransient
	private Layer parentLayer;
	private boolean queryable;
	private String title;
	private String name;
	private String[] styleNames;
	private List <Bbox> bBoxes;
	private List <Layer> subLayers;
	
	public Layer() { }
	
	// XPath has to be set to a "Layer" element
	public Layer(XmlHolder layerXml, Layer parent=null) {
		this.parentLayer=parent;
		
		queryable=true;
		String queryableAttrib=layerXml.getNodeValue("/*:Layer/@queryable");
		if(queryableAttrib==null && parent!=null) {
			queryable=parent.isQueryable();
		}else{
			queryable=queryableAttrib.equals("1");
		}
		
		title=layerXml.getNodeValue("/*:Layer/*:Title");
		
		name=layerXml.getNodeValue("/*:Layer/*:Name");
		
		styleNames=layerXml.getNodeValues("/*:Layer/*:Style/*:Name");

		bBoxes = new ArrayList<Bbox>();
		
		// LatLonBoundingBox		
		Bbox latLonBbox=null;
		if(layerXml.getDomNode("/*:Layer/*:LatLonBoundingBox")!=null) {
			double minx = Double.valueOf(layerXml.getNodeValue("/*:Layer/*:LatLonBoundingBox/@minx"));
			double miny = Double.valueOf(layerXml.getNodeValue("/*:Layer/*:LatLonBoundingBox/@miny"));
			double maxx = Double.valueOf(layerXml.getNodeValue("/*:Layer/*:LatLonBoundingBox/@maxx"));
			double maxy = Double.valueOf(layerXml.getNodeValue("/*:Layer/*:LatLonBoundingBox/@maxy"));
			latLonBbox=new Bbox(minx,miny,maxx,maxy, 4326);
		}else if(layerXml.getDomNode("/*:Layer/*:EX_GeographicBoundingBox")!=null) {
			double minx = Double.valueOf(layerXml.getNodeValue("/*:Layer/*:EX_GeographicBoundingBox/*:westBoundLongitude"));
			double miny = Double.valueOf(layerXml.getNodeValue("/*:Layer/*:EX_GeographicBoundingBox/*:southBoundLatitude"));
			double maxx = Double.valueOf(layerXml.getNodeValue("/*:Layer/*:EX_GeographicBoundingBox/*:eastBoundLongitude"));
			double maxy = Double.valueOf(layerXml.getNodeValue("/*:Layer/*:EX_GeographicBoundingBox/*:northBoundLatitude"));
			latLonBbox=new Bbox(minx,miny,maxx,maxy, 4326);
		}
		if(latLonBbox!=null) {
			latLonBbox.setMaxBbox(new Bbox(latLonBbox));
			bBoxes.add(latLonBbox);
		}
			
		// Bboxes for other SRS
		String boundingBoxXPath = "/*:Layer/*:BoundingBox";
		int numberOfBboxes = layerXml.getNodeValue(
			"count("+boundingBoxXPath+")").toInteger();
		if(numberOfBboxes>0) {
			for(i in 1..numberOfBboxes) {
				Bbox bbox = new Bbox(layerXml, boundingBoxXPath+"["+i+"]");
				// Ignore EPSG:4326 here
				if(bbox.getEpsgCode()!=4326) {
					bBoxes.add(bbox);
				}
			}
		}
		
		
		// Create sub layers if necessary
		String layerXPath = "/*:Layer/*:Layer";
		int numberOfLayers = layerXml.getNodeValue(
			"count("+layerXPath+")").toInteger();
		if(numberOfLayers>0) {
			subLayers = new ArrayList<Layer>();
			for(i in 1..numberOfLayers) {
				Layer layer = new Layer(new XmlHolder(
					layerXml.getDomNode(layerXPath+"["+i+"]")),
					this );
				subLayers.add(layer);
			}
		}
	}
	
		
	public boolean isQueryable() {
		return queryable;
	}
	
	public String getTitle() {
		if(title!=null) {
			return title;
		}else if(parentLayer!=null) {
			return parentLayer.getTitle();
		}
		return null;
	}
	
	// Returns a List of all Bboxes including the
	// LatLonBoundingBox with EPSG:4326 which is the
	// first Element
	public List <Bbox> getBboxes() {
		if(bBoxes!=null) {
			return bBoxes;
		}else if(parentLayer!=null) {
			return parentLayer.getBboxes();
		}
		return null;
	}
	
	public Bbox getBbox(int epsg) {
		if(bBoxes!=null) {
			for(bbox in bBoxes) {
				if(bbox.getEpsgCode()==epsgCode) {
					return bbox;
				}
			}
		}
		return null;
	}
		
	public String getName() {
		if(name!=null) {
			return name;
		}else if(parentLayer!=null) {
			return parentLayer.getName();
		}
		return null;
	}
	
	public String[] getStyleNames() {
		if(styleNames!=null) {
			return styleNames;
		}else if(parentLayer!=null) {
			return parentLayer.getStyleNames();
		}
		return null;
	}
	
	// Returns the direct sub layers if parameter deep is false,
	// or all sub layers if parameter deep is true. Default is deep.
	// Returns null if the layer has no subLayers
	public List <Layer> getSubLayers(boolean deep=true) {
		if(deep==false) {
			return this.subLayers;
		}else if(this.subLayers!=null) {
			List <Layer> allSubLayers = new ArrayList<Layer>();
			for(directSubLayer in this.subLayers) {
				// Add the subLayer to the list
				allSubLayers.add(directSubLayer);
				// and its subLayers
				List <Layer> directSubLayerSubLayers = directSubLayer.getSubLayers(true);
				if(directSubLayerSubLayers!=null) {
					allSubLayers.addAll(directSubLayerSubLayers);
				}
			}
			return allSubLayers;
		}
		return null;
	}
		
	public Layer getRandomSubLayer() {
		def random = new Random();
		int pos = random.nextInt(this.subLayers.size());
		return this.subLayers[pos];
	}
		
	public void afterUnmarshal(Unmarshaller u, Object parent) {
		this.parentLayer = (Layer)parent;
	}
	
	public void transferProperties() {
		ProjectHelper ph = new ProjectHelper();
		ph.setTransferProperty(
			"LAYER.NAME", this.name
		);
	}
}
