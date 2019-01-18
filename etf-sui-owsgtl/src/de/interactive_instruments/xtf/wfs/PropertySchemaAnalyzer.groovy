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

import de.interactive_instruments.xtf.OutputFormat
import de.interactive_instruments.xtf.exceptions.MaxDepthExceededException
import de.interactive_instruments.xtf.exceptions.NamespaceHolderException
import de.interactive_instruments.xtf.exceptions.SchemaAnalysisException
import org.apache.log4j.Logger
import org.apache.xerces.impl.xs.XSWildcardDecl
import org.apache.xerces.xs.*
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import org.w3c.dom.ls.LSInput

import javax.xml.namespace.QName

public class PropertySchemaAnalyzer {

	private final XSModel model;
	private final Logger log;
	private List<FeatureTypeProperty> ftPropertyExps;
	private OutputFormat outputFormat;

	final static Set<String> BLACKLISTED_ATTRIBUTES = ['type'] as Set<String>;

	/*
	 * Create a SchemaAnalyzer for a specified schema xml and the output format.
	 * External schema definitions that are imported in the passed schema xml string,
	 * are loaded automatically by the SchemaAnalyzer.
	 * The SchemaAnalyzer builds property paths and returns a list of FeatureTypeProperty 
	 * objects by invoking the method analyze(FeatureType).
	 * 
	 * Every step of the analysis process might be logged by setting the log level to debug. 
	 * Please note that the debug output might be over one GB for large schema definitions!
	 */
	public PropertySchemaAnalyzer(String schemaXml, OutputFormat outputFormat) throws
		ClassCastException, ClassNotFoundException,
		InstantiationException, IllegalAccessException,
		SchemaAnalysisException, IOException
	{
		this.log = Logger.getLogger(this.getClass());
		
		this.outputFormat = outputFormat;
		System.setProperty(DOMImplementationRegistry.PROPERTY,
			"org.apache.xerces.dom.DOMXSImplementationSourceImpl");
		DOMImplementationRegistry registry =
			DOMImplementationRegistry.newInstance();
						
		Reader r = new StringReader(schemaXml);
		DOMImplementationLS lsImpl = 
			(DOMImplementationLS) DOMImplementationRegistry.newInstance().
			getDOMImplementation("LS");
		LSInput lsIn = lsImpl.createLSInput();
		lsIn.setCharacterStream(r);

		XSImplementation xsImpl =
		(XSImplementation) registry.getDOMImplementation("XS-Loader");
		XSLoader schemaLoader = xsImpl.createXSLoader(null);
		
		this.model = schemaLoader.load(lsIn);
						
		if(this.model==null) {
			throw new SchemaAnalysisException(this, 
					"Unable to load schema document");
		}
		
		/*
		this.log.setLevel(Level.DEBUG);
		// this.log.addAppender(new ConsoleAppender(new SimpleLayout()));
	    FileAppender fileAppender = new FileAppender( new SimpleLayout(),
	    		"C:/PropertyAnalyzer.txt", false );
	    this.log.addAppender(fileAppender);
		*/
		
		this.log.debug(this.getClass().getName()+" initialized with \""+
	    		outputFormat.getFormat()+"\"");	   

	}
	
	public OutputFormat getOutputFormat() {
		return this.outputFormat;
	}

		
	/*
	 * Analyze the schema model and return a list of FeatureTypeProperty objects 
	 * for the requested FeatureType.  
	 */
	public List<FeatureTypeProperty> analyze(QName featureType) 
		throws SchemaAnalysisException, NamespaceHolderException
	{	
		this.ftPropertyExps = new ArrayList<FeatureTypeProperty>();
		// Select Element with FeatureType name and begin to analyze
		XSElementDeclaration featureTypeElement = 
			model.getElementDeclaration(featureType.getLocalPart(), 
					featureType.getNamespaceURI());
		if(featureTypeElement==null) {
			throw new SchemaAnalysisException(this, 
					"FeatureType "+featureType.getNamespaceURI()+
					":"+featureType.getLocalPart()+" not found in Schema document!" );
		}
		if(featureTypeElement.getAbstract()) {
			throw new SchemaAnalysisException(this, 
					"FeatureType "+featureType.getLocalPart()+" is declared abstract!" );
		}
		log.debug("Analyzing FeatureType "+featureType.getLocalPart());
		
		final NamespaceHolder nsHolder = new NamespaceHolder(model.getNamespaces());
		analyzeElement(featureTypeElement, new FeatureTypeProperty(nsHolder));
		
		int i=0;
		log.debug("Found the following FeatureType Property Expressions for FeatureType "+
			featureType.getLocalPart()+":");
		for(FeatureTypeProperty path : ftPropertyExps) {
			log.debug(" "+(++i)+". "+path.getName()+" ["+path.getPropertyType()+"]");
		}
							
		return ftPropertyExps;
	}
	
