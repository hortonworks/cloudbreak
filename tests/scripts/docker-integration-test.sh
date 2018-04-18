#!/usr/bin/env bash

echo
echo "Runnning integration tests in container"
echo "======================================="
#docker-compose run test

echo "Refresh the Test Runner Docker image"
docker pull afarsang/docker-bats-jq

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
    afarsang/docker-bats-jq /work/scripts/integration-test.sh
RESULT=$?

echo
echo "Failed tests:"
echo "============="
grep "not ok" test-result.tap || { echo " none " ; exit ; }
echo

exit $RESULT
