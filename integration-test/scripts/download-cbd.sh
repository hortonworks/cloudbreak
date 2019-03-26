#!/usr/bin/env bash

: ${INTEGCB_LOCATION?"integcb location"}
cd $INTEGCB_LOCATION

os=$(uname)
latest_tag=$(git tag --points-at HEAD | sort -n | tail -1)
if ! [[ $latest_tag ]]; then
    latest_tag=$(git describe --abbrev=0 --tags)
fi
state=$(echo $latest_tag | cut -d '-' -f 2)
if [[ ! $latest_tag || "$state" = *"dev"* ]]; then
    branch="master"
else
    version=$(echo $latest_tag | cut -d '-' -f 1)
    branch="rc-$(echo $version | cut -d '.' -f 1,2)"
fi
echo -e "\n\033[1;96m--- build latest cbd: $branch for $os\033[0m\n"

rm_flag=""
if ! [[ "$CIRCLECI" ]]; then
    rm_flag="--rm"
fi

docker volume rm cbd-source 2>/dev/null || :
docker volume create --name cbd-source 1>/dev/null
docker run $rm_flag --user="0" --entrypoint init.sh -v cbd-source:/var/workspace jpco/git:1.0 https://github.com/hortonworks/cloudbreak-deployer.git $branch cloudbreak-deployer
docker run $rm_flag --user="0" -e GOPATH=/usr -v cbd-source:/usr/src/github.com/hortonworks -w /usr/src/github.com/hortonworks/cloudbreak-deployer golang:1.12 make bindata
docker run $rm_flag --user="0" -e GOPATH=/usr -v cbd-source:/usr/src/github.com/hortonworks -w /usr/src/github.com/hortonworks/cloudbreak-deployer golang:1.12 make build
docker run $rm_flag --user="0" -v cbd-source:/var/workspace jpco/git:1.0 cat /var/workspace/cloudbreak-deployer/build/${os}/cbd > cbd
if [[ $rm_flag ]]; then
    docker volume rm cbd-source 1>/dev/null || :
fi

chmod +x cbd
cbd_version=$(./cbd --version)
echo -e "$cbd_version"
