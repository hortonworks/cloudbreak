#!/usr/bin/env bash

set -o pipefail

: ${INTEGCB_LOCATION?"integcb location"}
: ${CB_TARGET_BRANCH?"pull request target branch"}
: ${CB_VERSION?"version of the latest build"}

cd $INTEGCB_LOCATION

os=$(uname -s)
arch=$(uname -m)
rm_flag="--rm"
GIT_IMAGE=docker-private.infra.cloudera.com/cloudera_thirdparty/jpco/git:1.0
GO_IMAGE=docker-private.infra.cloudera.com/cloudera_thirdparty/golang:1.20.1
GITHUB_API=${GITHUB_API:-https://github.infra.cloudera.com/api/v3}
GITHUB_BASE=${GITHUB_BASE:-https://github.infra.cloudera.com}
CBD_REPO=${CBD_REPO:-cloudbreak/cloudbreak-deployer}

# CB-33669: fetch a prebuilt cbd instead of compiling it from source every run.
# cbd is published as a GitHub release (a per-OS tgz containing a single `cbd`
# binary) by the cloudbreak-deployer release job, versioned per branch line
# (e.g. 2.111.0-bNN). Not every build is published, so we pick the newest
# *available* release on the target branch's line. CB_VERSION (already resolved
# by get_latest_version in CI, the same tag the deployer release job stamps)
# gives us that line via its major.minor.patch prefix. On any failure we fall
# back to building from source -- never a hard failure. This turns a ~63s docker
# build into a few-second download on the common path.
line=${CB_VERSION%%-*}

# cloudbreak-deployer is a public repo, so the releases API and asset downloads
# work without authentication -- no token needed.
download_prebuilt_cbd() {
    [ -n "$line" ] || { echo "cbd: no version line from CB_VERSION='$CB_VERSION'" >&2; return 1; }
    local suffix="_${os}_${arch}.tgz"
    local tag
    tag=$(curl -sf "${GITHUB_API}/repos/${CBD_REPO}/releases?per_page=100" \
        | jq -r --arg line "$line" --arg suf "$suffix" \
            '[ .[] | select(.tag_name | startswith($line + "-b"))
                   | select([.assets[].name] | any(endswith($suf)))
                   | .tag_name ]
             | sort_by(ltrimstr($line + "-b") | tonumber) | last // empty')
    if [ -z "$tag" ]; then
        echo "cbd: no downloadable release with a *${suffix} asset on line ${line}" >&2
        return 1
    fi
    local url="${GITHUB_BASE}/${CBD_REPO}/releases/download/${tag}/cloudbreak-deployer_${tag}${suffix}"
    echo -e "\n\033[1;96m--- fetch prebuilt cbd ${tag} (${os}_${arch}) for branch ${CB_TARGET_BRANCH}\033[0m\n"
    curl -Lsf "$url" | tar -xz -C .
    [ -s cbd ]
}

build_cbd_from_source() {
    echo -e "\n\033[1;96m--- build cbd from source: ${CB_TARGET_BRANCH} for ${os}_${arch}\033[0m\n"
    docker volume rm cbd-source 2>/dev/null || :
    docker volume create --name cbd-source 1>/dev/null
    docker run $rm_flag --user="0" --entrypoint init.sh -v cbd-source:/var/workspace $GIT_IMAGE https://github.infra.cloudera.com/cloudbreak/cloudbreak-deployer.git $CB_TARGET_BRANCH cloudbreak-deployer
    docker run $rm_flag --user="0" -e GOPATH=/usr -v cbd-source:/usr/src/github.infra.cloudera.com/cloudbreak -w /usr/src/github.infra.cloudera.com/cloudbreak/cloudbreak-deployer $GO_IMAGE make build
    docker run $rm_flag --user="0" -v cbd-source:/var/workspace $GIT_IMAGE cat /var/workspace/cloudbreak-deployer/build/${os}_${arch}/cbd > cbd
    if [[ $rm_flag ]]; then
        docker volume rm cbd-source 1>/dev/null || :
    fi
}

if ! download_prebuilt_cbd; then
    echo "cbd: falling back to building from source" >&2
    build_cbd_from_source
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
