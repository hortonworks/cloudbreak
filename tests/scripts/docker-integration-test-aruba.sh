#!/usr/bin/env bash

: ${BASE_URL:=https://127.0.0.1}

export BASE_URL=$BASE_URL

echo
echo "Runnning integration tests in container"
echo "======================================="
#docker-compose run test

echo "Refresh the Test Runner Docker image"
docker pull halmy/aruba-rspec:1.0

export TEST_CONTAINER_NAME=cli-integration-runner

echo "Path is on CircleCI: " $(pwd)
echo "CircleCI Home is: " "${HOME}"

docker run -it \
    --privileged \
    --rm \
    --name $TEST_CONTAINER_NAME \
    --net=host \
    -v $(dirname "$(pwd)")"/build/Linux":/usr/local/bin \
    -v $(pwd):/work \
    halmy/aruba-rspec:1.0 /work/scripts/integration-test-aruba.sh
RESULT=$?

cat aruba/test-result.html | grep -e ".*script.*totals.*failure" | cut -d ',' -f 2 | grep -e " 0"
