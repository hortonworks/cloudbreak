#!/bin/bash
set -x

echo yes | ambari-server install-mpack --mpack={{ mpack }} --verbose
echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/hdf_mpack_installed