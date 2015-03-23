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
    debug binver=$binver lastver=$lastver|gray

    if [[ ${binver} != ${lastver} ]]; then
        debug upgrade needed |yellow
        
        local url=https://github.com/sequenceiq/cloudbreak-deployer/releases/download/v${lastver}/cloudbreak-deployer_${lastver}_$[osarch].tgz \
        debug "lates turl: $url"
        curl -Ls $url | tar -zx -C /usr/local/bin/
    else
        debug you have the latest version | green
    fi
}

latest-version() {
  #curl -Ls https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/VERSION
  curl -I https://github.com/sequenceiq/cloudbreak-deployer/releases/latest 2>&1 \
      |sed -n "s/^Location:.*tag.v//p" \
      | dos2unix
}

main() {
	set -eo pipefail; [[ "$TRACE" ]] && set -x
	color-init

    debug "CloudBreak Deployer $(bin-version)"
    cmd-export cmd-help help
    cmd-export cbd-version version
    cmd-export cbd-update update

	if [[ "${!#}" == "-h" || "${!#}" == "--help" ]]; then
		local args=("$@")
		unset args[${#args[@]}-1]
		cmd-ns "" help "${args[@]}"
	else
		cmd-ns "" "$@"
	fi

}
