/**
 * This package contains a model that mirrors the model which is used in SoapUI and maps it to the QAF model
 * 
 * @author herrmann
 *
 */

@XmlSchema(  
	xmlns = {   
		@XmlNs(namespaceURI = "http://www.interactive-instruments.de/etf/1.0", prefix = "etf")
	},
	namespace = "http://www.interactive-instruments.de/etf/1.0",
	elementFormDefault = javax.xml.bind.annotation.XmlNsForm.QUALIFIED
)

package de.interactive_instruments.etf.sel.model.impl.spec;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlSchema;

