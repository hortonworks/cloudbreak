#!/bin/bash
METADATA_HASH=hash123
MYDOMAIN=test.kom
NODE_PREFIX=testamb

: ${NODE_PREFIX=amb}
: ${MYDOMAIN:=mycorp.kom}
: ${IMAGE:="sequenceiq/ambari"}