	/*
	 * Check if namespace is blacklisted.
	 * Used to skip complex type definitions which should not be analyzed in depth. 
	 */
	private boolean isBlacklisted(String typeNamespace) {
		// if(!typeNamespace.contains("gml") && !typeNamespace.contains("opengis") ) {
		if(
			// Allow citygml, needed by LEARM3D
			(typeNamespace.contains("opengis") && typeNamespace.contains("citygml")) || 
			// Skip gml
			!typeNamespace.contains("opengis") )
		{
			return false;
		}
		log.debug("Skipping blacklisted namespace "+typeNamespace);
		return true;
	}
			
	/*
	 * Analyze an element.
	 * If its type is simple it can just be added as property path.
	 * Complex types and substitution groups are analyzed recursive. 
	 */
	private void analyzeElement(XSElementDeclaration elementDecl, FeatureTypeProperty path)
		throws MaxDepthExceededException, SchemaAnalysisException
	{
		log.debug("Analyzing Element "+elementDecl);
		
		XSTypeDefinition typeDef = elementDecl.getTypeDefinition();
		
		if(typeDef==null) {
			throw new SchemaAnalysisException(this, 
				"Unable to lookup type definition for element "+elementDecl.getName());
		}
		
		if(elementDecl.getAbstract()) {
			log.debug(" The element is declared abstract. Will only analyze substitution groups");
		}
				
		// Substitution group analysis
		XSObjectList substGroup = model.getSubstitutionGroup(elementDecl);
		if(substGroup!=null && substGroup.getLength()>0) {
			log.debug(" Analyzing "+substGroup.getLength()+" substitution groups of element "+elementDecl+" ");
			for (int i = 0; i < substGroup.getLength(); i++) {
				if(substGroup.item(i) instanceof XSElementDeclaration) {
					// Create a copy of the path without the last segment. This will
					// be overwritten by elements of the substitution group
					FeatureTypeProperty substPath = path.getCopyWithoutLastSegment()
						.createCopyAndAddSegment((XSElementDeclaration)substGroup.item(i));
					analyzeElement((XSElementDeclaration) substGroup.item(i), substPath);
				}
			}
		}
		
		// Do not build paths if the element is declared abstract
		if(!elementDecl.getAbstract()) {
			// Complex type analysis
			if(typeDef instanceof XSComplexTypeDefinition) {
				log.debug(" Analyzing complex type definitions...");
								
				// Check if the base type is completely analyzed
				XSParticle particle = (XSParticle)((XSComplexTypeDefinition) typeDef).getParticle();
				if(path.isTypeAnalyzed() || particle==null) {
					// Needs no further analysis here
					analyzeAttributesAndAdd(path,elementDecl);
				}else{
					// Needs a deeper analysis of the type definition
					analyzeComplexTypeDef((XSComplexTypeDefinition) typeDef,
							path.createCopyAndAddSegment((XSComplexTypeDefinition)typeDef));
				}
			}else if(typeDef instanceof XSSimpleTypeDefinition) {
				analyzeAttributesAndAdd(path,elementDecl);
			}
		}
		
	}
	
