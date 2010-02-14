#!/usr/bin/python
# coding=utf-8

from xml.dom import minidom
import os, sys

ok = ""

while(ok != "y"):
	name = raw_input("Please enter the desired module name (should be a Java class name):")
	pkg = raw_input("Please enter the desired package (for instance: org.laas.oro.test):")
	fqn = pkg + "." + name

	output_path = "src/" + pkg.replace(".", "/") + "/"
	
	print "I'm about to create a plugin template called " + fqn
	print "All the files will be put in ./" + output_path
	
	ok1 = raw_input("Is that alright (y/n)?")
	
	if (ok1 == "y"):
		if (os.path.exists(output_path)):
			ok = raw_input("Attention! the destination path already exist. Are you sure you want to continue? (y/n)")
		else:
			os.makedirs(output_path)
			ok = "y"

f_manifest = open("./" + output_path + "MANIFEST", "w")
f_jardesc = open("./" + output_path + name + ".jardesc", "w")
f_module = open("./" + output_path + name + ".java", "w")
f_doc = open("./" + output_path + "package.html", "w")

print >> f_manifest, """Manifest-Version: 1.0
Bundle-Name: %(name)s
Bundle-SymbolicName: %(fqn)s
Bundle-Description: [add short description]
Bundle-Vendor: [add creator name]
Bundle-Version: 1.0
"""%{"name": name, "fqn": fqn}

print >> f_jardesc, """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<jardesc>
    <jar path="oro-server/plugins/%(name)s.jar"/>
    <options buildIfNeeded="true" compress="true" descriptionLocation="/oro-server/%(path)s%(name)s.jardesc" exportErrors="true" exportWarnings="true" includeDirectoryEntries="false" overwrite="true" saveDescription="true" storeRefactorings="false" useSourceFolders="false"/>
    <storedRefactorings deprecationInfo="true" structuralOnly="false"/>
    <selectedProjects/>
    <manifest generateManifest="false" manifestLocation="/oro-server/%(path)sMANIFEST" manifestVersion="1.0" reuseManifest="true" saveManifest="true" usesManifest="true">
        <sealing sealJar="false">
            <packagesToSeal/>
            <packagesToUnSeal/>
        </sealing>
    </manifest>
    <selectedElements exportClassFiles="true" exportJavaFiles="true" exportOutputFolder="false">
        <javaElement handleIdentifier="=oro-server/src&lt;%(pkg)s{%(name)s.java"/>
        <!-- add here other java file you need in your plugin -->
        <file path="/oro-server/%(path)spackage.html"/>
    </selectedElements>
</jardesc>"""%{"name": name, "path": output_path, "pkg": pkg}

print >> f_module, """/**
 * %(name)s plugin for ORO
 */
package %(pkg)s;

import java.util.Properties;
import java.util.Set;

import laas.openrobots.ontology.backends.IOntologyBackend;
import laas.openrobots.ontology.modules.IModule;
import laas.openrobots.ontology.service.IServiceProvider;
import laas.openrobots.ontology.service.RPCMethod;

/**
 * @author [add your name]
 *
 */
public class %(name)s implements IModule, IServiceProvider {

	IOntologyBackend oro;
	Properties serverParams;
	
	/**
	 * You must implement at least one of these four constructors
	 */
	public %(name)s(IOntologyBackend oro, Properties serverParams) {
		super();
		this.oro = oro;
		this.serverParams = serverParams;
	}
	
	public %(name)s(IOntologyBackend oro) {
		super();
		this.oro = oro;
	}

	public %(name)s(Properties serverParams) {
		super();
		this.serverParams = serverParams;
	}

	public %(name)s() {
		super();
	}
	
	/**
	 * The foo function has a @RPCMethod annotation: it will be automatically 
	 * registered and exposed by the server at the next restart.
	 * 
	 * @param args a set of strings
	 * @return the concatenation of all the strings
	 */
	@RPCMethod (
			category = "not classified",
			desc = "concatenate a set of strings."
			)
	public String foo(Set<String> args) {
		String res = "";
		for (String s: args) res += s;
		return res;
	}
	
	/* (non-Javadoc)
	 * @see laas.openrobots.ontology.modules.IModule#getServiceProvider()
	 */
	@Override
	public IServiceProvider getServiceProvider() {
		return this;
	}

}
"""%{"name": name, "pkg": pkg}

print >> f_doc, """<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
<html>
<head>
</head>
<body bgcolor="white">

[add here plugin documentation]

</body>
</html>"""
