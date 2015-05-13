debug() {
  if [[ "$DEBUG" ]]; then
      echo "[DEBUG] $*" | gray 1>&2
  fi
}

info() {
    echo "$*" | green 1>&2
}

warn() {
    echo "[WARN] $*" | yellow 1>&2
}

error() {
    echo "[ERROR] $*" | red 1>&2
}

cbd-version() {
    declare desc="Displays the version of Cloudbrek Deployer"
    echo -n "local version:"
    local localVer=$(bin-version)
    echo "$localVer" | green

    echo -n "latest release:"
    local releaseVer=$(latest-version)
    echo "$releaseVer" | green

    if [ $(version-compare $localVer $releaseVer) -lt 0 ]; then
        warn "!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
        warn "Your version is outdated"
        warn "!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
        warn "Please update it by:"
        echo "  cbd update" | blue
    fi
}

cbd-update() {
    declare desc="Binary selfupdater. Either latest github release (default), or specific branch from CircleCI"

    if [[ "$1" ]]; then
        cbd-update-snap $1
    else
        cbd-update-release
    fi
}

cbd-update-release() {
    declare desc="Updates itself from github release"

    local binver=$(bin-version)
    local lastver=$(latest-version)
    local osarch=$(uname -sm | tr " " _ )
    debug $desc
    debug binver=$binver lastver=$lastver osarch=$osarch | gray

    if [[ ${binver} != ${lastver} ]]; then
        debug upgrade needed |yellow
        
        local url=https://github.com/sequenceiq/cloudbreak-deployer/releases/download/v${lastver}/cloudbreak-deployer_${lastver}_${osarch}.tgz
        info "Updating $EXECUTABLE from url: $url"
        curl -Ls $url | tar -zx -C /tmp
        mv /tmp/cbd $EXECUTABLE
        debug $EXECUTABLE is updated
    else
        debug you have the latest version | green
    fi
}

cbd-update-snap() {
    declare desc="Updates itself, from CircleCI branch artifact"
    declare branch=${1:?branch name is required}

    url=$(cci-latest sequenceiq/cloudbreak-deployer $branch)
    info "Update $EXECUTABLE from: $url"
    curl -Ls $url | tar -zx -C /tmp
    mv /tmp/cbd $EXECUTABLE
    debug $EXECUTABLE is updated
}

latest-version() {
  #curl -Ls https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/VERSION
  curl -I https://github.com/sequenceiq/cloudbreak-deployer/releases/latest 2>&1 \
      |sed -n "s/^Location:.*tag.v//p" \
      | sed "s/\r//"
}

init-profile() {
    declare desc="Creates Profile if missing"

    CBD_PROFILE="Profile"

    # if the profile exist
    if [ -f $CBD_PROFILE ]; then
        info "$CBD_PROFILE already exists, now you are ready to run:"
        echo "cbd generate" | blue
    else
        # if cbd runs on boot2docker (ie osx)
        if boot2docker version &> /dev/null; then
            if [[ "$(boot2docker status)" == "running" ]]; then
                echo "export PUBLIC_IP=$(boot2docker ip)" > $CBD_PROFILE
                echo "export PRIVATE_IP=$(boot2docker ip)" >> $CBD_PROFILE
            else
                echo "boot2docker isn't running, please start it, with the following 2 commands:" | red
                echo "boot2docker start" | blue
                echo '$(boot2docker shellinit)' | blue
            fi
        else
            # this is for linux

            # on amazon
            if curl -f 169.254.169.254/latest/ &>/dev/null ; then
                echo "export PUBLIC_IP=$(curl 169.254.169.254/latest/meta-data/public-hostname)" > $CBD_PROFILE
                #echo "export PRIVATE_IP=$(curl 169.254.169.254/latest/meta-data/local-ipv4)" >> $CBD_PROFILE
            fi

            # on gce
            if curl -f -H "Metadata-Flavor: Google" 169.254.169.254/computeMetadata/v1/ &>/dev/null ; then
                echo "export PUBLIC_IP=$(curl -f -H "Metadata-Flavor: Google" 169.254.169.254/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip)" > $CBD_PROFILE
            fi

            # if Profile is still not created, give some hint:
            if ! [ -f $CBD_PROFILE ]; then
                warn "We can not guess your PUBLIC_IP, please run the following command: (replace 1.2.3.4 with a real IP)"
                echo "echo export PUBLIC_IP=1.2.3.4 > $CBD_PROFILE" | blue
            fi
        fi
        exit 2
    fi
    
    doctor
}

load-profile() {

    CBD_PROFILE="Profile"
    if [ -f $CBD_PROFILE ]; then
        debug "Use profile: $CBD_PROFILE"
        module-load "$CBD_PROFILE"
        PROFILE_LOADED=true
    else
        debug "diollar1=$1"
        if [[ "$1" != "init" ]];then
            echo "!! No Profile found. Please initalize your 'Profile' with the init command." | red
            echo "cbd init" | blue
        fi
    fi
    
    if [[ "$CBD_DEFAULT_PROFILE" && -f "Profile.$CBD_DEFAULT_PROFILE" ]]; then
        CBD_PROFILE="Profile.$CBD_DEFAULT_PROFILE"

		module-load $CBD_PROFILE
		debug "Using profile $CBD_DEFAULT_PROFILE"
	fi
}

doctor() {
    declare desc="Deployer doctor: Checks your environment, and reports a diagnose."

    info "===> $desc"
    cbd-version
    if [[ "$(uname)" == "Darwin" ]]; then
        debug "checking boot2docker on OSX only ..."
        docker-check-boot2docker
    fi

    docker-check-version
    deployer-generate
}

cbd-find-root() {
    CBD_ROOT=$PWD
}

deployer-delete() {
    declare desc="Deletes yaml files, and all dbs"
    cloudbreak-delete-dbs
    deployer-delete-yamls
}

deployer-delete-yamls() {
    rm uaa.yml docker-compose.yml
}

deployer-generate() {
    declare desc="Generates docker-compose.yml and uaa.yml"

    compose-generate-yaml
    generate_uaa_config
}

main() {
	set -eo pipefail; [[ "$TRACE" ]] && set -x

    cbd-find-root
    deps-init
	color-init
    load-profile "$@"

    circle-init
    compose-init

    debug "CloudBreak Deployer $(bin-version)"

    cmd-export cmd-help help
    cmd-export cbd-version version
    cmd-export cbd-update update
    cmd-export doctor doctor
    cmd-export init-profile init

    cmd-export-ns env "Environment namespace"
    cmd-export env-show
    cmd-export env-export

    if [[ "$PROFILE_LOADED" ]] ; then
        cmd-export deployer-generate generate
        cmd-export deployer-delete delete
        cmd-export compose-ps ps
        cmd-export compose-up start
        cmd-export compose-kill kill
        cmd-export compose-logs logs
        cmd-export compose-pull pull

        cmd-export migrate-startdb startdb
        cmd-export migrate-cmd migrate

    fi

    if [[ "$DEBUG" ]]; then
        cmd-export fn-call fn
    fi
    
	if [[ "${!#}" == "-h" || "${!#}" == "--help" ]]; then
		local args=("$@")
		unset args[${#args[@]}-1]
		cmd-ns "" help "${args[@]}"
	else
		cmd-ns "" "$@"
	fi

}
