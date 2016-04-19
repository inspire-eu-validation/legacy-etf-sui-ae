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
import java.util.Map;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.driver.TestProjectUnavailable;
import de.interactive_instruments.etf.model.item.DefaultVersionData;
import de.interactive_instruments.etf.model.item.EID;
import de.interactive_instruments.etf.model.item.EidMap;
import de.interactive_instruments.etf.model.item.ImmutableVersionData;
import de.interactive_instruments.etf.model.plan.AbstractTestProject;
import de.interactive_instruments.etf.model.plan.TestObjectResourceType;
import de.interactive_instruments.etf.model.plan.TestProject;
import de.interactive_instruments.exceptions.config.MissingPropertyException;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class SuiTestProject extends AbstractTestProject {

	IFile tmpProjectFile;

	public SuiTestProject(EID id, ImmutableVersionData versionData, String label, String description, URI uri, EidMap<TestObjectResourceType> supportedResources) {
		super(uri, supportedResources);
		this.versionData = new DefaultVersionData(versionData);
		this.id = id;
		this.label = label;
		this.description = description;
	}

	/**
	 * Creates a tmp copy of the project file
	  */
	public IFile getTmpProjectFileCopy() throws IOException, TestProjectUnavailable {
		if (tmpProjectFile == null) {
			final IFile originalProjectFile = new IFile(projectUri.getPath());
			try {
				originalProjectFile.expectFileIsReadable();
			} catch (IOException e) {
				throw new TestProjectUnavailable(originalProjectFile.toString());
			}
			tmpProjectFile = originalProjectFile.createTempCopy(
					label + "_proj", "xtf");
			tmpProjectFile.expectIsReadAndWritable();
		}
		return tmpProjectFile;
	}
}
