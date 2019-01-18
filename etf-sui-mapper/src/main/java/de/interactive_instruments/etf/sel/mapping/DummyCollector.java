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
package de.interactive_instruments.etf.sel.mapping;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import de.interactive_instruments.IFile;
import de.interactive_instruments.etf.dal.dto.result.TestResultStatus;
import de.interactive_instruments.etf.sel.Utils;
import de.interactive_instruments.etf.testdriver.TestResultCollector;
import de.interactive_instruments.etf.testdriver.TestRunLogger;
import de.interactive_instruments.etf.testdriver.TestTaskEndListener;

/**
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 */
public class DummyCollector implements TestResultCollector {

	private final static String dummyUUID = "00000000-0000-0000-C000-000000000046";

	@Override
	public IFile getAttachmentDir() {
		return null;
	}

	@Override
	public IFile getResultFile() {
		return null;
	}

	@Override
	public String getTestTaskResultId() {
		return dummyUUID;
	}

	@Override
	public boolean endWithSkippedIfTestCasesFailed(final String... strings)
			throws IllegalArgumentException, IllegalStateException {
		return false;
	}

	@Override
	public TestResultStatus status(final String s) throws IllegalArgumentException {
		return null;
	}

	@Override
	public boolean statusEqualsAny(final String s, final String... strings) throws IllegalArgumentException {
		return false;
	}

	@Override
	public boolean isErrorLimitExceeded() {
		return false;
	}

	@Override
	public void addMessage(final String s) {

	}

	@Override
	public void addMessage(final String s, final Map<String, String> map) {

	}

	@Override
	public void addMessage(final String s, final String... strings) {

	}

	@Override
	public String markAttachment(final String s, final String s1, final String s2, final String s3, final String s4)
			throws IOException {
		return dummyUUID;
	}

	@Override
	public String saveAttachment(final Reader reader, final String s, final String s1, final String s2) throws IOException {
		return dummyUUID;
	}

	@Override
	public String saveAttachment(final InputStream inputStream, final String s, final String s1, final String s2)
			throws IOException {
		return dummyUUID;
	}

	@Override
	public String saveAttachment(final String s, final String s1, final String s2, final String s3) throws IOException {
		return dummyUUID;
	}

	@Override
	public File getTempDir() {
		return null;
	}

	@Override
	public void internalError(final String s, final Map<String, String> map, final Throwable throwable) {

	}

	@Override
	public void internalError(final Throwable throwable) {

	}

	@Override
	public String internalError(final String errorMessage, final byte[] bytes, final String mimeType) {
		return dummyUUID;
	}

	@Override
	public void info(final String message) {
		Utils.log(message);
	}

	@Override
	public void error(final String message) {
		Utils.log(message);
	}

	@Override
	public void debug(final String message) {
		Utils.log(message);
	}

	@Override
	public TestRunLogger getLogger() {
		return null;
	}

	@Override
	public String startTestTask(final String s, final long l) throws IllegalArgumentException, IllegalStateException {
		return dummyUUID;
	}

	@Override
	public String startTestModule(final String s, final long l) throws IllegalArgumentException, IllegalStateException {
		return dummyUUID;
	}

	@Override
	public String startTestCase(final String s, final long l) throws IllegalArgumentException, IllegalStateException {
		return dummyUUID;
	}

	@Override
	public String startTestStep(final String s, final long l) throws IllegalArgumentException, IllegalStateException {
		return dummyUUID;
	}

	@Override
	public String startTestAssertion(final String s, final long l) throws IllegalArgumentException, IllegalStateException {
		return dummyUUID;
	}

	@Override
	public String end(final String s, final int i, final long l) throws IllegalArgumentException, IllegalStateException {
		return dummyUUID;
	}

	@Override
	public String end(final String s, final long l) throws IllegalArgumentException, IllegalStateException {
		return dummyUUID;
	}

	@Override
	public int currentModelType() {
		return -1;
	}

	@Override
	public void registerTestTaskEndListener(final TestTaskEndListener listener) {

	}

	@Override
	public void release() {

	}
}
