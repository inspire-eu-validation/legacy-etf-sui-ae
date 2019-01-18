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
import de.interactive_instruments.xtf.exceptions.InvalidProjectParameterException
import de.interactive_instruments.xtf.exceptions.RequiredDomNodeNotFoundException

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.namespace.QName

@XmlAccessorType( XmlAccessType.FIELD )
@XmlRootElement
public class Bbox implements TransferableRequestParameter {
	private double lx;
	private double ly;
	private double ux;
	private double uy;
	private int epsgCode;
	private double scaleHint;
	// Max border for the instance
	// (used for scaling)
	private Bbox maxBbox;
	
	private Bbox() { }
	
	/*
	 * Creates a copy of a Bbox object,
	 * but discards the internal maxBBOX. 
	 */
	public Bbox(Bbox aBbox) {
		this.lx = aBbox.lx;
		this.ly = aBbox.ly;
		this.ux = aBbox.ux;
		this.uy = aBbox.uy;
		this.epsgCode = aBbox.epsgCode;
		this.scaleHint = aBbox.scaleHint;
		this.maxBbox=null;
	}
	
	/**
	 * Creates a new BBOX
	 */
	public Bbox(double lx, double ly, double ux, double uy, epsgCode=0) {
		this.lx = lx;
		this.ly = ly;
		this.ux = ux;
		this.uy = uy;
		this.epsgCode = epsgCode;
	}
	
	public double[] getAsArray() {
		return [ this.lx, this.ly, this.ux, this.uy ];
	}
	
	/**
	 * Creates a new BBOX object from a XmlHolder object and a XPath to a Node
	 * which might be in a WFS/WMS-Capabilites document or GML envelope. 
	 * The internal maxBBOX is set to the this BBOX.	
	 */
	public Bbox(XmlHolder xml, String xpath) {

		// Set from WFS Capabilities
		if(xml.getDomNode(xpath+"//*:LowerCorner")!=null) {
			this.lx = xml.getNodeValue(xpath+"//*:LowerCorner").split(" ")[1].toDouble();
			this.ly = xml.getNodeValue(xpath+"//*:LowerCorner").split(" ")[0].toDouble();
			this.ux = xml.getNodeValue(xpath+"//*:UpperCorner").split(" ")[1].toDouble();
			this.uy = xml.getNodeValue(xpath+"//*:UpperCorner").split(" ")[0].toDouble();
			this.epsgCode=4326;
		}
		
		// Set from Gml Envelope
		if(xml.getDomNode(xpath+"//*:lowerCorner")!=null) {
			String srs = xml.getNodeValue(xpath+"//*:Envelope/@srsName");
			srs = srs.substring(srs.lastIndexOf(":")+1, srs.length());
			this.epsgCode=srs.toInteger();
			this.lx = xml.getNodeValue(xpath+"//*:lowerCorner").split(" ")[0].toDouble();
			this.ly = xml.getNodeValue(xpath+"//*:lowerCorner").split(" ")[1].toDouble();
			this.ux = xml.getNodeValue(xpath+"//*:upperCorner").split(" ")[0].toDouble();
			this.uy = xml.getNodeValue(xpath+"//*:upperCorner").split(" ")[1].toDouble();
		}
		
		// WMS Capabilities
		// XPath has to be set to the "BoundingBox" element
		if(xml.getDomNode(xpath+"/@minx")!=null) {
			String srs = xml.getNodeValue(xpath+"/@CRS");
			if(srs==null) {
				// WMS 1.1.x
				srs = xml.getNodeValue(xpath+"/@SRS");
			}
			srs = srs.substring(srs.lastIndexOf(":")+1, srs.length());
			this.epsgCode=srs.toInteger();
			this.lx = xml.getNodeValue(xpath+"/@minx").toDouble();
			this.ly = xml.getNodeValue(xpath+"/@miny").toDouble();
			this.ux = xml.getNodeValue(xpath+"/@maxx").toDouble();
			this.uy = xml.getNodeValue(xpath+"/@maxy").toDouble();
		}
		
		if(this.epsgCode==0) {
			throw new RequiredDomNodeNotFoundException(this, "Unable to create BBOX from XML ");
		}
		
		// Set the borders as maxBbox
		this.maxBbox = new Bbox(this);
	}
	
