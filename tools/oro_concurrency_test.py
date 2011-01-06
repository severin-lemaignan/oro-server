from pyoro import Oro
import random

def onSmthg(evt):
    pass

kb = Oro("localhost", 6969)

agent = "agent" + str(random.randint(0, 400))

#~ kb += agent + " rdf:type Agent"

kb += "JIDOKUKA_ROBOT rdf:type Robot"
kb += "ACHILE_HUMAN1 rdf:type Human"

kb.updateForAgent("JIDOKUKA_ROBOT",["ACHILE_HUMAN1 isLocated NEAR_LEFT","ACHILE_HUMAN1 isSitting true","ACHILE_HUMAN1 isVisible true","JIDOKUKA_ROBOT isSitting true","JIDOKUKA_ROBOT isVisible true","SPACENAVBOX isLocated FRONT","SPACENAVBOX isMoving false","SPACENAVBOX isVisible true"])
kb.updateForAgent("ACHILE_HUMAN1",["ACHILE_HUMAN1 isSitting true","ACHILE_HUMAN1 isVisible true","JIDOKUKA_ROBOT isLocated NEAR_FRONT","JIDOKUKA_ROBOT isSitting true","JIDOKUKA_ROBOT isVisible true","SPACENAVBOX isLocated LEFT","SPACENAVBOX isVisible true"])

#kb.subscribe(["instance8 relatesTo ?toto"], onSmthg)

#~ for i in range(random.randint(5, 10)):
#~ 
    #~ c = random.randint(0, 2)
    #~ if c == 1:
        #~ #kb += ["instance" + str(i) + " relatesTo instance" + str(i + 1)]
        #~ kb.addForAgent(agent, ["instance" + str(i) + " relatesTo instance" + str(i + 1)])
    #~ elif c == 2:
        #~ #kb.updateForAgent(agent, ["instance" + str(i) + " isVisible true"])
        #~ kb.updateForAgent("JIDOKUKA_ROBOT",["ACHILE_HUMAN1 isLocated NEAR_LEFT","ACHILE_HUMAN1 isSitting true","ACHILE_HUMAN1 isVisible true","JIDOKUKA_ROBOT isSitting true","JIDOKUKA_ROBOT isVisible true","SPACENAVBOX isLocated FRONT","SPACENAVBOX isMoving false","SPACENAVBOX isVisible true"])
    #~ else:
        #~ #kb.clear(["?toto relatesTo instance" + str(i + 1)])
        #~ kb.clearForAgent(agent, ["?toto relatesTo instance" + str(i + 1)])

kb.close()
