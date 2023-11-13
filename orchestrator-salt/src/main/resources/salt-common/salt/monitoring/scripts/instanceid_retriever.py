#!/bin/python3

import sys
import urllib.request

def get_instance_id(cloud_provider):
    if cloud_provider.upper() == "AWS":
        token=execute_request('http://169.254.169.254/latest/api/token', {'Connection':'close', 'Metadata': 'true', 'X-aws-ec2-metadata-token-ttl-seconds': '21600'}, 'PUT')
        return execute_request('http://169.254.169.254/latest/meta-data/instance-id', {'Connection':'close', 'Metadata': 'true', 'X-aws-ec2-metadata-token': token }, 'GET')
    elif cloud_provider.upper() == "AZURE":
        return execute_request('http://169.254.169.254/metadata/instance/compute/vmId?api-version=2017-08-01&format=text', {'Connection':'close', 'Metadata': 'true'}, 'GET')
    elif cloud_provider.upper() == "GCP":
        return execute_request('http://metadata.google.internal/computeMetadata/v1/instance/name', {'Connection':'close', 'Metadata-Flavor': 'Google'}, 'GET')
    return ""

def execute_request(url, headers, method):
    result=""
    try:
        req = urllib.request.Request(url, headers=headers, method=method)
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