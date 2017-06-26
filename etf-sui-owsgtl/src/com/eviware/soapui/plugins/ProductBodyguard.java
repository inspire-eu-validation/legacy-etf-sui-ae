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

package com.eviware.soapui.plugins;

import java.io.File;
import java.security.Provider;

/**
 * Bypass SoapUI Open Source signature validation of plugins
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public final class ProductBodyguard extends Provider {

	public ProductBodyguard() {
		super("SoapUIOSPluginSignChecker", 1.0, "Plugin signature validity checker");
	}

	public final synchronized boolean isKnown(File plugin) {
		return true;
	}
}
