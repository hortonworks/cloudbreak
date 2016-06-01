#!/bin/bash

URL="https://gist.githubusercontent.com/seanorama/bbe936cff511d8e5b98f1c8b6c155f55/raw/5446e0747e0a6e99b49c881e23a70748aec97b19/security.py.diff"
curl -sSL "${URL}" | patch -b /usr/hdp/share/hst/hst-agent/lib/hst_agent/security.py
hst reset-agent -q