docker-check-boot2docker() {

    boot2docker version &> /dev/null || local missing=1
    if [[ "$missing" ]]; then
        echo "[ERROR] boot2docker command not found, please install by:" | red
        echo "brew install boot2docker" | blue
        exit 127
    fi

    debug "TODO: check for version and instruction for update ..."

    info "boot2docker: OK" | green
}

docker-getversion() {
    declare desc="Gets the numeric version from version string"

    local versionstr="$*"
    debug versionstr=$versionstr
    local fullver=$(echo "${versionstr%,*}" |sed "s/.*version[ :]*//")
    debug fullver=$fullver
    # remove -rc2 and similar
    local numver=$(echo ${fullver%-*} | sed "s/\.//g")
    debug numver=$numver

    echo $numver
}

docker-check-version() {
    declare desc="Checks if docker is at least 1.5.0"

    docker --version &> /dev/null || local missing=1
    if [[ "$missing" ]]; then
        echo "[ERROR] docker command not found, please install docker. https://docs.docker.com/installation/" | red
        exit 127
    fi
    info "docker command: OK"

    local ver=$(docker --version 2> /dev/null)
    local numver=$(docker-getversion $ver)
    
    if [ $numver -lt 150 ]; then
        local target=$(which docker 2>/dev/null || true)
        : ${target:=/usr/local/bin/docker}
        echo "[ERROR] Please upgrade your docker version to 1.5.0 or latest" | red
        echo "suggested command:" | red
        echo "  sudo curl -Lo $target https://get.docker.com/builds/$(uname -s)/$(uname -m)/docker-latest ; chmod +x $target" | blue
        exit 1
    fi
    info "docker client version: OK"

    if ! grep -q DOCKER_HOST Profile; then
        if ! grep DOCKER_HOST $CBD_PROFILE; then
            echo "[WARNING] no DOCKER_HOST set in profile" | yellow
            echo "Run the following command to fix:" | yellow
            echo '  for v in DOCKER_HOST DOCKER_CERT_PATH DOCKER_TLS_VERIFY; do echo export ${v}=${!v}; done >> '$CBD_PROFILE | blue
        fi
    fi

    docker version &> /tmp/cbd.log || noserver=1
    if [[ "$noserver" ]]; then
        echo "[ERROR] docker version returned an error" | red
        cat /tmp/cbd.log | yellow
        exit 127
    fi

    local serverVer=$(docker version 2> /dev/null | grep "Server version")
    local numserver=$(docker-getversion $serverVer)

    if [ $numserver -lt 150 ]; then
        echo "[ERROR] Please upgrade your docker version to 1.5.0 or latest" | red
        echo "[WARNING] your local docker seems to be fine, only the server version is outdated" | yellow
        exit 1
    fi
    info "docker server version: OK"
}
