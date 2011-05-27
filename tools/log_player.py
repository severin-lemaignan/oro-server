#! /usr/bin/python

import sys, re
from pyoro import Oro, OroServerError

from threading import Thread
from Queue import Queue

HOST = "localhost"
PORT = 6969

threads_list = {}


class Client(Thread):
    def __init__(self, oro):
        print "[NEW THREAD CREATED]"
        Thread.__init__(self)
        self.req = Queue()
        self.running = True
        self.oro = oro
        self.start()

    def __del__(self):
        self.oro.close()

    def run(self):
        while (not self.req.empty() or self.running):
            req = self.req.get() #block until a request arrives
            eval("self.oro." + req)
        print("[LEAVING THE THREAD]")


i=0

with open(sys.argv[1], 'r') as f:
    lines = f.readlines()
    nb_line = len(lines)
    for l in lines:
        i += 1
        cmd_line = re.search("(?<=thread )\d+",l)
        try:
            thread_id = cmd_line.group(0)
            req = re.search("(?<=Got incoming request: ).+", l).group(0)
            if thread_id not in threads_list.keys():
                print "New thread " + thread_id
                threads_list[thread_id] = Client(Oro(HOST, PORT))
                
            print "(" + str(i*100/nb_line) +  "% - l."+str(i)+") Thread " + thread_id + " sending " + req
            try:
                print "[SENDING REQUEST " + req + "]"
                threads_list[thread_id].req.put(req)
            except SyntaxError as e:
                print "[SYNTAX ERROR] -> " + e.text + " @ " + str(e.offset)
            except OroServerError as e:
                print "[ORO ERROR] " + str(e)
        except AttributeError:
            #Not a log line with a request content
            pass

for key, t in threads_list.items():
    t.running = False

print "[PARSING OF THE LOG FINISHED]"
