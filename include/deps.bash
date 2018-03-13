# Simple binary dependency management

declare DEPS_REPO="${DEPS_REPO:-https://raw.githubusercontent.com/hortonworks/glidergun-rack/master/index}"

deps-init() {
	export PATH="$(deps-dir)/bin:$PATH"
}

deps-dir() {
	echo "$CBD_ROOT/.deps"
}

deps-require() {
	declare name="$1" version="${2:-latest}"
	deps-check "$name" "$version" && return
	echo "* Dependency required, installing $name $version ..." | yellow
	deps-install "$name" "$version"
}

deps-check() {
	declare name="$1" version="${2:-latest}"
	[[ -f "$(deps-dir)/bin/$name" ]]
}

deps-install() {
	declare name="$1" version="${2:-latest}"
	local tag index tmpfile dep filename extension install
	mkdir -p "$(deps-dir)/bin"
	index=$(curl-proxy-aware -s "$DEPS_REPO/$name")
	tag="$(uname -s)_$(uname -m | grep -s 64 > /dev/null && echo amd64 || echo 386)"
	if ! dep="$(echo "$index" | grep -i -e "^$version $tag " -e "^$version \* ")"; then
		echo "!! Dependency not in index: $name $version" | red
		_exit 2
	fi
	IFS=' ' read v t url checksum <<< "$dep"
    downdir="$(deps-dir)/tmp/download"
	mkdir -p "$downdir"
	tmpfile="${downdir:?}/$name"
	curl-proxy-aware -Ls $url > "$tmpfile"
	if [[ "$checksum" ]]; then
		if ! [[ "$(cat "$tmpfile" | checksum md5)" = "$checksum" ]]; then
			echo "!! Dependency checksum failed: $name $version $checksum" | red
			_exit 2
		fi
	fi
	cd $TEMP_DIR
	filename="$(basename "$url")"
	extension="${filename##*.}"
	case "$extension" in
		zip) unzip "$tmpfile" > /dev/null;;
		tgz|gz) tar -zxf "$tmpfile" > /dev/null;;
        *) cp "$tmpfile" .;;
	esac
	install="$(echo "$index" | grep "^# install: " || true)"
	if [[ "$install" ]]; then
		IFS=':' read _ script <<< "$install"
		export PREFIX="$(deps-dir)"
		eval "$script" > /dev/null
		unset PREFIX
	else
		chmod +x "$tmpfile"
        mv "$tmpfile" "$(deps-dir)/bin"
	fi
	cd - > /dev/null
	deps-check "$name" "$version"
}
