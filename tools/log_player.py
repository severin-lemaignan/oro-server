#! /usr/bin/env python

import sys, re
from pyoro import Oro, OroServerError

from threading import Thread
from datetime import datetime, timedelta
from Queue import Queue

HOST = "localhost"
PORT = 6969

default_timedelta = timedelta(0,0,0,50) # Time delta used to 'separate' requests when no timestamp is available

threads_list = {}

nb_lines = 0
current_line = 0

start_time = None

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
            timedelta, req = self.req.get()

            while timedelta > (datetime.now() - start_time):
                    # Wait for the right time
                    pass

            current_line += 1
            print("[" + str(current_line * 100 / nb_lines) + "% - t+" + str(timedelta.seconds) + "." + str(int(timedelta.microseconds/1000)) + "sec - THREAD " + str(self.id) + " SENDING REQUEST " + req + "]")

            try:
                eval("self.oro." + req)
            except SyntaxError as e:
                print "[SYNTAX ERROR] -> " + e.text + " @ " + str(e.offset)
            except OroServerError as e:
                print "[ORO ERROR] " + str(e)

        print("[LEAVING THE THREAD " + str(self.id) + "]")
        self.oro.close()


line_nb=0

print "[PARSING THE LOG...]"
with open(sys.argv[1], 'r') as f:
    lines = f.readlines()
    for l in lines:

        cmd_line = re.search("(\d+ [.:\d]+) thread (\d+)",l)

        if cmd_line:
            time = datetime.strptime(cmd_line.group(1), "%Y%m%d %H:%M:%S.%f")

            if not start_time:
                # Initializes the start time at the first encountered
                # timestamp
                start_time = time

            thread_id = cmd_line.group(2)
        else:
            # Try to look for the thread number without timestamp
            cmd_line = re.search("(?<=thread )\d+",l)
            if cmd_line:
                thread_id = cmd_line.group(0)

                time = datetime.now() #+ default_timedelta * line_nb # Default date if not found in the log
                if not start_time:
                    # Initializes the start time at the first timestamp
                    start_time = datetime.now()
            else:
                #Not a log line with a request content
                continue

        line_nb += 1
        req = re.search("(?<=Got incoming request: ).+", l).group(0)
        if thread_id not in threads_list.keys():
            print "New thread " + thread_id
            threads_list[thread_id] = Client(Oro(HOST, PORT), thread_id)

        threads_list[thread_id].req.put((time - start_time, req))


nb_lines = line_nb
print "[PARSING OF THE LOG FINISHED. " + str(nb_lines) + " REQUESTS. STARTING EXECUTION]\n\n\n"

start_time = datetime.now()

for key, t in threads_list.items():
    t.start()

for key, t in threads_list.items():
    t.join()

print "[DONE. LOG REPLAYED.]"
