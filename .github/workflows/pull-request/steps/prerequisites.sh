#!/bin/bash -ex

get_latest_version() {
  GIT_ACTIVE_BRANCH=$BRANCH
  if [[ $GIT_ACTIVE_BRANCH = master* ]]
  then
  LATEST_VERSION=$(curl "http://release.infra.cloudera.com/hwre-api/getreleaseversion?stack=CB&releaseline=master" | jq -r '.version')
  LATEST_TAG=$(curl "http://release.infra.cloudera.com/hwre-api/listbuilds?stack=CB&release=${LATEST_VERSION}&type=dev" | jq -r '.latest_build_version')
  elif [[ $GIT_ACTIVE_BRANCH = rc-* ]]
  then
  CUT_VERSION=$(echo $GIT_ACTIVE_BRANCH | cut -f 2 -d '-')
  LATEST_TAG=$(curl "http://release.infra.cloudera.com/hwre-api/listbuilds?stack=CB&release=${CUT_VERSION}.0&type=rc" | jq -r '.latest_build_version')
  else
  CUT_VERSION=$(echo $GIT_ACTIVE_BRANCH | cut -f 2 -d '-')
  LATEST_TAG=$(curl "http://release.infra.cloudera.com/hwre-api/listbuilds?stack=CB&release=${CUT_VERSION}" | jq -r '.latest_build_version')
  fi
  echo $LATEST_TAG
}

version() {
  echo "$@" | awk -F. '{ printf("%d%03d%03d%03d\n", $1,$2,$3,$4); }';
}