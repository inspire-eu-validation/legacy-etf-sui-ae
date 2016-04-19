/**
 * This package maps the SoapUI results to the QAF model
 * 
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */

@XmlSchema(  
	xmlns = {   
		@XmlNs(namespaceURI = "http://www.interactive-instruments.de/etf/1.0", prefix = "etf")
	},
	namespace = "http://www.interactive-instruments.de/etf/1.0",
	elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED
)

package de.interactive_instruments.etf.sel.model.impl.result;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;

