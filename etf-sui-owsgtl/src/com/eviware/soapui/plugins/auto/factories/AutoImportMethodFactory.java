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

package com.eviware.soapui.plugins.auto.factories;

import com.eviware.soapui.impl.actions.ImportMethod;
import com.eviware.soapui.impl.actions.ImportMethodFactory;
import com.eviware.soapui.plugins.auto.PluginDiscoveryMethod;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class AutoImportMethodFactory extends AbstractSoapUIFactory implements ImportMethodFactory {

	public AutoImportMethodFactory(final Class factoryType) {
		super(factoryType);
	}

	public AutoImportMethodFactory(PluginDiscoveryMethod annotation, Class<ImportMethod> methodClass) {
		super(ImportMethodFactory.class);
	}

	@Override public ImportMethod createNewDiscoveryMethod() {
		return null;
	}
}
