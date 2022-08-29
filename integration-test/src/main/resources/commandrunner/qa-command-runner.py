#!/bin/env python3

import sys
import subprocess
import optparse
import json
import os
import shutil

SALT_CMD_PREFIX = '/opt/salt_*/bin/salt'

def run_command(cmd):
    proc = subprocess.Popen(cmd,
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            shell=True,
                            universal_newlines=True)
    std_out, std_err = proc.communicate()
    return proc.returncode, std_out, std_err

def read_json_line_file(file):
    data = {}
    with open(file, 'r') as json_file:
        json_list = list(json_file)
    for json_str in json_list:
        if json_str:
            json_data = json.loads(json_str)
            for key, json_dict_elem in json_data.items():
                data[key] = json_dict_elem
    return data

def execute_local_commands(config):
    results = list()
    for c in config:
        result = {}
        description = c["description"]
        command = c["command"]
        code, out, err = run_command(command)
        result["command"] = c["command"]
        result["description"] = c["description"]
        result["code"] = code
        result["out"] = out.rstrip() if out else out
        result["err"] = err.rstrip() if err else err
        results.append(result)
    return results


def execute_salt_commands(command_file_location, config, hosts, host_groups):
    global SALT_CMD_PREFIX
    ping_out_file = "/tmp/qa_test_ping.json"
    test_cmd = "%s '*' test.ping --out=json --out-file=%s --out-indent=-1" % (SALT_CMD_PREFIX, ping_out_file)
    available_nodes = []
    test_code, _, _ = run_command(test_cmd)
    test_json = read_json_line_file(ping_out_file)
    if test_json:
        for key in test_json:
            if str(test_json[key]) == "True":
                available_nodes.append(key)
    qa_path = '/srv/salt/qa'
    if os.path.exists('/srv/salt') and not os.path.exists(qa_path):
        os.mkdir(qa_path)
    script_file = os.path.abspath(__file__)
    script_file_basename = os.path.basename(script_file)
    command_file_basename = os.path.basename(command_file_location)
    try:
        shutil.copy(command_file_location, os.path.join(qa_path, command_file_basename))
    except shutil.SameFileError:
        pass
    try:
        shutil.copy(script_file, os.path.join(qa_path, script_file_basename))
    except shutil.SameFileError:
        pass
    available_nodes_str = ",".join(available_nodes)
    cmd_copy_script_file = "%s -L %s cp.get_file salt:///qa/%s /tmp/%s" % (
    SALT_CMD_PREFIX, available_nodes_str, script_file_basename, script_file_basename)
    cmd_copy_command_file = "%s -L %s cp.get_file salt:///qa/%s /tmp/%s" % (
    SALT_CMD_PREFIX, available_nodes_str, command_file_basename, command_file_basename)
    run_command(cmd_copy_script_file)
    run_command(cmd_copy_command_file)
    final_output = '/tmp/qa_test_results.json'
    run_all_commands_cmd = \
        "%s -L %s cmd.run 'python3 /tmp/%s -c /tmp/%s -l' --out=json --out-file=%s --out-indent=-1" % (
    SALT_CMD_PREFIX, available_nodes_str, script_file_basename, command_file_basename, final_output)
    run_command(run_all_commands_cmd)
    final_json = read_json_line_file(final_output)
    outputs = {}
    if final_json:
        for key in final_json:
            outputs[key] = json.loads(final_json[key])
    return outputs

if __name__ == "__main__":
    main_result={}
    try:
        parser = optparse.OptionParser("usage: %prog [options]")
        parser.add_option("-c", "--config", dest="config", default=None, type="string",
                      help="Configuration file for executing salt commands.")
        parser.add_option("--hosts", dest="hosts", default=None, type="string", help="Comma separated hosts filter,")
        parser.add_option("--host-groups", dest="host_groups", default=None, type="string",
                      help="Comma separated host groups filter.")
        parser.add_option("-l", "--local", action="store_true", dest="local", help="Run commands locally,")
        (options, args) = parser.parse_args()
        if not options.config:
            raise ValueError("Configuration option -c / --config is required!")
        hosts_arr = options.hosts.split(",") if options.hosts else list()
        host_groups_arr = options.host_groups.split(",") if options.host_groups else list()
        if not os.path.exists(options.config):
            raise ValueError("Configuration file %s does not exist!" % options.config)
        config = {}
        with open(options.config) as f:
            config = json.load(f)
        if options.local:
            local_results=execute_local_commands(config)
            print(json.dumps(local_results))
            sys.exit(0)
        else:
            responses=execute_salt_commands(options.config, config, hosts_arr, host_groups_arr)
            main_result['responses']=responses
            main_result['code']=0
    except Exception as e:
        main_result['code']=1
        main_result['err']=str(e)
        main_result['responses']={}
    print(json.dumps(main_result))
