init() {
    echo "cloudbreak-deployer init ..." | yellow
}

cbd-version() {
    declare desc="Displays the version of Cloudbrek Deployer"
    bin-version
}

main() {
	set -eo pipefail; [[ "$TRACE" ]] && set -x
	color-init

    cmd-export cmd-help help
    cmd-export cbd-version version

	if [[ "${!#}" == "-h" || "${!#}" == "--help" ]]; then
		local args=("$@")
		unset args[${#args[@]}-1]
		cmd-ns "" help "${args[@]}"
	else
		cmd-ns "" "$@"
	fi

}
