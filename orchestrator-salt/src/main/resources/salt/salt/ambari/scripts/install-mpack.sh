#!/bin/bash
set -ex

echo yes | ambari-server install-mpack --mpack={{ mpack }} --verbose
echo "$(date +%Y-%m-%d:%H:%M:%S) {{ mpack }}" >> /var/mpack_installed