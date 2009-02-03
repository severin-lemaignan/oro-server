#Makefile for oro-server
# (c) LAAS-CNRS 2009

BUILD_DIR = build
JAR_DIR = .
SRC_DIR = src
DOC_DIR = doc

BASE_PACKAGE = laas.openrobots.ontology
ENTRYPOINT = $(BASE_PACKAGE).connectors.OroServer

OPENROBOTS_BASE = /home/slemaign/openrobots
JENA_LIBS = $(OPENROBOTS_BASE)/java/Jena-2.5.7/lib
JYARP_LIB = $(OPENROBOTS_BASE)/java/libjyarp/lib
CLASSPATH = $(JENA_LIBS)/arq.jar:$(JENA_LIBS)/commons-logging-1.1.1.jar:$(JENA_LIBS)/concurrent.jar:$(JENA_LIBS)/icu4j_3_4.jar:$(JENA_LIBS)/jena.jar:$(JENA_LIBS)/log4j-1.2.12.jar:$(JENA_LIBS)/stax-api-1.0.jar:$(JENA_LIBS)/xercesImpl.jar:$(JENA_LIBS)/xml-apis.jar:$(JENA_LIBS)/junit.jar:$(JENA_LIBS)/iri.jar:$(JYARP_LIB)/libjyarp.jar

JAVAC = javac
JAVADOC = javadoc
JAR = jar

CLEAN = rm -rf
INSTALL = /usr/bin/install

########## Variables for Javadoc documentation ##########
WINDOWTITLE = 'ORO: the OpenRobots Ontology - Server documentation'
HEADER = '<b>ORO: the OpenRobots Ontology</b><br/><font size="-1">Server documentation</font>'
BOTTOM = '<font size="-1">ORO is a part of the <a href="https://softs.laas.fr/openrobots/wiki/">OpenRobots</a> framework.<br/><br><a href="mailto:openrobots@laas.fr">openrobots@laas.fr</a><br/>LAAS-CNRS 2009</font>'
GROUPCORE = "Core Packages" "$(BASE_PACKAGE)*"
GROUPSERVER  = "Server Packages" "$(BASE_PACKAGE).connectors*"
GROUPTESTS = "Tests Packages" "$(BASE_PACKAGE).tests*"
##########################################################

all : oro-server doc

oro-serveroro-jar
	echo -e '#!/bin/sh\njava -Djava.library.path=$(OPENROBOTS_BASE)/lib -jar oro-server.jar' > $(JAR_DIR)/start
	chmod +x $(JAR_DIR)/start

oro-jar: oro-build
	echo -e "Class-Path: \n `echo $(CLASSPATH) | sed 's/:/ \n /g'`" > MANIFEST.MF
	$(JAR) cfme $(JAR_DIR)/oro-server.jar MANIFEST.MF $(ENTRYPOINT) -C $(BUILD_DIR) .
	$(CLEAN) MANIFEST.MF

oro-build :
	$(INSTALL) -d $(BUILD_DIR)
	$(JAVAC) -classpath $(CLASSPATH) -d $(BUILD_DIR) `find -name "*.java"`


install :

distclean: clean doc-clean
	$(CLEAN) start
	$(CLEAN) oro-server.jar


clean :
	$(CLEAN) $(BUILD_DIR)

doc-clean:
	$(CLEAN) $(DOC_DIR)

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
	-group $(GROUPSERVER) \
	-group $(GROUPTESTS) \
	-J-Xmx180m \
	-stylesheetfile $(SRC_DIR)/javadoc.css \
	-subpackages $(BASE_PACKAGE)

