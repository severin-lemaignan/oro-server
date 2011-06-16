#! /usr/bin/env python

import sys, re
from pyoro import Oro, OroServerError

from threading import Thread
from datetime import datetime, timedelta
from Queue import Queue

HOST = "localhost"
PORT = 6969

threads_list = {}

nb_lines = 0
current_line = 0

start_time = None
end_time = None

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

            current_timedelta = datetime.now() - start_time
            while timedelta > current_timedelta:
                    # Wait for the right time
                    current_timedelta = datetime.now() - start_time

            delay = current_timedelta - timedelta
            current_line += 1
            print("[" + str(current_line * 100 / nb_lines) + \
                  "% - t+" + str(timedelta) + \
                  " - THREAD " + str(self.id) + " SENDING REQUEST " + req + "]")

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
time = None

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

                time = datetime.now() # Default date if not found in the log
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

if nb_lines == 0:
    print("No ORO requests found. Are you sure you're parsing a ORO-server log?")
    sys.exit(0)

end_time = time

timespan = end_time - start_time
print "[PARSING OF THE LOG FINISHED. " + str(nb_lines) + " REQUESTS IN " + \
       str(timespan) + \
       "sec. STARTING EXECUTION]\n\n\n"

start_time = datetime.now()
for key, t in threads_list.items():
    t.start()

for key, t in threads_list.items():
    t.join()

end_time = datetime.now()

timespan_replay = end_time - start_time
delay = timespan_replay - timespan

slowdown = (timespan_replay.seconds * 1000000 + timespan_replay.microseconds) / (timespan.seconds * 1000000 + timespan.microseconds)
print "[DONE. LOG REPLAYED IN " + \
       str(timespan_replay) + ". Delta = " + str(delay) + " - " + str(int((slowdown - 1) * 100)) + "% slower]"

