#!/usr/bin/env bash

echo -e "\n\033[1;96m--- Copy ../core/build/libs/cloudbreak.jar to test-image directory\033[0m\n"
cp ../core/build/libs/cloudbreak.jar test-image
echo -e "\n\033[1;96m--- Build dev cloudbreak test image\033[0m\n"
docker build -t hortonworks/cloudbreak:dev test-image