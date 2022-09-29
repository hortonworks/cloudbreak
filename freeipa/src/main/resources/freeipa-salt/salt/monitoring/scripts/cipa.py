#!/usr/bin/env python3

import os
import subprocess
import shutil
import sys

MAIN_PATCHED_SCRIPT = "/usr/local/lib/python" + str(sys.version_info.major) + "." + str(sys.version_info.minor) + "/site-packages/checkipaconsistency/main.py"
CIPA_METRICS_PATH="/var/lib/node_exporter/files/cipa.prom"
CIPA_CONFIG_PATH="/root/.config/checkipaconsistency"
CIPA_BIN_PATH="/usr/local/bin/cipa"

def run_cipa():
    global CIPA_BIN_PATH
    proc = subprocess.Popen("%s --output metrics" % CIPA_BIN_PATH,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            stdin=subprocess.PIPE,
                            shell=True)
    output, err = proc.communicate()
    return proc.returncode, output, err

def check_content(fname):
    with open(fname) as dataf:
        return any("--output" in line for line in dataf)

def main():
    global MAIN_PATCHED_SCRIPT, CIPA_METRICS_PATH, CIPA_CONFIG_PATH, CIPA_BIN_PATH
    if not os.path.exists(MAIN_PATCHED_SCRIPT) or not check_content(MAIN_PATCHED_SCRIPT):
        print("No cipa tool installed or OpenMetrics format output is not supported. Package location path: " + MAIN_PATCHED_SCRIPT)
        sys.exit(0)
    if not os.path.exists(CIPA_BIN_PATH) or not os.path.exists(CIPA_CONFIG_PATH):
        print("[SKIP] cipa tool is required (with root config).")
        sys.exit(0)
    ret_code, out, err= run_cipa()
    if ret_code != 0:
        print("Executing cipa command failed: %s" % str(err))
        sys.exit(1)
    current_pid=os.getpid()
    cipa_metrics_temp_path="%s.%s" % (CIPA_METRICS_PATH, current_pid)
    if out:
        out_decoded=out.decode()
        with open(cipa_metrics_temp_path, 'a') as f:
           f.write(out_decoded)
        if os.path.exists(cipa_metrics_temp_path):
            shutil.move(cipa_metrics_temp_path, CIPA_METRICS_PATH)

if __name__ == "__main__":
    main()