#!/usr/bin/env bash

set -ex

: ${INTEGCB_LOCATION?"integcb location"}

cd $INTEGCB_LOCATION && \
./cbd regenerate && \
./cbd start commondb && \
sudo -E ./cbd migrate && \
sudo rm -fr .schema/ && \
cd ..