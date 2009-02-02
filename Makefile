#Makefile for liboro
# (c) LAAS-CNRS 2009

BUILD_DIR = build
JAR_DIR = .
SRC_DIR = src

ENTRYPOINT = laas.openrobots.ontology.connectors.OroServer

OPENROBOTS_BASE = /home/slemaign/openrobots
JENA_LIBS = $(OPENROBOTS_BASE)/java/Jena-2.5.7/lib
JYARP_LIB = $(OPENROBOTS_BASE)/java/libjyarp/lib
CLASSPATH = $(JENA_LIBS)/arq.jar:$(JENA_LIBS)/commons-logging-1.1.1.jar:$(JENA_LIBS)/concurrent.jar:$(JENA_LIBS)/icu4j_3_4.jar:$(JENA_LIBS)/jena.jar:$(JENA_LIBS)/log4j-1.2.12.jar:$(JENA_LIBS)/stax-api-1.0.jar:$(JENA_LIBS)/xercesImpl.jar:$(JENA_LIBS)/xml-apis.jar:$(JENA_LIBS)/junit.jar:$(JENA_LIBS)/iri.jar:$(JYARP_LIB)/libjyarp.jar

JAVAC = javac
JAR = jar

CLEAN = rm -rf
INSTALL = /usr/bin/install



all : oro-finalize

oro-finalize: oro-jar
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

distclean: clean
	$(CLEAN) start
	$(CLEAN) oro-server.jar


clean :
	$(CLEAN) $(BUILD_DIR)

doc:

