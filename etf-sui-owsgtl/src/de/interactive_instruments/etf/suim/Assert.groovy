/*
 * Copyright ${year} interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.interactive_instruments.etf.suim

import com.eviware.soapui.model.iface.MessageExchange
import com.eviware.soapui.model.testsuite.TestRunContext
import com.eviware.soapui.support.XmlHolder
import de.interactive_instruments.SUtils
import de.interactive_instruments.XmlUtils
import de.interactive_instruments.etf.model.exceptions.TestStepNotFoundException
import de.interactive_instruments.exceptions.EmptyContentParseException
import de.interactive_instruments.exceptions.XmlParseException
import de.interactive_instruments.xtf.ProjectHelper
import de.interactive_instruments.xtf.SOAPUI_I
import org.apache.xmlbeans.XmlException

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
class Assert {

    private final XmlHolder xml
    private logger
    private TestRunContext context
    private MessageExchange messageExchange

    public static INSPIRE_DS_NS = [
            'atom':'http://www.w3.org/2005/Atom',
            'fes':'http://www.opengis.net/fes/2.0',
            'gmd':'http://www.isotc211.org/2005/gmd',
            'gml':'http://www.opengis.net/gml/3.2',
            'georss':'http://www.georss.org/georss',
            'inspire_common':'http://inspire.ec.europa.eu/schemas/common/1.0',
            'inspire_dls':'http://inspire.ec.europa.eu/schemas/inspire_dls/1.0',
            'ows':'http://www.opengis.net/ows/1.1',
            'os':'http://a9.com/-/spec/opensearch/1.1/',
            'wfs':'http://www.opengis.net/wfs/2.0',
            'srv':'http://www.isotc211.org/2005/srv',
            'xlink':'http://www.w3.org/1999/xlink',
            'xml':'http://www.w3.org/XML/1998/namespace'
    ]

    private static containsException(final XmlHolder xml) {
        return xml.getNodeValue("exists(/*:ServiceExceptionReport) or exists(/*:ExceptionReport) or exists(/*:Exception)") == "true"
    }

    Assert(final MessageExchange messageExchange, final TestRunContext context, logger, final String...namespaces=null) {
        this(messageExchange, context, logger, SUtils.toStrMap(namespaces))
    }

    Assert(final MessageExchange messageExchange, final TestRunContext context, logger, final Map<String, String> namespaces) {
        this.logger=logger
        this.context=context
        this.messageExchange=messageExchange
        if(!messageExchange.hasResponse()) {
            err("No data returned")
            throw new TranslatableAssertionError("TR.noDataReturned")
        }
        try {
            if(messageExchange.responseContentAsXml==null) {
                throw new TranslatableAssertionError("TR.xmlExpected")
            }
            this.xml = new XmlHolder(messageExchange.responseContentAsXml)
            if(this.xml==null || this.xml.getDomNode("/")==null) {
                throw new TranslatableAssertionError("TR.xmlExpected")
            }
        }catch (XmlException e) {
            throw new TranslatableAssertionError("TR.xmlExpected")
        }
        if(containsException(this.xml)) {
            final String exceptionText = SUtils.concatStr("; ", this.xml.getNodeValues("//*:ExceptionText"))
            if(SUtils.isNullOrEmpty(exceptionText)) {
                throw new TranslatableAssertionError("TR.unexpectedException", "text", "unknown error")
            }else{
                throw new TranslatableAssertionError("TR.unexpectedException", "text", exceptionText)
            }
        }
        if(this.xml.getDomNode("/html")!=null) {
            throw new TranslatableAssertionError("TR.unexpectedHtml")
        }

        if(namespaces!=null) {
            namespaces.entrySet().each{ it -> this.xml.declareNamespace(it.key, it.value) }
        }
    }

    private err(final String message) {
        if(logger!=null) {
            logger.error(message)
        }
    }

    void isTrue(final String path, final String translationTemplate, final String... translations) {
        final String result = this.xml.getNodeValue(path)
        if(result == null || result == "false") {
            err("Expression '"+path+"' evaluated unexpectedly to 'false'")
            throw new TranslatableAssertionError(translationTemplate, translations)
        }
    }

    void isFalse(final String path, final String translationTemplate, final String... translations) {
        final String result = this.xml.getNodeValue(path)
        if(result != null &&  result != "false") {
            err("Expression '"+path+"' evaluated unexpectedly to 'true'")
            throw new TranslatableAssertionError(translationTemplate, translations)
        }
    }

    void notExists(final String path, final String translationTemplate=null) {
        org.w3c.dom.Node[] nodes = this.xml.getDomNodes(path)
        if(nodes!=null && nodes.length>0) {
            if(nodes.length==1) {
                final String elementName = nodes[0].getNodeName()
                err("'"+elementName+"' with ("+path+") was not expected in the response")
                final String value = XmlUtils.nodeValue(nodes[0])
                if(SUtils.isNullOrEmpty(value)) {
                    throw new TranslatableAssertionError(translationTemplate==null ? "TR.elementNotExpected" : translationTemplate, "xpath", path, "elementName", elementName)
                }else{
                    throw new TranslatableAssertionError(translationTemplate==null ? "TR.elementWithValueNotExpected" : translationTemplate, "xpath", path, "elementName", elementName, "value", value)
                }
            }else{
                String elementNames=""
                for (int i = 0; i < nodes.length ;) {
                    elementNames+=(nodes[i++].getNodeName())
                    if(i < nodes.length) {
                        elementNames+=", "
                    }
                }
                throw new TranslatableAssertionError(translationTemplate==null ? "TR.elementsNotExpected" : translationTemplate, "xpath", path, "elementNames", elementNames)
            }
        }
    }

    private String getNodeValue(final String path, final String elementName = XmlUtils.getLastXpathSegment(path,false)) {
        def node = this.xml.getDomNode(path)
        if(node==null) {
            throw new TranslatableAssertionError(
                    "TR.missingElement", "element", elementName, "xpath", path)
        }
        return XmlUtils.nodeValue(node)
    }

    def exists(final String path, final String translationTemplate="TR.missingElement") {
        def node = this.xml.getDomNode(path)
        if(node==null) {
            final String elementName=XmlUtils.getLastXpathSegment(path,false)
            err("The expected element '"+elementName+"' ("+path+") was not found in the response")
            throw new TranslatableAssertionError(translationTemplate, "xpath", path, "element", elementName)
        }
        return node
    }

    String existsNonEmptyText(final String path, String elementName=XmlUtils.getLastXpathSegment(path,false), final String translationTemplate="TR.emptyValue") {
        final String nodeValue = getNodeValue(path, elementName)
        if(SUtils.isNullOrEmpty(nodeValue)) {
            err("Element '"+path+"' has empty value")
            throw new TranslatableAssertionError(translationTemplate, "xpath", path, "element", elementName)
        }
        return nodeValue
    }

    String existsNonEmptyTextMatching(final String path, final String regex, final String translationTemplate="TR.doesNotMatchRegex") {
        final String nodeValue = getNodeValue(path)
        if(SUtils.isNullOrEmpty(nodeValue)) {
            err("Element '"+path+"' has empty value")
            throw new TranslatableAssertionError(translationTemplate, "xpath", path)
        }else if(!nodeValue.matches(regex)) {
            err("Element '"+path+"' does not match regular expression '"+regex+"'")
            throw new TranslatableAssertionError(translationTemplate, "xpath", path)
        }
        return nodeValue
    }

    void equals(final String path, final String elementName, final String expectedValue, final String translationTemplate="TR.invalidValue") {
        final String nodeValue = existsNonEmptyText(path)
        if(nodeValue != expectedValue) {
            err("'"+path+"': "+nodeValue+" does not equal "+expectedValue)
            throw new TranslatableAssertionError(translationTemplate, "xpath", path, "element", elementName, "expected", expectedValue, "actual", nodeValue)
        }
    }

    void equalPathValues(final String path, final String expectedPath, final String translationTemplate="TR.invalidValue") {
        final String val = existsNonEmptyText(path)
        final String expectedVal = existsNonEmptyText(expectedPath)
        if(val != expectedVal) {
            def node = this.xml.getDomNode(path)
            final String elementName = node.getNodeName()
            err("'"+val+"' does not equal '"+expectedVal+"'")
            throw new TranslatableAssertionError(translationTemplate, "xpath", path, "element", elementName, "expected", expectedVal, "actual", val)
        }
    }

    void equalPathValuesInStep(final String path, final String testStepNameForExpectedVal, final String pathToExpectedVal=path, final String translationTemplate="TR.invalidValue") {
        final String val = existsNonEmptyText(path)

        SOAPUI_I.init(this.logger, this.context, this.messageExchange)

        final ProjectHelper ph = new ProjectHelper()
        def response
        try {
            response = ph.getTestStepResponseAsXML(testStepNameForExpectedVal)
            if(containsException(response)) {
                throw new TranslatableAssertionError("TR.preCondition.testStep.unexpectedException")
            }
        }catch(final TestStepNotFoundException e) {
            throw new TranslatableAssertionError("TR.internalError", "text", e.getMessage())
        }catch(final XmlParseException e) {
            throw new TranslatableAssertionError("TR.preCondition.testStep.xmlExpected")
        }catch(final EmptyContentParseException e) {
            throw new TranslatableAssertionError("TR.preCondition.testStep.noDataReturned")
        }

        final String expectedVal = response.getNodeValue(pathToExpectedVal)
        if(SUtils.isNullOrEmpty(expectedVal)) {
            throw new TranslatableAssertionError("TR.preCondition.testStep.missingElement")
        }
        if(val != expectedVal) {
            def node = this.xml.getDomNode(path)
            final String elementName = node.getNodeName()
            err("'"+val+"' does not equal '"+expectedVal+"'")
            throw new TranslatableAssertionError(translationTemplate, "xpath", path, "element", elementName, "expected", expectedVal, "actual", val)
        }
    }

    void equalsResponseFromTestStep(final String testStep) {

        SOAPUI_I.init(this.logger, this.context, this.messageExchange)

        final ProjectHelper ph = new ProjectHelper()
        def response
        try {
            response = ph.getTestStepResponseAsXML(testStep)
            if(containsException(response)) {
                throw new TranslatableAssertionError("TR.preCondition.testStep.unexpectedException")
            }
        }catch(final TestStepNotFoundException e) {
            throw new TranslatableAssertionError("TR.internalError", "text", e.getMessage())
        }catch(final XmlParseException e) {
            throw new TranslatableAssertionError("TR.preCondition.testStep.xmlExpected")
        }catch(final EmptyContentParseException e) {
            throw new TranslatableAssertionError("TR.preCondition.testStep.noDataReturned")
        }

        response
    }
}
