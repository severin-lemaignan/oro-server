/*
 * Copyright (c) 2008-2010 LAAS-CNRS Séverin Lemaignan slemaign@laas.fr
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/

package laas.openrobots.ontology;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Attributes;

import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.exceptions.InvalidPluginException;
import laas.openrobots.ontology.exceptions.PluginNotFoundException;
import laas.openrobots.ontology.helpers.JarClassLoader;
import laas.openrobots.ontology.helpers.Logger;
import laas.openrobots.ontology.helpers.VerboseLevel;
import laas.openrobots.ontology.modules.IModule;

public class PluginLoader {

	private enum CONSTRUCTOR_TYPE {ORO_PARAMS, ORO, PARAMS, DEFAULT};
	private IOntologyBackend oro;
	private Properties params;
	
	public PluginLoader(IOntologyBackend oro, Properties params) {
		super();
		this.oro = oro;
		this.params = params;
	}
	
	public IModule loadJAR(String jarFile) throws PluginNotFoundException, InvalidPluginException {
		
		Logger.log("Loading plugin " + jarFile + "\n", VerboseLevel.VERBOSE);
		
		URL url = null;
        try {
            url = new URL(jarFile);
        } catch (MalformedURLException e) {
			throw new PluginNotFoundException(
					"Invalid URL for plugin:" +	jarFile + 
					" (check you didn't forget the file:// prefix)");
        }
        // Create the class loader for the application jar file
        JarClassLoader cl = new JarClassLoader(url);
        Attributes manifest;
        try {
			 manifest = cl.getManifestEntries();
		} catch (IOException e) {
			throw new InvalidPluginException("Could not read the plugin manifest for " + jarFile);
		}
		
		if (manifest == null)
			throw new InvalidPluginException("Invalid manifest for plugin " + jarFile);

		String className = manifest.getValue("Bundle-SymbolicName");
		String pluginName = manifest.getValue("Bundle-Name");
		String pluginVersion = manifest.getValue("Bundle-Version");

		if (className == null || className.isEmpty())
			throw new InvalidPluginException("The plugin manifest for " + jarFile + " contains no class name.");
			
		Class<IModule> pluginClass;
		try {
			pluginClass = (Class<IModule>) cl.loadClass(className);
		} catch (ClassNotFoundException e1) {
			Logger.log("Unable to find plugin " + className + ". Check your classpath. Skipping this plugin for now.\n", VerboseLevel.SERIOUS_ERROR);
			throw new PluginNotFoundException("Unable to find plugin " + className + ". Check your classpath.");
		}
					
		Constructor c;
		CONSTRUCTOR_TYPE mode = CONSTRUCTOR_TYPE.DEFAULT;

		IModule module = null;
		
		try {
			//First, looking for a Constructor(IOntologyBackend, Properties)
			c = pluginClass.getConstructor(IOntologyBackend.class, Properties.class);
			mode = CONSTRUCTOR_TYPE.ORO_PARAMS;
			
		} catch (SecurityException e) {
			Logger.log("Security exception while loading " + className + ". Skipping this plugin.\n", VerboseLevel.SERIOUS_ERROR);
			e.printStackTrace();
			throw new InvalidPluginException("Impossible to load the plugin (security exception)");
		} catch (NoSuchMethodException e) {
			try {
				//Then, looking for a Constructor(IOntologyBackend)
				c = pluginClass.getConstructor(IOntologyBackend.class);
				mode = CONSTRUCTOR_TYPE.ORO;
				
			} catch (SecurityException se) {
				Logger.log("Security exception while loading " + className + ". Skipping this plugin.\n", VerboseLevel.SERIOUS_ERROR);
				se.printStackTrace();
				throw new InvalidPluginException("Impossible to load the plugin (security exception)");
			} catch (NoSuchMethodException e2) {
				try {
					//Then, looking for a Constructor(Properties)
					c = pluginClass.getConstructor(Properties.class);
					mode = CONSTRUCTOR_TYPE.PARAMS;
				} catch (SecurityException se) {
					Logger.log("Security exception while loading " + className + ". Skipping this plugin.\n", VerboseLevel.SERIOUS_ERROR);
					se.printStackTrace();
					throw new InvalidPluginException("Impossible to load the plugin (security exception)");
				} catch (NoSuchMethodException e3) {
					try {
						//Finally, looking for a default Constructor()
						c = pluginClass.getConstructor();
					} catch (SecurityException se) {
						Logger.log("Security exception while loading " + className + ". Skipping this plugin.\n", VerboseLevel.SERIOUS_ERROR);
						se.printStackTrace();
						throw new InvalidPluginException("Impossible to load the plugin (security exception)");
					} catch (NoSuchMethodException e4) {
						Logger.log("Plugin " + className + " has no suitable constructor. Skipping this plugin.\n", VerboseLevel.SERIOUS_ERROR);
						throw new InvalidPluginException("Impossible to load the plugin (no suiable constructor)");
					}
				}
			}
		}

		
		try {
			switch (mode) {
				case ORO_PARAMS:
					module = (IModule) c.newInstance(oro, params);
					break;
				case ORO:
					module = (IModule) c.newInstance(oro);
					break;
				case PARAMS:
					module = (IModule) c.newInstance(params);
					break;
				case DEFAULT:
					module = (IModule) c.newInstance();
			}
			
		} catch (IllegalArgumentException e) {
			Logger.log("Unexpected error at plugin initialization. Skipping this plugin.\n", VerboseLevel.SERIOUS_ERROR);
			e.printStackTrace();
			throw new InvalidPluginException("Unexpected error at plugin initialization.");
		} catch (InstantiationException e) {
			Logger.log("Unexpected error at plugin initialization. Skipping this plugin.\n", VerboseLevel.SERIOUS_ERROR);
			e.printStackTrace();
			throw new InvalidPluginException("Unexpected error at plugin initialization.");
		} catch (IllegalAccessException e) {
			Logger.log("Unexpected error at plugin initialization. Skipping this plugin.\n", VerboseLevel.SERIOUS_ERROR);
			e.printStackTrace();
			throw new InvalidPluginException("Unexpected error at plugin initialization.");
		} catch (InvocationTargetException e) {
			Logger.log("Unexpected error at plugin initialization. Skipping this plugin.\n", VerboseLevel.SERIOUS_ERROR);
			e.printStackTrace();
			throw new InvalidPluginException("Unexpected error at plugin initialization.");
		}

		
		Logger.log("Plugin \"" + pluginName + "\" v." + pluginVersion + " successfully loaded and initialized.\n");
		
		return module;

	}
}
