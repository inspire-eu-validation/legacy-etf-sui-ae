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

import de.interactive_instruments.xtf.ProjectHelper
import de.interactive_instruments.xtf.TransferableRequestParameter
import de.interactive_instruments.xtf.exceptions.MaxDepthExceededException
import de.interactive_instruments.xtf.exceptions.SchemaAnalysisException
import org.apache.log4j.Logger
import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl
import org.apache.xerces.impl.xs.XSComplexTypeDecl
import org.apache.xerces.xs.*

import javax.xml.bind.annotation.*

@XmlAccessorType( XmlAccessType.NONE )
@XmlRootElement
public class FeatureTypeProperty implements TransferableRequestParameter {

	@XmlElement
	private String xsdType;

	@XmlElement
	private List<String> xPathSegments;

	@XmlElement
	private PropertyType type;
	// Reference the associated NamespaceHolder for de-/serialization

	@XmlIDREF
	@XmlAttribute(name="nshId", required=true)
	private final NamespaceHolder nsHolder;

	// For cycle detection
	@XmlTransient
	private TreeSet<String> visitedNodes;
	// For cycle detection
	@XmlTransient
	private String lastSegment;

	// Number of elements in xpath
	private int depth;

	/*
	 *  Reaching the max depth indicates cycles in the analysis process
	 *  and will result in a MaxDepthExceededException
	 */
	private static int maxDepth=500;

	// Use the same logger which is also used by the PropertySchemaAnalyzer
	private static final Logger log = Logger.getLogger(
		de.interactive_instruments.xtf.wfs.PropertySchemaAnalyzer.class);

	/*
	 * XSD types that will be mapped to the internal GEOMETRY type
	 */
	final static Set<String> GEOMETRY_TYPES = [
			'PointPropertyType',
			'CurvePropertyType',
			'SurfacePropertyType',
			'GeometryPropertyType',
			'MultiPointPropertyType',
			'MultiCurvePropertyType',
			'MultiSurfacePropertyType',
			'MultiGeometryPropertyType'] as Set<String>;


    /*
	 * XSD types that will be mapped to the internal LITERAL type
	 */
	final static Set<String> LITERAL_TYPES = [
			'anyURI',
			'ID',
            'string',
            'integer',
            'int',
            'long',
            'short',
            'decimal',
            'float',
            'double',
            'boolean',
            'byte',
            'QName',
            'base64Binary',
            'hexBinary',
            'unsignedInt',
            'unsignedShort',
            'unsignedByte'] as Set<String>;

    /*
	 * XSD types that will be mapped to the internal DATE type
	 */
    final static Set<String> DATE_TYPES = [
			'date',
            'time',
            'g',
            'dateTime',
            'duration' ] as Set<String>;

	/*
	 * Needed for serialization
	 */
	@SuppressWarnings("unused")
	private FeatureTypeProperty() {
		nsHolder=null;
	}

	/*
	 * Creates a FeatureTypeProperty with an empty path and
	 * associates the NamespaceHolder.
	 */
	public FeatureTypeProperty(final NamespaceHolder nsHolder) {
		visitedNodes=new TreeSet<String>();
		this.nsHolder=nsHolder;
		this.lastSegment="";
		this.xPathSegments=new ArrayList<String>();
		this.type=PropertyType.UNKNOWN;
	}

	/*
	 * Creates a copy of a Path object based on an element declaration.
	 * The Element is added as segment to the XPath and marked as visited
	 */
	private FeatureTypeProperty(FeatureTypeProperty path, XSElementDeclaration elementDecl)
		throws SchemaAnalysisException
	{
		this.nsHolder=path.nsHolder;
		copyPath(path);
		if(!elementDecl.getAbstract()) {
			if(visitedNodes==null) {
				visitedNodes=new TreeSet<String>();
			}
			if(elementDecl.getName()==null || elementDecl.getName().equals("null")) {
				throw new SchemaAnalysisException("Element name is null");
			}
			/*
			if(elementDecl.getEnclosingCTDefinition()!=null) {
				// Do not add an element with an locally scoped complex type definition.
				// A locally scoped complex type must be analyzed by the schema analyzer!
				throw new SchemaAnalysisException(
					"Element contains a locally scoped ComplexType definition");
			}
			*/

			xPathSegments.add(elementDecl.getNamespace()+":"+elementDecl.getName());

			setTypeFromDef(elementDecl.getTypeDefinition());

			log.debug("Adding element to XPath expression "+elementDecl.getName());
			addVisitedNode(elementDecl);
		}
	}

