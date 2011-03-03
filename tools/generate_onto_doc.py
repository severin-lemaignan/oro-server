#! /usr/bin/env python

import pyoro

oro = pyoro.Oro("localhost", 6969)

def print_onto_line(s,p,o):
     print("|| a " + oro.getLabel(s) + "<<BR>>(''" + s + "'') " + \
           "|| '''" + oro.getLabel(p) + "''' <<BR>>(''" + p + "'') " + \
           "|| a " + oro.getLabel(o) + "<<BR>>(''" + o + "'') " + \
           "||<bgcolor=\"#ff420e\"> ||")


print("=== Properties ===")
print("||<bgcolor=\"#94bd5e\"> Subject  ||<bgcolor=\"#9999ff\"> Predicate ||<bgcolor=\"#ffff66\"> Object || Updated by SPARK? ||")
for p in oro["* rdf:type owl:ObjectProperty"]:
    for r in oro[p + " rdfs:range *"]:
        for d in oro[p + " rdfs:domain *"]:
           print_onto_line(d,p,r) 


for p in oro["* rdf:type owl:DatatypeProperty"]:
    for r in oro[p + " rdfs:range *"]:
        for d in oro[p + " rdfs:domain *"]:
           print_onto_line(d,p,r) 

oro.close()
