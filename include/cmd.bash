# A lightweight subcommand framework

declare -A CMDS

cmd-list() {
	declare desc="Lists available commands"
	declare ns="$1"
	cmd-list-keys "$ns" | sed "s/$ns://"
}

cmd-list-keys() {
	declare ns="$1"
	for k in "${!CMDS[@]}"; do
		echo "$k"
	done | grep "^$ns:" | sort
}

cmd-list-ns() {
	for k in "${!CMDS[@]}"; do
		echo "$k"
	done | grep -v : | sort
}

cmd-export() {
	declare desc="Exports a function as a command"
	declare fn="$1" as="${2:-$1}"
	local ns=""
	for n in $(cmd-list-ns); do
		echo "$fn" | grep "^$n-" &> /dev/null && ns="$n"
	done
	CMDS["$ns:${as/#$ns-/}"]="$fn"
}

cmd-bash-complete-ns() {
    for n in $(cmd-list-ns); do
        echo "        $n) COMPREPLY=(\$(compgen -W '$(cmd-list $n | xargs)' -- \$act)) ;;"
    done
}

cmd-bash-complete() {
     declare desc='Generates bash autocomplete function: eval "$(cbd bash-complete)"'

     cat << EOF
_${SELF}_comp() {
  local act=\${COMP_WORDS[\$COMP_CWORD]}
  case \$COMP_CWORD in
    1) COMPREPLY=(\$(compgen -W "$(cmd-list | xargs)" -- \$act )) ;;
    2) case \${COMP_WORDS[1]}  in
$(cmd-bash-complete-ns)
       esac
    esac
}; complete -F _${SELF}_comp $SELF
EOF
 }

cmd-export-ns() {
	declare ns="$1" desc="$2"
	eval "$1() {
		declare desc=\"$desc\"
		cmd-ns $1 \"\$@\";
	}"
	cmd-export "$1"
	CMDS["$1"]="$1"
}

cmd-ns() {
	local ns="$1"; shift
	local cmd="$1"; shift || true
	local status=0
	if cmd-list "$ns" | grep ^$cmd\$ &> /dev/null; then
		${CMDS["$ns:$cmd"]} "$@"
	else
		if [[ "$cmd" ]]; then
			echo "No such command: $cmd"
			status=2
		elif [[ "$ns" ]]; then
			echo "$(fn-desc "$ns")"
		fi
		echo
		echo "Available commands:"
		for cmd in $(cmd-list "$ns"); do
			printf "  %-24s %s\n" "$cmd" "$(fn-desc "${CMDS["$ns:$cmd"]}")"
			#for subcmd in $(cmd-list "$cmd"); do
			#	printf "    %-24s %s\n" "$subcmd" "$(fn-desc "${CMDS["$cmd:$subcmd"]}")"
			#done
		done
		echo
		_exit $status
	fi
}

cmd-help() {
	declare desc="Shows help information for a command"
	declare args="$@"
	if [[ "$args" ]]; then
    	for cmd; do true; done # last arg
    	local ns="${args/%$cmd/}"; ns="${ns/% /}"; ns="${ns/ /-}"
    	local fn="${CMDS["$ns:$cmd"]}"
    	fn-info "$fn" 1
	else
		cmd-ns ""
	fi
}
