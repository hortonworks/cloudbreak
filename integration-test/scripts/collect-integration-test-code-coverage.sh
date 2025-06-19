#!/usr/bin/env bash

set -ex

: ${INTEGCB_LOCATION?"integcb location"}

cd $INTEGCB_LOCATION;

docker login --username=$DOCKERHUB_USERNAME --password=$DOCKERHUB_PASSWORD

if [[ $(docker ps -q -f "name=cbreak") ]]; then
  echo "Stopping cbreak containers"
  docker stop $(docker ps -q -f "name=cbreak")
  sleep 5
fi

echo -e "\n\033[1;96m--- Collect jacoco reports\033[0m\n"
mkdir -p ./jacoco-reports
for container in $(docker ps -aq -f "name=cbreak"); do
  container_name=$(docker inspect -f '{{.Name}}' "$container" | sed 's/\///')
  echo -e "Collecting jacoco report from container: $container_name"
  if docker cp "$container":/jacoco.exec ./jacoco-reports/"${container_name}_jacoco.exec" 2>/dev/null; then
    echo "Jacoco report collected successfully from container: $container_name"
  else
    echo "Jacoco report not found in container: $container_name"
  fi
done