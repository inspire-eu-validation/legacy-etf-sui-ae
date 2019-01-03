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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarFile;

/**
 * A Classloader which can reload objects from external achieves
 *
 * @author Jon Herrmann ( herrmann aT interactive-instruments doT de )
 *
 */
public final class ReloadableClassLoader extends URLClassLoader {

	private URLClassLoader iCL;

	public ReloadableClassLoader(URL[] urls) {
		super(urls);
		iCL = new URLClassLoader(urls);
	}

	public ReloadableClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		iCL = new URLClassLoader(urls, parent);
	}

	public void reload() {
		final ClassLoader parent = iCL.getParent();
		final URL[] urls = iCL.getURLs();
		resetAndLoad(urls, parent);
	}

	public void resetAndLoad(URL[] urls, ClassLoader parent) {
		try {
			iCL.close();
			forceCloseUcp(iCL);
		} catch (Exception e) {}
		iCL = null;
		iCL = new URLClassLoader(urls, parent);
	}

	public void resetAndLoad(URL[] urls) {
		try {
			iCL.close();
			forceCloseUcp(iCL);
		} catch (Exception e) {}
		iCL = null;
		iCL = new URLClassLoader(urls);
	}

	// Workaround for Windows: close all jar file handles
	private void forceCloseUcp(ClassLoader classLoader) {
		try {
			Class<? extends ReloadableClassLoader> clazz = this.getClass();
			Field ucp = clazz.getDeclaredField("ucp");
			ucp.setAccessible(true);
			Object sunMiscURLClassPath = ucp.get(classLoader);
			Field loaders = sunMiscURLClassPath.getClass().getDeclaredField("loaders");
			loaders.setAccessible(true);
			Collection<?> collection = (Collection<?>) loaders.get(sunMiscURLClassPath);
			for (Object sunMiscURLClassPathJarLoader : collection.toArray()) {
				try {
					Field loader = sunMiscURLClassPathJarLoader.getClass().getDeclaredField("jar");
					loader.setAccessible(true);
					Object jarFile = loader.get(sunMiscURLClassPathJarLoader);
					((JarFile) jarFile).close();
				} catch (Throwable t) {}
			}
		} catch (Throwable t) {}
	}

	@Override
	public URL findResource(String name) {
		return iCL.findResource(name);
	}

	@Override
	public Enumeration findResources(String name) throws IOException {
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
	public URL getResource(String name) {
		return iCL.getResource(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return iCL.getResourceAsStream(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return iCL.getResources(name);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return iCL.loadClass(name);
	}

	@Override
	public void setClassAssertionStatus(String className, boolean enabled) {
		iCL.setClassAssertionStatus(className, enabled);
	}

	@Override
	public void setDefaultAssertionStatus(boolean enabled) {
		iCL.setDefaultAssertionStatus(enabled);
	}

	@Override
	public void setPackageAssertionStatus(String packageName, boolean enabled) {
		iCL.setPackageAssertionStatus(packageName, enabled);
	}
}
