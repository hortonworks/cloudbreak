#!/bin/python3

import sys
import urllib.request

def get_instance_id(cloud_provider):
    if cloud_provider.upper() == "AWS":
        return execute_request('http://169.254.169.254/latest/meta-data/instance-id', {'Connection':'close', 'Metadata': 'true'})
    elif cloud_provider.upper() == "AZURE":
        return execute_request('http://169.254.169.254/metadata/instance/compute/vmId?api-version=2017-08-01&format=text', {'Connection':'close', 'Metadata': 'true'})
    elif cloud_provider.upper() == "GCP":
        return execute_request('http://metadata.google.internal/computeMetadata/v1/instance/name', {'Connection':'close', 'Metadata-Flavor': 'Google'})
    return ""

def execute_request(url, headers):
    result=""
    try:
        req = urllib.request.Request(url, headers=headers)
        return urllib.request.urlopen(req, timeout=5).read().decode('utf-8')
    except:
        pass
    return result

def main(args):
    if (len(args) < 1):
        sys.exit(0)
    result=get_instance_id(args[0])
    sys.stdout.write(result)

if __name__ == "__main__":
    main(sys.argv[1:])