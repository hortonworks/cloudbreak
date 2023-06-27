#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}
: ${CB_TARGET_BRANCH?"pull request target branch"}
: ${CB_VERSION?"version of the latest build"}

cd $INTEGCB_LOCATION

os=$(uname)
echo -e "\n\033[1;96m--- build latest cbd: $CB_TARGET_BRANCH for $os\033[0m\n"
rm_flag=""
if ! [[ "$CIRCLECI" && "$CIRCLECI" == "true" ]]; then
    rm_flag="--rm"
fi

docker volume rm cbd-source 2>/dev/null || :
docker volume create --name cbd-source 1>/dev/null
docker run $rm_flag --user="0" --entrypoint init.sh -v cbd-source:/var/workspace docker-private.infra.cloudera.com/cloudera_thirdparty/jpco/git:1.0 https://github.infra.cloudera.com/cloudbreak/cloudbreak-deployer.git $CB_TARGET_BRANCH cloudbreak-deployer
docker run $rm_flag --user="0" -e GOPATH=/usr -v cbd-source:/usr/src/github.infra.cloudera.com/cloudbreak -w /usr/src/github.infra.cloudera.com/cloudbreak/cloudbreak-deployer docker-private.infra.cloudera.com/cloudera_thirdparty/golang:1.20.1 make build
docker run $rm_flag --user="0" -v cbd-source:/var/workspace docker-private.infra.cloudera.com/cloudera_thirdparty/jpco/git:1.0 cat /var/workspace/cloudbreak-deployer/build/${os}/cbd > cbd
if [[ $rm_flag ]]; then
    docker volume rm cbd-source 1>/dev/null || :
fi

set -x
chmod +x cbd
mkdir etc

if [ -n "$CM_LICENSE_FILE" ] ; then
    echo "CM_LICENSE_FILE variable is not empty, creating license file from it's content under etc/license.txt";
    cp -Rv $CM_LICENSE_FILE etc/
    sudo chmod -R 755 ./etc
else
    echo "CM_LICENSE_FILE variable is empty, copying the license from mock-thunderhead source"
    cp ../../mock-thunderhead/src/test/resources/etc/license.txt etc/license.txt
fi