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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.interactive_instruments.container.ContainerFactory;
import de.interactive_instruments.etf.dal.assembler.AssemblerException;
import de.interactive_instruments.etf.dal.assembler.EntityAssembler;
import de.interactive_instruments.etf.dal.dto.result.TestReportDto;
import de.interactive_instruments.etf.model.plan.TestObject;
import de.interactive_instruments.etf.sel.model.impl.result.SuiTestReport;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class SuiTestReportAssembler implements EntityAssembler<TestReportDto, SuiTestReport> {

	private final String username = System.getProperty("user.name");

	@Override
	public List<SuiTestReport> assembleEntities(Collection<TestReportDto> collection) throws AssemblerException {
		final List<SuiTestReport> l = new ArrayList<>();
		for (TestReportDto testReportDto : collection) {
			l.add(assembleEntity(testReportDto));
		}
		return l;
	}

	@Override
	public SuiTestReport assembleEntity(TestReportDto dto) throws AssemblerException {
		final SuiTestObject testObject = new SuiTestObjectAssembler().assembleEntity(dto.getTestObject());
		return new SuiTestReport(dto.getContainerFactory(), username, dto.getPublicationLocation(),
				dto.getId(), dto.getLabel(), testObject);
	}
}
