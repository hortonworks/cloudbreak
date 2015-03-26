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
    bin-version | green

    echo latest version: | green
    latest-version | yellow
}

cbd-update() {
    declare desc="Updates itself"

    local binver=$(bin-version)
    local lastver=$(latest-version)
    local osarch=$(uname -sm | tr " " _ )
    debug binver=$binver lastver=$lastver osarch=$osarch | gray

    if [[ ${binver} != ${lastver} ]]; then
        debug upgrade needed |yellow
        
        local url=https://github.com/sequenceiq/cloudbreak-deployer/releases/download/v${lastver}/cloudbreak-deployer_${lastver}_${osarch}.tgz
        debug "latest url: $url"
        curl -Ls $url | tar -zx -C /usr/local/bin/
    else
        debug you have the latest version | green
    fi
}

cbd-update-snap() {
    declare desc="Updates itself, from CircleCI, branch name is optional, default: master"
    declare branch=${1:-master}

    url=$(cci-latest sequenceiq/cloudbreak-deployer $branch)
    debug "Update binary from: $url"
    curl -Ls $url | tar -zx -C /usr/local/bin/

}

latest-version() {
  #curl -Ls https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/VERSION
  curl -I https://github.com/sequenceiq/cloudbreak-deployer/releases/latest 2>&1 \
      |sed -n "s/^Location:.*tag.v//p" \
      | sed "s/\r//"
}


load-profile() {
    CBD_PROFILE="Profile"
    if [ -f $CBD_PROFILE ]; then
        debug "Use profile: $CBD_PROFILE"
        module-load "$CBD_PROFILE"
    else
        echo "!! No Profile found. Please create a file called 'Profile' in the current dir. To fix run:" | red
        echo " touch Profile" | blue
        exit 2
    fi

    if [[ "$CBD_DEFAULT_PROFILE" && -f "Profile.$CBD_DEFAULT_PROFILE" ]]; then
        CBD_PROFILE="Profile.$CBD_DEFAULT_PROFILE"

		module-load $CBD_PROFILE
		debug "Use profile $CBD_DEFAULT_PROFILE"
	fi
}

doctor() {
    declare desc="Checks everything, and reports a diagnose"

    if [[ "$(uname)" == "Darwin" ]]; then
        debug "checking boot2docker on OSX only ..."
        docker-check-boot2docker
    fi

    docker-check-version
    info "Everything is very-very first class !!!"
}

main() {
	set -eo pipefail; [[ "$TRACE" ]] && set -x
	color-init
    load-profile

    circle-init
    cloudbreak-init

    debug "CloudBreak Deployer $(bin-version)"

    cmd-export cmd-help help
    cmd-export cbd-version version
    cmd-export cbd-update update
    cmd-export cbd-update-snap update-snap
    cmd-export doctor doctor

    cmd-export-ns env "Environment namespace"
    cmd-export env-show
    cmd-export env-export

    cmd-export cloudbreak-deploy start
    cmd-export cloudbreak-destroy kill

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
