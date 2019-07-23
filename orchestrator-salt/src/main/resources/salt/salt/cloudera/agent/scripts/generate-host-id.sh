#!/bin/bash -x

# create uuid file if not exists
FILE=/var/lib/cloudera-scm-agent/uuid
if [[ ! -f "$FILE" ]]; then
    mkdir -p /var/lib/cloudera-scm-agent
    touch /var/lib/cloudera-scm-agent/uuid
fi

# generate and trim hash by hostname into the uuid file
echo -n $(echo $(hostname -f) | md5sum | awk '{print $1}' | cut -d$'\n' -f1) > /var/lib/cloudera-scm-agent/uuid