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

import com.eviware.soapui.impl.rest.RestRequestInterface.HttpMethod
import com.eviware.soapui.impl.support.http.HttpRequestTestStep
import com.eviware.soapui.impl.wsdl.testcase.WsdlTestCase
import com.eviware.soapui.impl.wsdl.teststeps.HttpTestRequestStep
import com.eviware.soapui.impl.wsdl.teststeps.RestTestRequestStep
import com.eviware.soapui.impl.wsdl.teststeps.WsdlTestStep
import com.eviware.soapui.model.iface.MessageExchange
import com.eviware.soapui.model.propertyexpansion.PropertyExpander
import com.eviware.soapui.support.types.StringToStringMap
import com.eviware.soapui.support.types.StringToStringsMap
import de.interactive_instruments.UriUtils
import de.interactive_instruments.xtf.exceptions.InvalidProjectParameterException

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class Util {


    /**
     * Set basic authentication headers for all HTTP test requests or just append the
     * username and password to all requests, depending on the authentication method.
     * For all requests with variable endpoints the authentication is set!
     * Requires that the project variables authUser, authPwd and authMethod are set.
     * Possible values for xtf.authMethod: appendCredentials, httpBasic
     */
    public static void updateCredentials(def testRunner) {

        final String authUser = testRunner.testCase.testSuite.project.getPropertyValue('authUser');
        final String authPwd = testRunner.testCase.testSuite.project.getPropertyValue('authPwd');
        final String authMethod = testRunner.testCase.testSuite.project.getPropertyValue('authMethod');
		final String authServiceEndpoint = testRunner.testCase.testSuite.project.getPropertyValue('serviceEndpoint');
        assert authServiceEndpoint != null

        for( testSuite in testRunner.testCase.testSuite.project.getTestSuiteList() ) {
            for( testCase in testSuite.getTestCaseList() ) {
                for( testStep in testCase.getTestStepList() ) {
                    if( testStep instanceof HttpTestRequestStep || testStep instanceof RestTestRequestStep) {
                        removeAuthorization(testStep);
                        final String testStepEndpoint = testStep.getHttpRequest().getEndpoint();
                        if (testStepEndpoint != null && (testStepEndpoint.startsWith('${') || domainAndPathEqual(authServiceEndpoint, testStepEndpoint))) {
                            updateHttpTestStepCredentials(testStep, authUser, authPwd, authMethod)
                        }
                    }
                }
            }
        }
    }

	/**
	 * Set basic authentication headers for one single HTTP test requests or just append the
	 * username and password to the request, depending on the authentication method.
     * Endpoint variables are expanded and checked!
	 * Requires that the project variables authUser, authPwd and authMethod are set.
	 * Possible values for xtf.authMethod: appendCredentials, httpBasic
	 */
	public static void updateTestStepCredentials(
            HttpRequestTestStep testStep,
            def context,
            String authUser = testStep.testCase.testSuite.project.getPropertyValue('authUser'),
            String authPwd = testStep.testCase.testSuite.project.getPropertyValue('authPwd'),
            String authMethod = testStep.testCase.testSuite.project.getPropertyValue('authMethod'),
            String authServiceEndpoint = testStep.testCase.testSuite.project.getPropertyValue('serviceEndpoint')
    ) {
		assert authServiceEndpoint != null

		if( testStep instanceof HttpTestRequestStep || testStep instanceof RestTestRequestStep) {

            def propertyExpander = new PropertyExpander(true);
            final String testStepEndpoint = propertyExpander.expandProperties( context,
                    testStep.getHttpRequest().getEndpoint())
            if(testStepEndpoint != null && domainAndPathEqual(authServiceEndpoint, testStepEndpoint)) {
                updateHttpTestStepCredentials(testStep, authUser, authPwd, authMethod);
            }else{
                removeAuthorization(testStep);
            }
		}
	}

    private static void updateHttpTestStepCredentials(
            HttpRequestTestStep testStep,
            String authUser = testStep.testCase.testSuite.project.getPropertyValue('authUser'),
            String authPwd = testStep.testCase.testSuite.project.getPropertyValue('authPwd'),
            String authMethod = testStep.testCase.testSuite.project.getPropertyValue('authMethod')
    ) {
        // Append Credentials
        if (authMethod != null && authMethod.equals("appendCredentials")) {
            if (testStep.getHttpRequest().getMethod() == HttpMethod.POST) {
                // Add username and password to the root element
                String requestBody = testStep.getProperty("Request").value;
                if (requestBody != null && !requestBody.trim().equals("")) {
                    // Xml Parser is useless here, because request may contain properties
                    // which do not validate
                    int tagStart = requestBody.indexOf("<");
                    int tagEnd = requestBody.lastIndexOf(">");
                    String rootElement = requestBody.substring(tagStart, tagEnd);
                    int freePos = rootElement.indexOf(" ");

                    int authUserPos = rootElement.indexOf(" username=");
                    String usernameAttrib = " username=\"" + authUser + "\"";
                    if (authUserPos == -1) {
                        rootElement = rootElement.substring(0, freePos) +
                                usernameAttrib +
                                rootElement.substring(freePos, rootElement.length());
                    } else {
                        int usernameEndPos = rootElement.indexOf(" ", authUserPos + 7);
                        rootElement = rootElement.substring(0, authUserPos) +
                                usernameAttrib + rootElement.substring(
                                usernameEndPos, rootElement.length());
                    }

                    String pwdAttrib = " password=\"" + authPwd + "\"";
                    int authPwdBegPos = rootElement.indexOf(" password=");
                    if (authPwdBegPos == -1) {
                        rootElement = rootElement.substring(0, freePos) +
                                pwdAttrib +
                                rootElement.substring(freePos, rootElement.length());
                    } else {
                        int pwdEndPos = rootElement.indexOf(" ", authPwdBegPos + 7);
                        rootElement = rootElement.substring(0, authPwdBegPos) +
                                pwdAttrib + rootElement.substring(
                                pwdEndPos, rootElement.length());
                    }
                    testStep.getHttpRequest().setRequestContent(
                            rootElement + requestBody.substring(tagEnd, requestBody.length()));
                }
            } else {
                // Just set the GET-Parameters
                testStep.getHttpRequest().setPropertyValue("username", authUser);
                testStep.getHttpRequest().setPropertyValue("password", authPwd);
            }
        } else if (authMethod != null && authMethod.equals("basic")
                && authUser != null && authUser.size() > 0
                && authPwd != null && authPwd.size() > 0) {
            // Set Authorization header
            def headers = new StringToStringsMap();
            def auth = authUser + ':' + authPwd;
            def encodedAuth = auth.bytes.encodeBase64().toString();
            headers.put('Authorization', 'Basic ' + encodedAuth);
            testStep.getHttpRequest().setRequestHeaders(headers);
        }

    }

    public static void removeAuthorization(def testStep) {
        def headers = testStep.getHttpRequest().getRequestHeaders();
        headers.remove('Authorization');
        testStep.getHttpRequest().setRequestHeaders(headers);

        if(testStep.getHttpRequest().hasProperty("username")) {
            testStep.getHttpRequest().removeProperty("username");
        }
        if(testStep.getHttpRequest().hasProperty("password")) {
            testStep.getHttpRequest().removeProperty("password");
        }
    }

    public static boolean domainAndPathEqual(final String url1, final String url2) {
        final String domain1=getDomainName(url1)
        final String path1=UriUtils.encodedUri(url1).getPath();
        final String path2=UriUtils.encodedUri(url2).getPath();
        return domain1!=null && getDomainName(url1)?.equalsIgnoreCase(getDomainName(url2)) &&
                path1!=null && path1.equalsIgnoreCase(path2)
    }

    public static String getDomainName(final String url) throws URISyntaxException {
        final String domain = UriUtils.encodedUri(url).getHost();
        return (domain!=null && domain.startsWith("www.")) ? domain.substring(4) : domain;
    }

	public static void setProjectProperty(def testRunner, String name, String value) {
		testRunner.getTestCase().getTestSuite().getProject().setPropertyValue(name, value);
	}

	public static String getPropertyValueOrDefault(def modelItem, String name, def defaultValue) {
		def value = modelItem.getPropertyValue(name);
		if(value!=null && !value.trim().equals("")) {
			return value;
		}
		return defaultValue;
	}

	public static String getProjectPropertyOrNull(String propertyName,
		def testRunner=SOAPUI_I.getInstance().getTestRunner())
	{
		final def proj = testRunner.getTestCase().getTestSuite().getProject();
		final String property = getPropertyValueOrDefault(proj, propertyName, null);
		return property;
	}

	public static String getProjectProperty(String propertyName,
		def testRunner=SOAPUI_I.getInstance().getTestRunner())
	{
		final String property = getProjectPropertyOrNull(propertyName, testRunner);
		if(property==null) {
			throw new InvalidProjectParameterException(this,
				"Project property \""+propertyName+"\" is not set!");
		}
		return property;
	}

	public static void assertXML_Encoding(def messageExchange, String encoding) {
		String resp = new String(messageExchange.getRawResponseData());
		def index = (resp.indexOf("<?xml version=\"1.0\" encoding=\""+encoding.toUpperCase()+"\"?>")) +
			(resp.indexOf("<?xml version=\"1.0\" encoding=\""+encoding.toLowerCase()+"\"?>")) + 1;
		if(index<53)
			throw new Exception("Response encoding does not match "+encoding);
	}

	public static String getResponseHeader(def testStep, String headerName) {
		try {
			return testStep.getTestRequest().getResponse().getResponseHeaders().get(headerName,"");
		}catch(Exception e) {
			return "";
		}
	}

	public static String cutString(final String str, int maxSize=500) {
		if( str == null) {
			return "!!! N U L L !!!";
		}
		if(str.size()>maxSize) {
			return str.substring(0,maxSize)+"[...cutted...]";
		}else{
			return str;
		}
	}

	public static boolean isImageWhite(BufferedImage bufImage) {
		int[] pxl = getNonWhitePixelFromImg(bufImage);
		if(pxl[0]==-1 && pxl[1]==-1) {
			return true;
		}
		return false;
	}

	// Returns the position of the first pixel which is non-white
	public static int[] getNonWhitePixelFromImg(BufferedImage bufImage) {
		int[] pxl = new int[2];
		pxl[0]=-1;
		pxl[1]=-1;
		for(int w=0; w < bufImage.getWidth(); w++) {
			for(int h=0; h < bufImage.getHeight(); h++) {
				final int pixel = bufImage.getRGB(w, h);
				final int alpha = (pixel >> 24) & 0xff;
				final int red = (pixel >> 16) & 0xff;
				final int green = (pixel >> 8) & 0xff;
				final int blue = (pixel) & 0xff;
				if(alpha>0 && (red<255 && green<255 && blue<255)) {
					pxl[0]=w+1;
					pxl[1]=h+1;
					/*
					SOAPUI_I.getInstance().getLog().info(
						"Alpha: "+alpha+
						"Red: "+red+
						"Green: "+green+
						"Blue: "+blue);
					*/
					return pxl;
				}
			}
		}
		// Pixel not found, image is white
		return pxl;
	}

	public static BufferedImage createImage(MessageExchange messageExchange, def log) {
		URL imageUrl = new URL(messageExchange.getEndpoint());
		BufferedImage image = ImageIO.read( imageUrl );
		if(image==null) {
			throw new Exception("Image is invalid!");
		}

		// Split key value pairs
		StringToStringMap keyValMap = new StringToStringMap();
		for(pair in imageUrl.getQuery().split("&")) {
			String[] tok = pair.split("=");
			keyValMap.put(tok[0], tok[1]);
		}

		int height = Integer.valueOf(keyValMap.get("HEIGHT", 0));
		int width = Integer.valueOf(keyValMap.get("WIDTH", 0));

		if(height!=image.getHeight() || width!=image.getWidth()) {
			throw new Exception("Requested image with WIDTH x HEIGHT "+
				width+"x"+ height+
				" does not match received image "+
				image.getWidth()+"x"+image.getHeight());
		}
		return image;
	}

	public static List<WsdlTestCase> getAllTestTestCases(def testRunner) {
		List<WsdlTestCase> testCasesList = new ArrayList<WsdlTestCase>();
		for(testSuite in testRunner.testCase.testSuite.project.getTestSuiteList()) {
			for(testCase in testSuite.getTestCaseList()) {
				testCasesList.add(testCase);
			}
		}
		return testCasesList;
	}

	public static List<WsdlTestStep> getAllTestSteps(def testRunner) {
		List<WsdlTestStep> testStepList = new ArrayList<WsdlTestStep>();
		for(testCase in getAllTestTestCases(testRunner)) {
			for(testStep in testCase.getTestStepList()) {
				testStepList.add(testStep);
			}
		}
		return testStepList;
	}

	public static String getOnlineResourceForOperation(def capabilitesXml, String operationName) {
		return getHttpGetOnlineResourceForOperation(capabilitesXml, operationName);
	}

	public static String getHttpGetOnlineResourceForOperation(def capabilitesXml, String operationName) {
		// WMS 1.1.0
		String endpt = capabilitesXml.getNodeValue(
			"/*/*:Capability/*:Request/*:"+operationName+"/*:DCPType/*:HTTP/*:Get/*:OnlineResource/@*:href");
		if(endpt==null) {
			// WFS 1.0.0
			endpt = capabilitesXml.getNodeValue(
			"/*/*:Capability/*:Request/*:"+operationName+"/*:DCPType/*:HTTP/*:Get/@*:onlineResource");
		}
		if(endpt==null) {
			// WFS 2.0.0
			endpt = capabilitesXml.getNodeValue(
				"/*/*:OperationsMetadata/*:Operation[@name='"+operationName+"']/*:DCP/*:HTTP/*:Get/@*:href");
		}
		return endpt;
	}

	public static String getHttpPostOnlineResourceForOperation(def capabilitesXml, String operationName) {
		// WMS 1.1.0
		String endpt = capabilitesXml.getNodeValue(
				"/*/*:Capability/*:Request/*:"+operationName+"/*:DCPType/*:HTTP/*:Post/*:OnlineResource/@*:href");
		if(endpt==null) {
			// WFS 1.0.0
			endpt = capabilitesXml.getNodeValue(
					"/*/*:Capability/*:Request/*:"+operationName+"/*:DCPType/*:HTTP/*:Post/@*:onlineResource");
		}
		if(endpt==null) {
			// WFS 2.0.0
			endpt = capabilitesXml.getNodeValue(
					"/*/*:OperationsMetadata/*:Operation[@name='"+operationName+"']/*:DCP/*:HTTP/*:Post/@*:href");
		}
		return endpt;
	}

	/**
	 * Takes a List as parameter and returns the first, last, middle and three random elements
	 * if test is not intensive
	 **/
	public static def genRandomTestListOnIntesiveTests(def list, int numberOfRandomElements=3) {

		if(list.size() <= 3+numberOfRandomElements ||
			TestSetup.isTestIntensive())
		{
			return list;
		}

		def random = new Random();
		int midPos = list.size()/2;

		def testList = [];
		testList.add(list[0]);
		testList.add(list[midPos]);
		testList.add(list.size());

		while(testList.size()<numberOfRandomElements) {
			int randomPos = random.nextInt(list.size());
			if(!testList.contains(randomPos)) {
				testList.add(randomPos);
			}
		}
		return testList;
	}

	public static String xpathWithWildcardNamespaces(String xpath) {
		final String newPath='*:'
		int i = xpath.indexOf(':')+1
		if(i!=-1) {
			int nextPos = xpath.indexOf('/', i+1)
			for(; i!=-1 && nextPos!=-1;) {
				println i + " " + nextPos;
				println xpath.substring(i, nextPos)
				newPath += xpath.substring(i, nextPos) + '/*:';
				i = xpath.indexOf(':', i)+1;
				nextPos = xpath.indexOf('/', nextPos+1);
			}
			if(nextPos==-1 && i!=-1) {
				newPath += xpath.substring(i)
			}
			return newPath;
		}else{
			return xpath;
		}
	}


	private static int testStringId=1;

	public static setTestString() {
		new ProjectHelper().setTransferProperty("TESTSTRING",
			("XTRASERVER_TESTFRAMEWORK_EXCEPTION_TESTING__"+testStringId+++"__THIS_IS_NOT_A_BUG"));
	}
}
