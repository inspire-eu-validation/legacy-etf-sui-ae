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
package de.interactive_instruments.etf.sel.groovyengine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import com.eviware.soapui.SoapUIExtensionClassLoader;

import de.interactive_instruments.CLUtils;

/**
 * An SoapUIExtensionClassLoader which can reload objects from external achieves
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 *
 */
public final class ReloadableSoapUIExtensionClassLoader extends SoapUIExtensionClassLoader {

	private SoapUIExtensionClassLoader iCL;
	private final URL[] urls;

	public ReloadableSoapUIExtensionClassLoader(final URL[] urls, ClassLoader parent) {
		super(urls, parent);
		iCL = new SoapUIExtensionClassLoader(urls, parent);
		this.urls = urls;
		try {
			this.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void resetAndLoad(final URL[] urls, ClassLoader parent) {

		try {
			iCL.close();
			CLUtils.forceCloseUcp(iCL);
		} catch (Exception e) {}
		iCL = null;
		final SoapUIExtensionClassLoader newCL = new SoapUIExtensionClassLoader(urls, parent);
		System.out.println("Replacing ClassLoader " + iCL + " with " + newCL);
		iCL = newCL;
	}

	public void resetAndLoad(final URL[] urls) {
		try {
			iCL.close();
			CLUtils.forceCloseUcp(iCL);
		} catch (Exception e) {}
		final SoapUIExtensionClassLoader newCL = new SoapUIExtensionClassLoader(urls, this.getParent());
		System.out.println("Replacing ClassLoader " + iCL + " with " + newCL);
		iCL = null;
		iCL = newCL;
	}

	public void reset() {
		try {
			iCL.close();
			CLUtils.forceCloseUcp(iCL);
		} catch (Exception e) {}
		final SoapUIExtensionClassLoader newCL = new SoapUIExtensionClassLoader(this.urls, this.getParent());
		System.out.println("Replacing ClassLoader " + iCL + " with " + newCL);
		iCL = null;
		iCL = newCL;
	}

	@Override
	public void addFile(final File file) throws MalformedURLException {
		iCL.addFile(file);
	}

	@Override
	public void addURL(final URL url) {
		iCL.addURL(url);
	}

	@Override
	public URL findResource(final String name) {
		return iCL.findResource(name);
	}

	@Override
	public Enumeration findResources(final String name) throws IOException {
		return iCL.findResources(name);
	}

	@Override
	public URL[] getURLs() {
		return iCL.getURLs();
	}

	@Override
	public void clearAssertionStatus() {
		iCL.clearAssertionStatus();
	}

	@Override
	public URL getResource(final String name) {
		return iCL.getResource(name);
	}

	@Override
	public InputStream getResourceAsStream(final String name) {
		return iCL.getResourceAsStream(name);
	}

	@Override
	public Enumeration<URL> getResources(final String name) throws IOException {
		return iCL.getResources(name);
	}

	@Override
	public Class<?> loadClass(final String name) throws ClassNotFoundException {
		return findClass(name);
		// return iCL.loadClass(name);
	}

	@Override
	protected Class<?> loadClass(final String name,
			boolean resolve) throws ClassNotFoundException {
		return iCL.loadClass(name);
	}

	@Override
	public Class<?> findClass(final String name) throws ClassNotFoundException {
		return iCL.loadClass(name);
	}

	@Override
	public void setClassAssertionStatus(final String className, final boolean enabled) {
		iCL.setClassAssertionStatus(className, enabled);
	}

	@Override
	public void setDefaultAssertionStatus(final boolean enabled) {
		iCL.setDefaultAssertionStatus(enabled);
	}

	@Override
	public void setPackageAssertionStatus(final String packageName, final boolean enabled) {
		iCL.setPackageAssertionStatus(packageName, enabled);
	}
}
