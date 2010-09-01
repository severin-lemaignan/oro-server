#Makefile for oro-server
# (c) LAAS-CNRS 2010

PREFIX ?= /usr/local

BUILD_DIR = build
SRC_DIR = src

DOC_DIR ?= ${PREFIX}/share/doc/oro-server

#default plugins directory
PLUGINS_DIR ?= ${PREFIX}/java/oro-server/plugins

#default ontologies directory
ONTOLOGIES_DIR ?= ${PREFIX}/share/ontologies

#holds diagrams, CSS, etc.
MEDIA_DIR = media

BASE_PACKAGE = laas.openrobots.ontology
ENTRYPOINT = $(BASE_PACKAGE).OroServer

JENA_LIBS = $(PREFIX)/java/jena/lib
PELLET_LIBS = $(PREFIX)/java/pellet/lib

CLASSPATH = $(JENA_LIBS)/arq-2.8.4.jar:$(JENA_LIBS)/iri-0.8.jar:$(JENA_LIBS)/log4j-1.2.13.jar:$(JENA_LIBS)/icu4j-3.4.4.jar:$(JENA_LIBS)/jena-2.6.3.jar:$(JENA_LIBS)/xercesImpl-2.7.1.jar:$(JENA_LIBS)/junit-4.5.jar:$(JENA_LIBS)/slf4j-api-1.5.8.jar:$(JENA_LIBS)/slf4j-log4j12-1.5.8.jar:$(PELLET_LIBS)/aterm-java-1.6.jar:$(PELLET_LIBS)/pellet-core.jar:$(PELLET_LIBS)/pellet-datatypes.jar:$(PELLET_LIBS)/pellet-el.jar:$(PELLET_LIBS)/pellet-jena.jar:$(PELLET_LIBS)/pellet-query.jar:$(PELLET_LIBS)/pellet-rules.jar:$(PELLET_LIBS)/xsdlib/relaxngDatatype.jar:$(PELLET_LIBS)/xsdlib/xsdlib.jar

JAVA?= java
JAVAC?= javac
JAVADOC?= javadoc
JAR?= jar

CLEAN?= rm -rf
INSTALL?= /usr/bin/install
CP?= cp

########## Variables for Javadoc documentation ##########
WINDOWTITLE = 'ORO: the OpenRobots Ontology - Server documentation'
HEADER = '<b>ORO: the OpenRobots Ontology</b><br/><font size="-1">Server documentation - build on $(shell date +%F)</font>'
BOTTOM = '<font size="-1">ORO is a part of the <a href="https://softs.laas.fr/openrobots/wiki/">OpenRobots</a> framework.<br/><a href="mailto:openrobots@laas.fr">openrobots@laas.fr</a><br/>LAAS-CNRS 2010</font>'
GROUPCORE = "Core Packages" "$(BASE_PACKAGE)*"
GROUPMODULES  = "Modules Packages" "$(BASE_PACKAGE).modules*"
#GROUPSERVER  = "Connectors Packages" "$(BASE_PACKAGE).connectors*"
GROUPTESTS = "Tests Packages" "$(BASE_PACKAGE).tests*"
##########################################################

all : oro-server

oro-server: oro-jar
	/bin/echo -e '#!/bin/sh\n$(JAVA) -Djava.library.path=$(PREFIX)/lib -jar $(PREFIX)/java/oro-server/lib/oro-server.jar $$1' > oro-server
	chmod +x oro-server
	
	#Replace paths in configuration files
	sed 's*@PLUGINS_PATH@*$(PLUGINS_DIR)*' etc/oro-server/oro.conf.in > etc/oro-server/oro.conf.in2
	sed 's*@ONTOLOGIES_PATH@*$(ONTOLOGIES_DIR)*' etc/oro-server/oro.conf.in2 > etc/oro-server/oro.conf
	$(CLEAN) etc/oro-server/oro.conf.in2
	sed 's*@PLUGINS_PATH@*$(PLUGINS_DIR)*' etc/oro-server/oro_test.conf.in > etc/oro-server/oro_test.conf.in2
	sed 's*@ONTOLOGIES_PATH@*$(ONTOLOGIES_DIR)*' etc/oro-server/oro_test.conf.in2 > etc/oro-server/oro_test.conf
	$(CLEAN) etc/oro-server/oro_test.conf.in2
	
	echo "If you have the test ontology oro_test.owl, you can now run 'make test' to run the unit tests"

oro-jar: oro-build
	/bin/echo -e "Class-Path: \n `echo $(CLASSPATH) | sed 's/:/ \n /g'`" > MANIFEST.MF
	$(JAR) cfme oro-server.jar MANIFEST.MF $(ENTRYPOINT) -C $(BUILD_DIR) .
	$(CLEAN) MANIFEST.MF

oro-build :
	$(INSTALL) -d $(BUILD_DIR)
	$(JAVAC) -classpath $(CLASSPATH) -d $(BUILD_DIR) `find $(SRC_DIR)/laas -name "*.java"`

install: oro-server install-doc
	$(INSTALL) -d ${PREFIX}/java/oro-server/lib
	$(INSTALL) oro-server.jar ${PREFIX}/java/oro-server/lib
	$(INSTALL) -d ${PREFIX}/etc/oro-server
	$(INSTALL) etc/oro-server/*.conf ${PREFIX}/etc/oro-server
	$(INSTALL) -d ${PREFIX}/bin
	$(INSTALL) oro-server ${PREFIX}/bin

distclean: clean doc-clean
	$(CLEAN) oro-server
	$(CLEAN) oro-server.jar
	$(CLEAN) etc/oro-server/*.conf
	$(CLEAN) *.log
	$(CLEAN) *.owl
	$(CLEAN) *.tar.gz

clean :
	$(CLEAN) $(BUILD_DIR)
	$(CLEAN) etc/oro-server/*.conf

install-doc:
	$(INSTALL) -d ${DOC_DIR}
	$(JAVADOC) -sourcepath $(SRC_DIR) \
	-encoding "UTF-8" \
	-charset "UTF-8" \
	-docencoding "UTF-8" \
	-overview $(SRC_DIR)/overview/overview.html \
	-classpath $(CLASSPATH) \
	-d $(DOC_DIR) \
	-use \
	-windowtitle $(WINDOWTITLE) \
	-doctitle $(HEADER) \
	-header $(HEADER) \
	-bottom $(BOTTOM) \
	-group $(GROUPCORE) \
	-group $(GROUPMODULES) \
	-group $(GROUPTESTS) \
	-link http://jena.sourceforge.net/javadoc \
	-J-Xmx180m \
	-stylesheetfile $(MEDIA_DIR)/javadoc.css \
	-subpackages $(BASE_PACKAGE)
	$(INSTALL) -t $(DOC_DIR) $(MEDIA_DIR)/*

doc-clean:
	$(CLEAN) $(DOC_DIR)

test:
	$(JAVA) -classpath $(CLASSPATH):${PREFIX}/java/oro-server/lib/oro-server.jar -DORO_TEST_CONF=${PREFIX}/etc/oro-server/oro_test.conf junit.textui.TestRunner $(BASE_PACKAGE).tests.OpenRobotsOntologyTest