	/*
	 *  Creates a copy of a Path object based on a complex type definition
	 *  This will mark the segment as visited
	 */
	private FeatureTypeProperty(FeatureTypeProperty path, XSComplexTypeDefinition complexTypeDef)
		throws MaxDepthExceededException, SchemaAnalysisException
	{
		this.nsHolder=path.nsHolder;
		copyPath(path);
		if(!complexTypeDef.getAbstract()) {
			if(visitedNodes==null) {
				visitedNodes=new TreeSet<String>();
			}
		}

		log.debug("Adding complex type to XPath expression "+complexTypeDef.getName());
		addVisitedNode(complexTypeDef);
	}

	/*
	 *  Creates a copy of a Path object based on a attribute declaration
	 *  This will add the attribute as segment to the XPath
	 */
	private FeatureTypeProperty(FeatureTypeProperty path, XSAttributeDeclaration attribDecl)
		throws MaxDepthExceededException, SchemaAnalysisException
	{
		this.nsHolder=path.nsHolder;
		copyPath(path);
		if(attribDecl.getName()==null || attribDecl.getName().equals("null")) {
			throw new SchemaAnalysisException("Attribute name is null");
		}
		xPathSegments.add("@"+attribDecl.getNamespace()+":"+attribDecl.getName());

		setTypeFromDef(attribDecl.getTypeDefinition());

		log.debug("Adding attribute to XPath expression "+attribDecl.getName());
		addVisitedNode(attribDecl);
	}

	/*
	 * Mark a node as visited. Needed for cycle detection
	 */
	private void addVisitedNode(XSObject xsObj) {

		String xsObjName = xsObj.getName();

		if(xsObj.getName()==null) {
			log.debug("NULL");
			if(xsObj instanceof XSComplexTypeDecl) {
				// log.debug(" SimpleType: "+((XSComplexTypeDecl)xsObj).getSimpleType() );
				// log.debug(" TypeName: "+((XSComplexTypeDecl)xsObj).getTypeName() );
				xsObjName = ((XSComplexTypeDecl)xsObj).getTypeName();
			}
		}

		log.debug("Marking "+xsObjName+
			" [C:"+xsObj.getClass().getName()+
			" T:"+xsObj.getType()+"] as visited for cycle detection");

		log.debug("Path: "+this.getName());

		String xsObjNamespace = xsObj.getNamespace();
		if(xsObjNamespace==null) {
			xsObjNamespace="";
		}

		assert(this.lastSegment!=null);
		assert(xsObjName!=null);

		log.debug(this.lastSegment+"_"+xsObjNamespace+xsObjName);
		this.visitedNodes.add(this.lastSegment+"_"+xsObjNamespace+xsObjName);
		this.lastSegment=xsObjNamespace+":"+xsObjName;
	}

	/*
	 * Sets relevant attributes from a Path Object to this instance
	 */
	private void copyPath(FeatureTypeProperty path)
		throws MaxDepthExceededException
	{
		this.type=PropertyType.UNKNOWN;
		// this.xPathExp=path.xPathExp;
		this.xPathSegments=new ArrayList<String>();
		this.xPathSegments.addAll(path.xPathSegments);
		this.visitedNodes=path.visitedNodes;
		this.lastSegment=path.lastSegment;
		this.depth=path.depth+1;
		if(depth>=maxDepth) {
			throw new MaxDepthExceededException(maxDepth, path);
		}
	}

