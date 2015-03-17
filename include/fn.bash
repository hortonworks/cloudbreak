# Bash function introspection

fn-args() {
	declare desc="Inspect a function's arguments"
	local argline=$(type $1 | grep declare | grep -v "declare desc" | head -1)
	echo -e "${argline// /"\n"}" | awk -F= '/=/{print "<"$1">"}' | tr "\n" " "
}

fn-desc() {
	declare desc="Inspect a function's description"
	desc=""
	eval "$(type $1 | grep desc | head -1)"; echo $desc
}

fn-info() {
	declare desc="Inspects a function"
	declare fn="$1" showsource="$2"
	echo "$fn $(fn-args $fn)"
	echo "  $(fn-desc $fn)"
	echo
	if [[ "$showsource" ]]; then
		declare -f "$fn" | tail -n +1
		echo
	fi
}

fn-source() {
	declare desc="Shows function source"
	declare fn="$1"
	declare -f "$fn" | tail -n +2
}

fn-call() {
	declare desc="Run arbitrary function"
	declare fn="$1"; shift
	"$fn" "$@"
}
