#!/bin/bash

patch -b /usr/hdp/share/hst/hst-agent/lib/hst_agent/security.py /etc/smartsense/conf/security.py.diff
hst reset-agent -q