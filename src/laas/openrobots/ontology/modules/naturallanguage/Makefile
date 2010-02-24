#Makefile for NaiveNaturalLanguageModule plugin for oro-server
# (c) LAAS-CNRS 2010

BUNDLE_NAME = NaiveNaturalLanguageModule
BASE_PACKAGE = laas.openrobots.ontology.modules.naturallanguage

PREFIX ?= /usr/local

BUILD_DIR = build
DOC_DIR ?= $(PREFIX)/share/doc/oro-$(BUNDLE_NAME)

ORO_SERVER ?= $(PREFIX)/java/oro-server/lib
PLUGIN_DIR ?= $(ORO_SERVER)/plugins

#Set here the path to your oro-server JAR package
CLASSPATH ?= $(ORO_SERVER)/oro-server.jar

JAVA?= java
JAVAC?= javac
JAVADOC?= javadoc
JAR?= jar

CLEAN?= rm -rf
INSTALL?= /usr/bin/install
CP?= cp

########## Variables for Javadoc documentation ##########
WINDOWTITLE = 'ORO - $(BUNDLE_NAME) - Plugin documentation'
HEADER = '<b>ORO - $(BUNDLE_NAME)</b><br/><font size="-1">Plugin documentation - build on $(shell date +%F)</font>'
BOTTOM = '<font size="-1">ORO is a part of the <a href="https://softs.laas.fr/openrobots/wiki/">OpenRobots</a> framework.<br/><br><a href="mailto:openrobots@laas.fr">openrobots@laas.fr</a><br/>LAAS-CNRS 2010</font>'
##########################################################

all : oro-plugin-jar install-doc

oro-plugin-jar: oro-build	
	$(JAR) cfme $(BUNDLE_NAME).jar MANIFEST -C $(BUILD_DIR) .

oro-build :
	$(INSTALL) -d $(BUILD_DIR)
	$(JAVAC) -classpath $(CLASSPATH) -d $(BUILD_DIR) `find -name "*.java"`

install: oro-plugin-jar
	$(INSTALL) -d $(PLUGIN_DIR)
	$(INSTALL) $(BUNDLE_NAME).jar $(PLUGIN_DIR)
	echo Your plugin has been installed in $(PLUGIN_DIR)
	
distclean: clean doc-clean
	$(CLEAN) $(BUNDLE_NAME).jar

clean :
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
	-stylesheetfile javadoc.css \
	-subpackages $(BASE_PACKAGE)


doc-clean:
	$(CLEAN) $(DOC_DIR)
