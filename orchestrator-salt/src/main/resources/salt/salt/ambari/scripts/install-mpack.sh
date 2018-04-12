#!/bin/bash
set -ex

echo yes | ambari-server install-mpack --mpack={{ mpack.mpackUrl }} --verbose
echo "$(date +%Y-%m-%d:%H:%M:%S) {{ mpack.mpackUrl }}" >> /var/mpack_installed
