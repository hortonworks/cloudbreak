#!/usr/bin/env bash

if [ -z "$DOCKER_MACHINE" ]; then
    docker-machine env $(DOCKER_MACHINE) | awk '{ gsub ("\"", ""); print}' > docker_env.tmp
    echo TEST_ENV='docker-machine' >> docker_env.tmp
else
    echo TEST_ENV='boot2docker' > docker_env.tmp
fi
