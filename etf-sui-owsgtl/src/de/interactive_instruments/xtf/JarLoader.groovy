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
package de.interactive_instruments.xtf

import de.interactive_instruments.IFile

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.jar.Attributes

public class JarLoader extends URLClassLoader {
	
	URL[] urls;
	
	public JarLoader(URL[] urls) {
        super(urls);
        this.urls=urls;
    }
	
	public static void invokeMainMethod(IFile file, String[] args)
		throws Exception 
	{
		URL u=new URL("jar", "", file.toURI().toURL().toString() +"!/");
		JarURLConnection uc = (JarURLConnection)u.openConnection();
		Attributes attr = uc.getMainAttributes();
		if(attr==null) {
			throw new Exception("Main method of jar not found");
		}
		String mainClassName = attr.getValue(Attributes.Name.MAIN_CLASS);
				
		// Array initialization workaround for Java/Groovy
		URL[] tmpUrl = new URL[1];
		tmpUrl[0]=u;
		URLClassLoader loader = new URLClassLoader(tmpUrl);
		@SuppressWarnings("rawtypes")
		Class c = loader.loadClass(mainClassName);
		
		// Array initialization workaround for Java/Groovy
		Class[] tmpClass = new Class[1];
		tmpClass[0]=args.getClass();
		Method m = c.getMethod("main", tmpClass);
		m.setAccessible(true);
	    int mods = m.getModifiers();
	    if (m.getReturnType() != void.class || !Modifier.isStatic(mods) ||
	        !Modifier.isPublic(mods)) {
	        throw new NoSuchMethodException("main");
	    }
		// Array initialization workaround for Java/Groovy
    	Object[] tmpObj = new Object[1];
    	tmpObj[0]=args;
		m.invoke(null, tmpObj);
	}
}