	/**
	 * Creates a new BBOX from a String.
	 * Syntax example epsg:1234,62.0,56.1,91.0,59.0
	 * @param bboxString
	 */
	public Bbox(String bboxString) {
		def bbox = 	bboxString.split(',');
		if(bbox.size()!=5) {
			throw new InvalidProjectParameterException(this,
				"Unable to parse maxBBOX \""+bboxString+"\"");
		}
		int epsgCodePos = bbox[0].indexOf("epsg:");
		if(epsgCodePos==-1) {
			throw new InvalidProjectParameterException(this,
				"Unable to parse maxBBOX \""+bboxString+"\"."+
				"\"epsg:\" expected in front of EPSG-Code.");
		}
		
		try {
			this.lx = bbox[1].toDouble()
			this.ly = bbox[2].toDouble()
			this.ux = bbox[3].toDouble();
			this.uy = bbox[4].toDouble();
			this.epsgCode = bbox[0].substring(epsgCodePos+5).toInteger();
		}catch(Exception e) {
			throw new InvalidProjectParameterException(this,
				"Unable to parse maxBBOX \""+bboxString+
				"\" "+e.getMessage());
		}
	}
	
	public getLX() { return lx; }
	public getLY() { return ly; }
	public getUX() { return ux; }
	public getUY() { return uy; }
	
	public void setBbox(double lx, double ly, double ux, double uy) {
		this.lx=lx; this.ly=ly; this.ux=ux; this.uy=uy;
	}
	
	public void setScaleHint(double scaleHint) {
		this.scaleHint=scaleHint;
	}
	
	public void setMaxBbox(Bbox maxBbox) {
		this.maxBbox = maxBbox;
	}
	
	public Bbox getMaxBbox() {
		return this.maxBbox;
	}
	
	/**
	 * Return a BBOX Object as gml:Envelope string
	 * @return
	 */
	public String getGmlEnvelope(String namespacePrefix="gml") {
		String envelope = "<"+namespacePrefix+":Envelope xmlns:"+namespacePrefix+
			"='http://www.opengis.net/gml' srsName=\"EPSG:"+this.epsgCode+"\">\n";
		envelope += "<"+namespacePrefix+":lowerCorner>"+lx.toString()+" "+ly.toString()+
				"</"+namespacePrefix+":lowerCorner>\n"
		envelope += "<"+namespacePrefix+":upperCorner>"+ux.toString()+" "+uy.toString()+
				"</"+namespacePrefix+":upperCorner>\n"
		envelope += "</"+namespacePrefix+":Envelope>\n";
	}
	
	/**
	 * Return a BBOX Object as gml:Envelope string
	 * @return
	 */
	public String getGmlEnvelope(QName gml) {
		String envelope = "<"+gml.getPrefix()+":Envelope xmlns:"+gml.getPrefix()+
			"='"+gml.getNamespaceURI()+"' srsName=\"EPSG:"+this.epsgCode+"\">\n";
		envelope += "<"+gml.getPrefix()+":lowerCorner>"+lx.toString()+" "+ly.toString()+
				"</"+gml.getPrefix()+":lowerCorner>\n"
		envelope += "<"+gml.getPrefix()+":upperCorner>"+ux.toString()+" "+uy.toString()+
				"</"+gml.getPrefix()+":upperCorner>\n"
		envelope += "</"+gml.getPrefix()+":Envelope>\n";
	}
	
	public int getEpsgCode() { return epsgCode; }
	
	public void setEpsgCode(int epsg) { this.epsgCode=epsg; }
	
	/**
	 * Simple String representation: lx, ly, ux, uy
	 */
	public String toString() {
		return lx+","+ly+","+ux+","+uy;
	}

