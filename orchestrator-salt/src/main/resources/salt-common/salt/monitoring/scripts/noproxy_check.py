#!/usr/bin/env python3
import sys
import os
from urllib.request import proxy_bypass
from urllib.parse import urlparse

def skip_proxy(netloc, no_proxy):
  if no_proxy:
    os.environ["NO_PROXY"]=no_proxy
    no_proxy = no_proxy.replace(' ', '').split(',')
    for host in no_proxy:
        if netloc.endswith(host) or netloc.split(':')[0].endswith(host):
            return True
    if proxy_bypass(netloc):
        return True
  return False

def main(args):
    if (len(args) < 2):
        sys.stdout.write("false")
    url=args[0]
    no_proxy=args[1]
    skipProxy=skip_proxy(urlparse(url).netloc, no_proxy)
    if skipProxy:
        sys.stdout.write("true")
    else:
        sys.stdout.write("false")

if __name__ == "__main__":
    main(sys.argv[1:])
