#!/bin/bash
set -x

MYDOMAIN=test.kom
NODE_PREFIX=testamb

: ${NODE_PREFIX=amb}
: ${MYDOMAIN:=mycorp.kom}
: ${IMAGE:="sequenceiq/ambari"}
