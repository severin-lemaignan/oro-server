#!/usr/bin/python
# coding=utf-8

import os, sys
import datetime


def run_script():

	now = datetime.datetime.now()
	
	ok = ""

	while(ok != "y"):
		name = raw_input("Please enter the desired module name (should be a Java class name):")
		pkg = raw_input("Please enter the desired package (for instance: org.laas.oro.test):")
		fqn = pkg + "." + name

		output_path = name + "/"
		source_path = output_path + "src/" + pkg.replace(".", "/") + "/"
		
		print
		print "I'm about to create a plugin template called " + fqn
		print "Build & Makefile files will be put in ./" + output_path
		print "Template source files will be put in ./" + source_path
		print
		
		ok1 = raw_input("Is that alright (y/n)?")
		
		if (ok1 == "y"):
			if (os.path.exists(output_path)):
				ok = raw_input("Attention! the destination path already exist. Are you sure you want to continue? (y/n)")
			else:
				os.makedirs(source_path)
				ok = "y"

	f_manifest = open("./" + output_path + "MANIFEST", "w")
	f_makefile = open("./" + output_path + "Makefile", "w")
	f_jardesc = open("./" + output_path + name + ".jardesc", "w")
	f_module = open("./" + source_path + name + ".java", "w")
	f_doc = open("./" + source_path + "package.html", "w")

	print >> f_manifest, """Manifest-Version: 1.0
Bundle-Name: %(name)s
Bundle-SymbolicName: %(fqn)s
Bundle-Description: [add short description]
Bundle-Vendor: [add creator name]
Bundle-Version: 1.0
"""%{"name": name, "fqn": fqn}
	
	print >> f_makefile, """#Makefile for %(name)s plugin for oro-server
# (c) [add you name/organization here] %(year)s

BUNDLE_NAME = %(name)s
BASE_PACKAGE = %(pkg)s

PREFIX ?= /usr/local

BUILD_DIR = build
SRC_DIR = src
DOC_DIR ?= $(PREFIX)/share/doc/oro-plugins/$(BUNDLE_NAME)

#Set here the path to your oro-server JAR package
ORO_SERVER ?= $(PREFIX)/java/oro-server/lib
PLUGINS_DIR ?= $(ORO_SERVER)/java/oro-server/plugins

#Add here dependency on other Java libs
CLASSPATH = $(ORO_SERVER)/oro-server.jar

JAVA?= java
JAVAC?= javac
JAVADOC?= javadoc
JAR?= jar

CLEAN?= rm -rf
INSTALL?= /usr/bin/install
CP?= cp

########## Variables for Javadoc documentation ##########
WINDOWTITLE = 'ORO - $(BUNDLE_NAME) - Plugin documentation'
HEADER = '<b>ORO - $(BUNDLE_NAME)</b><br/><font size="-1">Plugin documentation - build on $(shell date +%%F)</font>'
BOTTOM = '<font size="-1">ORO is a part of the <a href="https://softs.laas.fr/openrobots/wiki/">OpenRobots</a> framework.<br/><br><a href="mailto:openrobots@laas.fr">openrobots@laas.fr</a><br/>LAAS-CNRS 2010</font>'
##########################################################

all : oro-plugin-jar

oro-plugin-jar: oro-build	
	$(JAR) cfm $(BUNDLE_NAME).jar MANIFEST -C $(BUILD_DIR) .

oro-build:
	$(INSTALL) -d $(BUILD_DIR)
	$(JAVAC) -classpath $(CLASSPATH) -d $(BUILD_DIR) `find -name "*.java"`

install: oro-plugin-jar install-doc
	$(INSTALL) -d $(PLUGINS_DIR)
	$(INSTALL) $(BUNDLE_NAME).jar $(PLUGINS_DIR)
	echo Your plugin has been installed in $(PLUGINS_DIR)

distclean: clean doc-clean
	$(CLEAN) $(BUNDLE_NAME).jar

clean:
	$(CLEAN) $(BUILD_DIR)

install-doc:
	$(INSTALL) -d $(DOC_DIR)
	$(JAVADOC) -sourcepath $(SRC_DIR) \
	-encoding "UTF-8" \
	-charset "UTF-8" \
	-docencoding "UTF-8" \
	-classpath $(CLASSPATH) \
	-d $(DOC_DIR) \
	-use \
	-windowtitle $(WINDOWTITLE) \
	-doctitle $(HEADER) \
	-header $(HEADER) \
	-bottom $(BOTTOM) \
	-J-Xmx180m \
	-subpackages $(BASE_PACKAGE)

doc-clean:
	$(CLEAN) $(DOC_DIR)
"""%{"name": name, "path": output_path, "pkg": pkg, "year": now.year}

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
	
	print
	print "Your templates have been successfully generated!"
	print """
You still have some homeworks to do:
  - open and complete ./%(output_path)sMANIFEST,
  - if you plan to use the GNU Make Makefile, open and edit ./%(output_path)sMakefile
to fit it to your system configuration (you'll probably need to complete the
classpath),
  - if you plan to use the Eclipse jardesc, edit it to add other Java source
file you may depend on,
  - don't forget to document your plugin in ./%(source_path)spackage.html

Then, enjoy hacking your new %(name)s.java source file!
	"""%{"output_path": output_path, "source_path": source_path, "name": name}


if "__main__" == __name__:
	try:
		run_script()
	except KeyboardInterrupt:
		print
		print
		print "Generation of plugin template cancelled. Bye bye!"
             
