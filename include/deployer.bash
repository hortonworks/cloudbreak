debug() {
  if [[ "$DEBUG" ]]; then
      echo "[DEBUG] $*" | gray 1>&2
  fi
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
    declare desc="Updates itself, from the latest snapshot binary"

    url=$(cci-latest sequenceiq/cloudbreak-deployer)
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
    if [ -f Profile ]; then
        module-load "Profile"
    else
        echo "!! No Profile found. Please create a file called 'Profile' in the current dir." | red
        exit 2
    fi

    if [[ "$CBD_DEFAULT_PROFILE" && -f "Profile.$CBD_DEFAULT_PROFILE" ]]; then
		module-load "Profile.$CBD_DEFAULT_PROFILE"
		echo "* Using default profile $CBD_DEFAULT_PROFILE" | yellow
		GUN_PROFILE="$CBD_DEFAULT_PROFILE"
	fi
}

main() {
	set -eo pipefail; [[ "$TRACE" ]] && set -x
	color-init
    circle-init

    debug "CloudBreak Deployer $(bin-version)"

    load-profile
    
    cmd-export cmd-help help
    cmd-export cbd-version version
    cmd-export cbd-update update
    cmd-export cbd-update-snap update-snap

	if [[ "${!#}" == "-h" || "${!#}" == "--help" ]]; then
		local args=("$@")
		unset args[${#args[@]}-1]
		cmd-ns "" help "${args[@]}"
	else
		cmd-ns "" "$@"
	fi

}