	/*
	 * Lookup types from predefined type mappings.
	 * Returns: true if the XSD type could be mapped to an internal type
	 */
	private boolean setTypeFromKnownTypes(def typeDefinition) {
		if(GEOMETRY_TYPES.contains(xsdType)) {
			type=PropertyType.GEOMETRY;
		}else if(DATE_TYPES.contains(xsdType)) {
			type=PropertyType.DATE;
		}else if(LITERAL_TYPES.contains(xsdType)) {
			type=PropertyType.LITERAL;
		}else if(xsdType.equals("ReferenceType")) {
			/*
			if(typeDefinition) {
				type=PropertyType.FEATURE_REFERENCE;
				definition.getNamespaceItem()
			}else{
			*/
				type=PropertyType.REFERENCE;
			// }
			xPathSegments.add("@xlink:href");
		}else{
			return false;
		}
		return true;
	}

	/*
	 * Determine the base type of a type definition.
	 * This will run up in the type inheritance tree and stop just one step before
	 * "anySimple" or "anyType".
	 */
	private XSTypeDefinition getTopSimpleBaseType(XSTypeDefinition type) {
		// Unknown type, run up to the base types
		XSTypeDefinition baseType = type.getBaseType();
		XSTypeDefinition simpleType = type;
		while(baseType!=null &&
				!baseType.getName().equals("anySimpleType") &&
				!baseType.getName().equals("anyType"))
		{
			simpleType=baseType;
			baseType=baseType.getBaseType();
		}
		return simpleType;
	}

	/*
	 * Set the internal PropertyType by analyzing the type definition
	 * of the model object.
	 * TODO: Supports union members of simple types rudimentary by selecting the
	 * first one...
	 */
	private void setTypeFromDef(final XSTypeDefinition definition) {
		log.debug("Setting type from definition "+definition.getName()+
			" [C: "+definition.getClass().getName()+
			" BT: "+definition.getBaseType()+"]");
		xsdType = definition.getName();
		if(xsdType!=null)
		{
			// Can we set the type from the definition?
			if(!setTypeFromKnownTypes(definition)) {
				// Try to determine the type from the base type.
				log.debug(" Trying to determine the type from the base type");
				XSTypeDefinition baseType = getTopSimpleBaseType(definition);
				xsdType=baseType.getName();
				if(!setTypeFromKnownTypes(baseType)) {
					// If it is a simple type definition, there might be union members
					log.debug(" Trying to determine the type from union members");
					if(baseType instanceof XSSimpleTypeDefinition) {
						XSObjectList unionMembers = ((XSSimpleTypeDefinition) baseType).getMemberTypes();
						if(unionMembers!=null && unionMembers.getLength()>0) {
							log.debug(" Analyzing union which has "+
								((XSSimpleTypeDefinition) baseType).getMemberTypes().getLength()+
								" members");
								log.info(" Selecting firtst member");
								baseType=getTopSimpleBaseType(
									((XSTypeDefinition) unionMembers.item(0)) );
								xsdType=baseType.getName();

								if(!setTypeFromKnownTypes(baseType)) {
									log.warn(" Union analysis failed");
								}
							/*
							for (int i = 0; i < unionMembers.getLength(); i++) {
								log.info("Member: "+unionMembers.item(i));
							}
							*/
						}
					}else{
						log.warn(" Unable to handle base type: "+baseType);
					}
				}
			}
		}else if(definition instanceof  XSSimpleTypeDecl) {
			// Check if it is a simple type with a restriction
			xsdType = definition.getBaseType().getName();
			// TODO: Save restriction pattern
			setTypeFromKnownTypes(definition);
		}

		if(xsdType==null || type==PropertyType.UNKNOWN) {
			log.warn(" Could not determine XSD Type defintion! Obj: "
				+definition);
		}else{
			log.debug(" Type of "+xsdType+" mapped to "+type);
		}
	}

	/*
	 * Returns a copy of this instance, without the last segment in the XPath
	 */
	public FeatureTypeProperty getCopyWithoutLastSegment()
		throws MaxDepthExceededException
	{
		FeatureTypeProperty copy = new FeatureTypeProperty(nsHolder);
		copy.copyPath(this);
		// Remove last segment (if it is not complex)
		final int cs = copy.xPathSegments.size();
		if(cs > 0 && copy.xPathSegments.get(cs-1).equals(this.lastSegment)) {
			copy.xPathSegments.remove(cs-1);
		}
		return copy;
	}

