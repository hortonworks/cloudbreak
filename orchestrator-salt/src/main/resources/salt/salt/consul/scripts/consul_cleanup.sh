#!/bin/bash

# remove all nodes that are failing before the new ones can join (Force Leave nodes which are not alive - status 1 and not left - status 3)
curl 127.0.0.1:8500/v1/agent/members | jq '.[] | select((.Status != 1) and (.Status != 3)) | .Name' | xargs -I@ /usr/local/bin/consul force-leave @