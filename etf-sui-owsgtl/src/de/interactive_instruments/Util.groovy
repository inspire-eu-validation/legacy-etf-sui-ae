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
package de.interactive_instruments

import de.interactive_instruments.xtf.SOAPUI_I

/**
 * Proxy for previous versions
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
class Util {
    public static void updateCredentials(def testRunner) {
        de.interactive_instruments.xtf.Util.updateCredentials(testRunner);
    }

    public static void removeAuthorization(def testStep) {
        de.interactive_instruments.xtf.Util.removeAuthorization(testStep);
    }

    public static void setProjectProperty(def testRunner, String name, String value) {
        de.interactive_instruments.xtf.Util.setProjectProperty(testRunner, name, value);
    }

    public static String getPropertyValueOrDefault(def modelItem, String name, def defaultValue) {
        return de.interactive_instruments.xtf.Util.getPropertyValueOrDefault(modelItem, name, defaultValue);
    }

    public static String getProjectPropertyOrNull(String propertyName, def testRunner=SOAPUI_I.getInstance().getTestRunner()) {
        return de.interactive_instruments.xtf.Util.getProjectPropertyOrNull(propertyName, testRunner);
    }

    public static String getProjectProperty(String propertyName,
                                            def testRunner=SOAPUI_I.getInstance().getTestRunner()) {
        return de.interactive_instruments.xtf.Util.getProjectProperty(propertyName, testRunner);
    }
}