	/*
	 * Analyze a complex type definition.
	 */
	private void analyzeComplexTypeDef(XSComplexTypeDefinition complexTypeDef, FeatureTypeProperty path)
		throws SchemaAnalysisException
	{
		assert(complexTypeDef!=null);
		log.debug("Analyzing ComplexType definition: "+complexTypeDef);
		if(isBlacklisted(complexTypeDef.getNamespace())) {
			// Skip analysis of complex type definition. Used for gml.
			return;
		}
				
		XSParticle particle = (XSParticle)complexTypeDef.getParticle();
		if(particle!=null) {
			XSModelGroup modelGroup = (XSModelGroup) particle.getTerm();
			analyzeModelGroup(modelGroup, path);
		}else if(complexTypeDef.getSimpleType()!=null) {
			log.debug("Analyzing simpleType "+complexTypeDef.getSimpleType());
			log.debug("Test: "+path.createCopyAndAddSegment(complexTypeDef).getName());
		}else{
			// Ignore ComplexType definition with base types such as string, doubleList, ...
			log.debug("Ignoring ComplexType definition with standard base type"+complexTypeDef);
		}
	}
		
	/*
	 * Analyze a model group.
	 */
	private void analyzeModelGroup(XSModelGroup modelGroup, FeatureTypeProperty path)
		throws SchemaAnalysisException
	{
		log.debug("Analyzing modelGroup: "+modelGroup);
		log.debug("Current path: "+path.getName()+" depth "+path.getDepth());
		
		XSObjectList particles = modelGroup.getParticles();
		
		for (int i = 0; i < particles.getLength(); ++i) {
			XSParticle particle = (XSParticle)particles.item(i);
		    XSTerm     term     = particle.getTerm();
			if (term instanceof XSElementDeclaration) {
	    		XSElementDeclaration elementDecl = (XSElementDeclaration)term;
	    		log.debug("Particle is element: "+elementDecl);
	    		if(path.willTermNotCauseCycle( term)) {
					analyzeElement(elementDecl, path.createCopyAndAddSegment(
						elementDecl));
					
	    		}else{
	    			log.debug("Skipping Element "+elementDecl.getName()+" due to cycle causation!");
	    		}
	    	}else if(term instanceof XSModelGroup) {
	    		XSModelGroup subModelGroup = (XSModelGroup) term;
	    		log.debug("Particle is modelGroup: "+subModelGroup);
	    		analyzeModelGroup((XSModelGroup) subModelGroup, path);
	    	}else if(term instanceof XSWildcardDecl){
	    		// Nothing todo. Wildcards are not supported
	    	}else{
	    		throw new SchemaAnalysisException(this, "Unknown: "+term.getClass());
	    	}
		}
	}
	
	/*
	 * Analyze the attributes of an element and add the element to the list of 
	 * FeatureTypeProperty objects
	 */
	private void analyzeAttributesAndAdd(FeatureTypeProperty path, XSElementDeclaration element) 
		throws MaxDepthExceededException, SchemaAnalysisException
	{
		XSTypeDefinition typeDef = element.getTypeDefinition();
		if(typeDef instanceof XSComplexTypeDefinition) {
			XSObjectList attribGroup = ((XSComplexTypeDefinition) typeDef).getAttributeUses();
			// Do not analyze attributes of a reference element
			if(attribGroup!=null && attribGroup.getLength()>0  && 
					path.getPropertyType()!=PropertyType.REFERENCE )
			{
				log.debug("Analyzing Attributes of element.");
				for(int i=0; i<=attribGroup.getLength(); i++) {
					XSAttributeUse attribute = (XSAttributeUse) attribGroup.item(i);
					if(attribute!=null && attribute.getAttrDeclaration()!=null &&
						attribute.getAttrDeclaration().getNamespace()!=null &&
						!BLACKLISTED_ATTRIBUTES.contains(attribute.getAttrDeclaration().getName()))
					{
						// Some items in the array are null!?
						// Ignore blacklisted attributes and attributes without a namespace decl   
						log.debug("Adding attribute "+attribute.getAttrDeclaration().getName());
						ftPropertyExps.add(path.createCopyAndAddSegment(
							attribute.getAttrDeclaration()));	
					}
				}
			}
			log.debug("Adding ComplexType Element : "+path.getName());
			analyzeComplexTypeDef((XSComplexTypeDefinition) typeDef, path);
		}else if(typeDef instanceof XSSimpleTypeDefinition) {
			// log.debug("Adding SimpleType Element : "+path.getName());
		}
		if(path.getName()==null || path.getName().trim().equals("")) {
			throw new SchemaAnalysisException(this, 
				"Property XPath is empty for element: "+element);
		}
		ftPropertyExps.add(path);
	}	
}
