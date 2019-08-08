#!/usr/bin/env bash

echo -e "\n\033[1;96m--- build cloudbreak\033[0m\n"
if [[ "$VERSION" ]]; then
echo "Do not need to build cb artifact because that is exist already"
else
../gradlew -p ../ clean build -x test -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest
fi