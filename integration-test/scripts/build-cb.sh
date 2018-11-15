#!/usr/bin/env bash

echo -e "\n\033[1;96m--- build cloudbreak\033[0m\n"
../gradlew -p ../ build -x test -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest

echo -e "\n\033[1;96m--- copy mock caas to it\033[0m\n"
cp ../mock-caas/build/libs/mock-caas.jar build/libs/mock-caas.jar
