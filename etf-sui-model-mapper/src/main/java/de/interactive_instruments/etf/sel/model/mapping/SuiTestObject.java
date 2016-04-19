/**
 * Copyright 2010-2016 interactive instruments GmbH
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
package de.interactive_instruments.etf.sel.model.mapping;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Map;

import de.interactive_instruments.Version;
import de.interactive_instruments.etf.model.item.*;
import de.interactive_instruments.etf.model.plan.AbstractTestObject;
import de.interactive_instruments.etf.model.plan.TestObjectResource;
import de.interactive_instruments.etf.model.plan.TestObjectResourceType;
import de.interactive_instruments.properties.Properties;
import de.interactive_instruments.properties.PropertyHolder;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class SuiTestObject extends AbstractTestObject {

	public SuiTestObject(String serviceEndpoint) {
		this.id = EidFactory.getDefault().createFromStrAsUUID(serviceEndpoint);
		this.versionData = DefaultVersionDataFactory.getInstance().createHash(this.toString());
		this.type = TestObjectType.EXECUTABLE;
		this.addResource(EidFactory.getDefault().createFromStrAsStr("serviceEndpoint"),
				URI.create(serviceEndpoint));
	}

	SuiTestObject(EID id, ImmutableVersionData versionData, String label, ModelItemMap<TestObjectResource> resources, Properties properties, String description) {
		this.id = id;
		this.versionData = new DefaultVersionData(versionData);
		this.type = TestObjectType.EXECUTABLE;
		this.resources = resources;
		this.properties = properties;
		this.description = description;
		// tbd calculate bytes
		this.label = label;
	}
}
