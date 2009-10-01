#Makefile for oro-server
# (c) LAAS-CNRS 2009

PREFIX ?= /usr/local

BUILD_DIR = build
SRC_DIR = src
DOC_DIR = doc

BASE_PACKAGE = laas.openrobots.ontology
ENTRYPOINT = $(BASE_PACKAGE).OroServer

JENA_LIBS = $(PREFIX)/java/jena/lib
PELLET_LIBS = $(PREFIX)/java/pellet/lib

CLASSPATH = $(JENA_LIBS)/arq.jar:$(JENA_LIBS)/slf4j-api-1.5.6.jar:$(JENA_LIBS)/slf4j-log4j12-1.5.6.jar:$(JENA_LIBS)/log4j-1.2.12.jar:$(JENA_LIBS)/icu4j_3_4.jar:$(JENA_LIBS)/jena.jar:$(JENA_LIBS)/stax-api-1.0.jar:$(JENA_LIBS)/xercesImpl.jar:$(JENA_LIBS)/junit-4.5.jar:$(JENA_LIBS)/iri.jar:$(PELLET_LIBS)/aterm-java-1.6.jar:$(PELLET_LIBS)/pellet-core.jar:$(PELLET_LIBS)/pellet-datatypes.jar:$(PELLET_LIBS)/pellet-el.jar:$(PELLET_LIBS)/pellet-jena.jar:$(PELLET_LIBS)/pellet-query.jar:$(PELLET_LIBS)/pellet-rules.jar:$(PELLET_LIBS)/xsdlib/relaxngDatatype.jar:$(PELLET_LIBS)/xsdlib/xsdlib.jar

JAVA?= java
JAVAC?= javac
JAVADOC?= javadoc
JAR?= jar

CLEAN?= rm -rf
INSTALL?= /usr/bin/install
CP?= cp

########## Variables for Javadoc documentation ##########
WINDOWTITLE = 'ORO: the OpenRobots Ontology - Server documentation'
HEADER = '<b>ORO: the OpenRobots Ontology</b><br/><font size="-1">Server documentation</font>'
BOTTOM = '<font size="-1">ORO is a part of the <a href="https://softs.laas.fr/openrobots/wiki/">OpenRobots</a> framework.<br/><br><a href="mailto:openrobots@laas.fr">openrobots@laas.fr</a><br/>LAAS-CNRS 2009</font>'
GROUPCORE = "Core Packages" "$(BASE_PACKAGE)*"
GROUPBACKEND  = "Ontology Backend Packages" "$(BASE_PACKAGE).backends*"
GROUPSERVER  = "Connectors Packages" "$(BASE_PACKAGE).connectors*"
GROUPTESTS = "Tests Packages" "$(BASE_PACKAGE).tests*"
##########################################################

all : oro-server doc

oro-server: oro-jar
	/bin/echo -e '#!/bin/sh\n$(JAVA) -Djava.library.path=$(PREFIX)/lib -jar $(PREFIX)/java/oro-server/lib/oro-server.jar $$1' > oro-server
	chmod +x oro-server
	echo "If you have the test ontology oro_test.owl, you can now run 'make test' to run the unit tests"

oro-jar: oro-build
	/bin/echo -e "Class-Path: \n `echo $(CLASSPATH) | sed 's/:/ \n /g'`" > MANIFEST.MF
	$(JAR) cfme oro-server.jar MANIFEST.MF $(ENTRYPOINT) -C $(BUILD_DIR) .
	$(CLEAN) MANIFEST.MF

oro-build :
	$(INSTALL) -d $(BUILD_DIR)
	$(JAVAC) -classpath $(CLASSPATH) -d $(BUILD_DIR) `find -name "*.java"`

install: oro-server
	$(INSTALL) -d ${PREFIX}/java/oro-server/lib
	$(INSTALL) oro-server.jar ${PREFIX}/java/oro-server/lib
	$(INSTALL) -d ${PREFIX}/etc/oro-server
	$(INSTALL) etc/oro-server/*.conf ${PREFIX}/etc/oro-server
	$(INSTALL) oro-server ${PREFIX}/bin

distclean: clean doc-clean
	$(CLEAN) oro-server
	$(CLEAN) oro-server.jar
	$(CLEAN) *.log
	$(CLEAN) *.owl
	$(CLEAN) *.tar.gz

clean :
	$(CLEAN) $(BUILD_DIR)

doc:
	$(JAVADOC) -sourcepath $(SRC_DIR) \
	-overview $(SRC_DIR)/overview/overview.html \
	-classpath $(CLASSPATH) \
	-d $(DOC_DIR) \
	-use \
	-windowtitle $(WINDOWTITLE) \
	-doctitle $(HEADER) \
	-header $(HEADER) \
	-bottom $(BOTTOM) \
	-group $(GROUPCORE) \
	-group $(GROUPBACKEND) \
	-group $(GROUPSERVER) \
	-group $(GROUPTESTS) \
	-link http://jena.sourceforge.net/javadoc \
	-J-Xmx180m \
	-stylesheetfile $(SRC_DIR)/javadoc.css \
	-subpackages $(BASE_PACKAGE) \
	-encoding utf8

install-doc: doc
	cd doc && ${CP} -r . ${PREFIX}/share/doc/oro-server

doc-clean:
	$(CLEAN) $(DOC_DIR)

test: oro-server
	$(JAVA) -classpath $(CLASSPATH):${PREFIX}/java/oro-server/lib/$<.jar -DORO_TEST_CONF=${PREFIX}/etc/oro-server/oro_test.conf junit.textui.TestRunner $(BASE_PACKAGE).tests.OpenRobotsOntologyTest

