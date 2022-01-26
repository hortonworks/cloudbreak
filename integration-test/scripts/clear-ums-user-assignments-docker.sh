#!/bin/bash -ex

: ${IMAGE_NAME:=python:3}

readonly CONTAINER_NAME=clear-ums-user-assignments

container-remove-exited() {
    declare desc="Remove Exited or Dead containers"
    local exited_containers=$(docker ps -a -f status=exited -f status=dead -q)

    if [[ -n "$exited_containers" ]]; then
        echo "Remove Exited or Dead docker containers"
        docker rm $exited_containers;
    else
        echo "There is no Exited or Dead container"
    fi
}

container-remove-stuck() {
    declare desc="Checking $CONTAINER_NAME container is running"

    if [[ "$(docker inspect -f {{.State.Running}} $CONTAINER_NAME 2> /dev/null)" == "true" ]]; then
        echo "Delete the running " $CONTAINER_NAME " container"
        docker rm -f $CONTAINER_NAME
    fi
}

clear-ums-user-assignments() {
  echo "Pull $IMAGE_NAME"
  docker pull $IMAGE_NAME

	docker run \
      -i \
      --rm \
      --name $CONTAINER_NAME \
    	-v $WORKSPACE:/prj:rw \
    	$IMAGE_NAME /bin/bash -c "set -o pipefail ; set -ex && env && cd /prj/integration-test && eval ./scripts/clear-ums-user-assignments.sh | tee clear-ums-user-assignments.log"
    	RESULT=$?
}

main() {
  container-remove-exited
  container-remove-stuck
  clear-ums-user-assignments
  exit $RESULT
}

main "$@"