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

import de.interactive_instruments.etf.dal.assembler.AssemblerException;
import de.interactive_instruments.etf.dal.assembler.EntityAssembler;
import de.interactive_instruments.etf.dal.dto.plan.TestObjectDto;
import de.interactive_instruments.etf.model.item.EID;
import de.interactive_instruments.etf.model.item.EidFactory;

/**
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 */
public class SuiTestObjectAssembler implements EntityAssembler<TestObjectDto, SuiTestObject> {
	@Override
	public Collection<SuiTestObject> assembleEntities(Collection<TestObjectDto> testObjectDtos) throws AssemblerException {
		final List<SuiTestObject> l = new ArrayList<>();
		for (TestObjectDto testObjectDto : testObjectDtos) {
			l.add(assembleEntity(testObjectDto));
		}
		return l;
	}

	@Override
	public SuiTestObject assembleEntity(TestObjectDto dto) throws AssemblerException {

		if (dto.getResourceById("serviceEndpoint") == null) {
			throw new AssemblerException("\"serviceEndpoint\" not set");
		}

		EID id = dto.getId();
		if (id == null) {
			id = EidFactory.getDefault().createFromStrAsUUID(dto.getResourceById("serviceEndpoint").toString());
		}

		return new SuiTestObject(id, dto.getVersionData().toVersionData(), dto.getLabel(),
				dto.getResourcesAsModelItemMap(),
				dto.getProperties(), dto.getDescription());
	}
}
