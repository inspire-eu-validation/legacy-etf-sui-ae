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
package de.interactive_instruments.etf.sel.model.impl;

/**
 * Separators used to separate key value pairs in the SoapUI property value strings
 *
 * @author J. Herrmann ( herrmann <aT) interactive-instruments (doT> de )
 *
 */
final public class Separators {

	private Separators() {}

	/*
	 * Separates parent and sub Requirement.
	 * Example: Requirement.1##SubRequirement.1
	 */
	public final static String SUB_REQ = "##";

	/*
	 * Used for property initialization.
	 * Example: Requirement.1__Name=
	 */
	public final static String ID_PROPERTY = "__";

	/*
	 * Seperate the Subrequirement from the Assertion
	 * Example: SubRequirement.1: Root element exists
	 */
	public final static String REQ_ASSERTION = ": ";

	/*
	 * Seperate multiple values
	 * Example: Requirement.1###Requirement.2###Requirement.3
	 */
	public final static String MULTIPLE_VALUE = "###";
}
