#!/usr/bin/env bash

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
    -v "${HOME}/project/build/Linux":/usr/local/bin \
    -v $(pwd):/work \
    halmy/aruba-rspec /work/scripts/integration-test-aruba.sh
RESULT=$?

echo

exit $RESULT