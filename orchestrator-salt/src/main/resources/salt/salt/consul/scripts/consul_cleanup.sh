#!/bin/bash

# remove all nodes that are failing before the new ones can join
curl 127.0.0.1:8500/v1/agent/members | jq '.[] | select(.Status != 1) | .Name' | xargs -I@ /usr/local/bin/consul force-leave @