	private Bbox borderBbox(
		double lx,
		double ly,
		double ux,
		double uy,
		int epsgCode,
		Bbox maxBbox=null)
	{
		if(maxBbox!=null) {
			if(lx<maxBbox.getLX())
				lx=maxBbox.getLX();
			if(ly<maxBbox.getLY())
				ly=maxBbox.getLY();
			if(ux>maxBbox.getUX())
				ux=maxBbox.getUX();
			if(uy>maxBbox.getUY())
				uy=maxBbox.getUY();
		}else if(this.maxBbox!=null) {
			if(lx<this.maxBbox.getLX())
				lx=this.maxBbox.getLX();
			if(ly<this.maxBbox.getLY())
				ly=this.maxBbox.getLY();
			if(ux>this.maxBbox.getUX())
				ux=this.maxBbox.getUX();
			if(uy>this.maxBbox.getUY())
				uy=this.maxBbox.getUY();
		}
		return new Bbox(lx,ly,ux,uy,epsgCode);
	}
	
	/**
	 * Returns a random BBOX within the BBOX passed
	 */
	public getScaledRandomBbox(double factor) {
		def random = new Random();

		double difx = (this.ux - this.lx);
		double dify = (this.uy - this.ly);
		double x = (difx*factor - difx)/2;
		double y = (dify*factor - dify)/2;

		double xOffset=(random.nextDouble()*2-1)*x*2;
		double yOffset=(random.nextDouble()*2-1)*y*2;
		
		double lx = this.lx-x+xOffset;
		double ly = this.ly-y+yOffset;
		double ux = this.ux+x+xOffset;
		double uy = this.uy+y+yOffset;
		
		Bbox scaledBBOX = borderBbox(lx,ly,ux,uy,epsgCode);
		return scaledBBOX;
	}
		
	/**
	 * Scales the BBOX.
	 */
	public Bbox getScaledBbox(double factor=0, Bbox maxBbox=null) {
	
		if(factor==0) {
			factor=this.scaleHint;
		}
	
		double difx = (this.ux - this.lx);
		double dify = (this.uy - this.ly);
		double x = (difx*factor - difx)/2;
		double y = (dify*factor - dify)/2;

		double lx = this.lx-x;
		double ly = this.ly-y;
		double ux = this.ux+x;
		double uy = this.uy+y;
	
		/*
		SOAPUI_I.getInstance().getLog().info("");
		SOAPUI_I.getInstance().getLog().info("--------IN--------");
		SOAPUI_I.getInstance().getLog().info("LX "+this.lx);
		SOAPUI_I.getInstance().getLog().info("LY "+this.ly);
		SOAPUI_I.getInstance().getLog().info("UX "+this.ux);
		SOAPUI_I.getInstance().getLog().info("UY "+this.uy);
		SOAPUI_I.getInstance().getLog().info("Diff x : "+difx);
		SOAPUI_I.getInstance().getLog().info("Diff y : "+dify);
		SOAPUI_I.getInstance().getLog().info("New x : "+x);
		SOAPUI_I.getInstance().getLog().info("New y : "+y);
		SOAPUI_I.getInstance().getLog().info("--------OUT-------");
		SOAPUI_I.getInstance().getLog().info("LX "+lx);
		SOAPUI_I.getInstance().getLog().info("LY "+ly);
		SOAPUI_I.getInstance().getLog().info("UX "+ux);
		SOAPUI_I.getInstance().getLog().info("UY "+uy);
		*/

		Bbox scaledBBOX = borderBbox(lx,ly,ux,uy,this.epsgCode,maxBbox);
		return scaledBBOX;
	}
	
	/**
	 * Check if the BBOX is valid
	 */
	public boolean isValidSurface() {
		if(lx>=ux || ly>=uy) {
			return false;
		}
		return true;
	}
	
	public boolean equals(Bbox bbox) {
		return this.lx == bbox.getLX() && this.ly == bbox.getLY() &&
			this.ux == bbox.getUX() && this.uy == bbox.getUY() &&
			this.epsgCode == bbox.getEpsgCode();
	}
	public boolean contains(Bbox bbox) {
		return this.lx <= bbox.getLX() && this.ly <= bbox.getLY() &&
			this.ux >= bbox.getUX() && this.uy >= bbox.getUY();
	}
	
	/**
	 *  Set BBOX and BBOX.EPSG in Transfer_Properties Step
	 */
	public void transferProperties() {
		ProjectHelper ph = new ProjectHelper();
		ph.setTransferProperty(
			"BBOX",
			this.toString());
		ph.setTransferProperty(
			"BBOX.EPSG",
			"EPSG:"+this.epsgCode );
	}
}
