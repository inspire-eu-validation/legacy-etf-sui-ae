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
package de.interactive_instruments.etf.sel.assertions;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionCategoryMapping;
import com.eviware.soapui.impl.wsdl.panels.assertions.AssertionListEntry;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.TestAssertionFactory;
import com.eviware.soapui.model.TestPropertyHolder;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.TestAssertion;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class SchemaValidationFactory implements TestAssertionFactory {

	@Override
	public TestAssertion buildAssertion(TestAssertionConfig config,
			Assertable modelItem) {
		return new SchemaAssertionImpl(config, modelItem);
	}

	@Override
	public Class<? extends WsdlMessageAssertion> getAssertionClassType() {
		return SchemaAssertionImpl.class;
	}

	@Override
	public String getAssertionId() {
		return SchemaAssertionImpl.ID;
	}

	@Override
	public String getAssertionLabel() {
		return SchemaAssertionImpl.LABEL;
	}

	@Override
	public AssertionListEntry getAssertionListEntry() {
		return new AssertionListEntry(SchemaAssertionImpl.ID,
				SchemaAssertionImpl.LABEL, SchemaAssertionImpl.DESCRIPTION);
	}

	@Override
	public String getCategory() {
		return AssertionCategoryMapping.VALIDATE_RESPONSE_CONTENT_CATEGORY;
	}

	@Override
	public boolean canAssert(Assertable arg0) {
		return true;
	}

	@Override
	public boolean canAssert(TestPropertyHolder arg0, String arg1) {
		return true;
	}
}
