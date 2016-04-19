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
package de.interactive_instruments.etf.sel.model.impl.plan;

import java.text.ParseException;
import java.util.Map;
import java.util.TreeSet;

import de.interactive_instruments.etf.model.item.*;
import de.interactive_instruments.etf.model.plan.AbstractRequirement;
import de.interactive_instruments.etf.model.plan.Requirement;
import de.interactive_instruments.properties.MutablePropertyHolder;
import de.interactive_instruments.properties.Properties;

/**
 * Maps a SoapUI Requirement to a model Requirement
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
public class SuiRequirement extends AbstractRequirement implements MutablePropertyHolder {

	public SuiRequirement(String label) {
		this.id = EidFactory.getDefault().createFromStrAsUUID(label);
		this.label = label;
		this.properties = new Properties();
		this.subRequirements = new TreeSet<>();
		this.versionData = DefaultVersionDataFactory.getInstance().createHash(this.toString());
	}

	public void addSubRequirement(final Requirement requirement) {
		this.subRequirements.add(requirement);
	}

	@Override
	public ModelItem getParent() {
		return null;
	}

	@Override
	public MutablePropertyHolder setProperty(String key, String value) {
		properties.setProperty(key, value);
		return this;
	}

	@Override
	public void removeProperty(String s) {
		this.properties.removeProperty(s);
	}
}
