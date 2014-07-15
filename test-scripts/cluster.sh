#!/bin/bash
echo "Testing cluster REST API"

echo "Create stack ..."
STACK_ID=$(curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d @json/stack.json http://localhost:8080/stacks | jq '.id')
echo "Created stack with id $STACK_ID"

# if stack does not exist
CLUSTER_ID=$(curl -u cbuser@sequenceiq.com:test123 -sX POST -H "Content-Type:application/json" -d @json/cluster.json http://localhost:8080/stacks/$STACK_ID/cluster | jq '.id')
echo "Created cluster with id $CLUSTER_ID"