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


load-profile() {
    CBD_PROFILE="Profile"
    if [ -f $CBD_PROFILE ]; then
        debug "Use profile: $CBD_PROFILE"
        module-load "$CBD_PROFILE"
    else
        echo "!! No Profile found. Please create a file called 'Profile' in the current dir." | red
        echo "Please copy and paste all blue text below:" | red

        if boot2docker version &> /dev/null; then
            (cat << EOF
cat > Profile << ENDOFPROFILE
export PUBLIC_IP=$(boot2docker ip)
export PRIVATE_IP=\$(docker run alpine sh -c 'ip ro | grep default | cut -d" " -f 3')
ENDOFPROFILE
EOF
            ) | blue
        else
            (cat << EOF
cat > Profile << ENDOFPROFILE
export PRIVATE_IP=\$(docker run alpine sh -c 'ip ro | grep default | cut -d" " -f 3')
ENDOFPROFILE
EOF
             ) | blue
        fi
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

cbd-find-root() {
    CBD_ROOT=$PWD
}

main() {
	set -eo pipefail; [[ "$TRACE" ]] && set -x

    cbd-find-root
    deps-init
	color-init
    load-profile

    circle-init
    cloudbreak-init
    compose-init

    debug "CloudBreak Deployer $(bin-version)"

    cmd-export cmd-help help
    cmd-export cbd-version version
    cmd-export cbd-update update
    cmd-export doctor doctor

    cmd-export-ns env "Environment namespace"
    cmd-export env-show
    cmd-export env-export

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
