#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}
: ${CB_TARGET_BRANCH?"pull request target branch"}
: ${CB_VERSION?"version of the latest build"}

cd $INTEGCB_LOCATION

os=$(uname)
echo -e "\n\033[1;96m--- build latest cbd: $CB_TARGET_BRANCH for $os\033[0m\n"
rm_flag=""
if ! [[ "$CIRCLECI" ]]; then
    rm_flag="--rm"
fi

docker volume rm cbd-source 2>/dev/null || :
docker volume create --name cbd-source 1>/dev/null
docker run $rm_flag --user="0" --entrypoint init.sh -v cbd-source:/var/workspace jpco/git:1.0 https://github.com/hortonworks/cloudbreak-deployer.git $CB_TARGET_BRANCH cloudbreak-deployer
docker run $rm_flag --user="0" -e GOPATH=/usr -v cbd-source:/usr/src/github.com/hortonworks -w /usr/src/github.com/hortonworks/cloudbreak-deployer golang:1.12 make bindata
docker run $rm_flag --user="0" -e GOPATH=/usr -v cbd-source:/usr/src/github.com/hortonworks -w /usr/src/github.com/hortonworks/cloudbreak-deployer golang:1.12 make build
docker run $rm_flag --user="0" -v cbd-source:/var/workspace jpco/git:1.0 cat /var/workspace/cloudbreak-deployer/build/${os}/cbd > cbd
if [[ $rm_flag ]]; then
    docker volume rm cbd-source 1>/dev/null || :
fi

chmod +x cbd
cbd_version=$(CB_VERSION)
mkdir etc
cp ../../mock-caas/src/test/resources/etc/license.txt etc/license.txt
echo -e "$cbd_version"
