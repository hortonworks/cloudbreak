#!/bin/bash
METADATA_HASH=hash123
CONSUL_SERVER_COUNT=3
MYDOMAIN=test.kom
METADATA_ADDRESS=http://cloudbreak.sequenceiq.com
NODE_PREFIX=testamb

: ${NODE_PREFIX=amb}
: ${MYDOMAIN:=mycorp.kom}
: ${IMAGE:="sequenceiq/ambari"}
