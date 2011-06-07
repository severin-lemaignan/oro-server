#! /usr/bin/env python

import sys, re
from pyoro import Oro, OroServerError

from threading import Thread
from Queue import Queue

HOST = "localhost"
PORT = 6969

threads_list = {}

nb_lines = 0
current_line = 0

class Client(Thread):
    def __init__(self, oro, id):
        print "[NEW THREAD CREATED]"
        Thread.__init__(self)
        self.req = Queue()
        self.oro = oro
        self.id = id

    def run(self):
        global current_line
        while (not self.req.empty()):
            req = self.req.get() #block until a request arrives
            current_line += 1
            print("[" + str(current_line * 100 / nb_lines) + "% - THREAD " + str(self.id) + " SENDING REQUEST " + req + "]")
            eval("self.oro." + req)
        print("[LEAVING THE THREAD " + str(self.id) + "]")
        self.oro.close()


i=0

with open(sys.argv[1], 'r') as f:
    lines = f.readlines()
    nb_lines = len(lines)
    for l in lines:
        i += 1
        cmd_line = re.search("(?<=thread )\d+",l)
        try:
            thread_id = cmd_line.group(0)
            req = re.search("(?<=Got incoming request: ).+", l).group(0)
            if thread_id not in threads_list.keys():
                print "New thread " + thread_id
                threads_list[thread_id] = Client(Oro(HOST, PORT), thread_id)

            try:
                print "(" + str(i*100/nb_lines) +  "% - l."+str(i)+") Adding [" + req + "] to thread " + thread_id
                threads_list[thread_id].req.put(req)
            except SyntaxError as e:
                print "[SYNTAX ERROR] -> " + e.text + " @ " + str(e.offset)
            except OroServerError as e:
                print "[ORO ERROR] " + str(e)
        except AttributeError:
            #Not a log line with a request content
            pass

print "[PARSING OF THE LOG FINISHED. WAITING FOR COMPLETION]\n\n\n"

for key, t in threads_list.items():
    t.start()

for key, t in threads_list.items():
    t.join()

print "[DONE. LOG REPLAYED.]"