	/*
	 * Returns a Path for a GetFeature Filter Expression
	 */
	public String getName() {
		String xpath="";
		if(this.xPathSegments.size()>0) {
			xpath=this.xPathSegments.get(0);
		}
		for(int i=1; i<this.xPathSegments.size(); i++) {
			xpath+="/"+this.xPathSegments.get(i);
		}
		return xpath;
	}


	/*
	 * Returns the number of segments in the XPath expression
	 */
	public int getDepth() {
		return depth;
	}

	/*
	 * Creates a copy of this instance
	 */
	public FeatureTypeProperty createCopyAndAddSegment(XSComplexTypeDefinition complexTypeDef)
		throws MaxDepthExceededException, SchemaAnalysisException
	{
		return new FeatureTypeProperty(this, complexTypeDef);
	}

	/*
	 * Creates a copy of this instance
	 */
	public FeatureTypeProperty createCopyAndAddSegment(XSElementDeclaration elementDecl)
		throws MaxDepthExceededException, SchemaAnalysisException
	{
		return new FeatureTypeProperty(this, elementDecl);
	}

	/*
	 * Creates a copy of this instance
	 */
	public FeatureTypeProperty createCopyAndAddSegment(XSAttributeDeclaration attribDecl)
		throws MaxDepthExceededException, SchemaAnalysisException
	{
		return new FeatureTypeProperty(this, attribDecl);
	}

	/*
	 *  Check if the term is already visited.
	 *  TODO: only detects simple cycles
	 */
	public boolean willTermNotCauseCycle(XSTerm term) throws SchemaAnalysisException {
		final String nodeName = term.getName()+term.getNamespace();
		if(nodeName==null || nodeName.equals("nullnull")) {
			throw new SchemaAnalysisException("Unexpected null term");
		}
		boolean cycle =visitedNodes.contains(this.lastSegment+"_"+
				term.getNamespace()+term.getName());

		log.debug("Cycle["+cycle+"]: "+this.lastSegment+"_"+
				term.getNamespace()+term.getName());
		return !cycle;
	}

	/*
	 * Returns the type of this PropertyType which has been mapped from a XSD type
	 */
	public PropertyType getPropertyType() {
		return type;
	}

	/*
	 * Returns the associated NamespaceHolder.
	 */
	public NamespaceHolder getNamespaceHolder() {
		return this.nsHolder;
	}

	/*
	 * Returns false if the internal type could not determined from the schema.
	 * This may indicate a defect in the SchemaAnalyzer...
	 */
	public boolean isTypeAnalyzed() {
		return type!=PropertyType.UNKNOWN;
	}

	/*
	 * Replaces all namespaces with namespace prefixes in the xpath expressions.
	 * Must be run at the end of the analysis process.
	 * The prefixes are provided by the NamespaceHolder.
	 */
	public void resolvePrefixes() {
		for(Map.Entry<String, String> item : this.nsHolder.getEntrySet()) {
			for(int i=0; i<this.xPathSegments.size(); i++) {
				this.xPathSegments.set(i,
						this.xPathSegments.get(i).replaceAll(
								item.getKey()+":", item.getValue()+":"));
				log.debug(this.xPathSegments.get(i));
			}
		}
	}

	/*
	 * Returns a list of all found segments of the FeatureType Property XPath
	 */
	public List<String> getSegments() {
		List<String> segments = new ArrayList<String>();
		assert(xPathSegments.size()>0);
		String spanXpath=xPathSegments.get(0);
		for(int i=1; i<this.xPathSegments.size(); i++) {
			spanXpath+="/"+xPathSegments.get(i);
			segments.add(spanXpath);
		}
		return segments;
	}

	/*
	 * Set the Xpath expression in Transfer_Properties
	 */
	public void transferProperties() {
		ProjectHelper ph = new ProjectHelper();
		ph.setTransferProperty(
			"FT.PROPERTY.NAME",
			this.getName()
		);
	}

	public boolean isAttribute() {
		return this.lastSegment.contains("@");
	}
}
