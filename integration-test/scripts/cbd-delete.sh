#!/usr/bin/env bash

cd $INTEGCB_LOCATION

TRACE=1 ./cbd kill
TRACE=1 ./cbd delete --force