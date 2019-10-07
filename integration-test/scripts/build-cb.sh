#!/usr/bin/env bash

date
echo -e "\n\033[1;96m--- build cloudbreak\033[0m\n"
../gradlew -p ../ clean build -x test -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest