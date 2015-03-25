docker-check-version() {
    declare desc="Checks if docker is at least 1.5.0"

    docker --version &> /dev/null || missing=1
    if [[ "$missing" ]]; then
        echo "[ERROR] docker command not found, please install docker. https://docs.docker.com/installation/" | red
        exit 127
    fi

    local ver=$(docker --version 2> /dev/null)
    debug ver=$ver
    local numver=$(echo ${ver%%,*}|sed "s/[^0-9]//g")
    debug numeric version: $numver
    
    if [ $numver -lt 150 ]; then
        local target=$(which docker 2>/dev/null || true)
        : ${target:=/usr/local/bin/docker}
        echo "[ERROR] Please upgrade your docker version to 1.5.0 or latest" | red
        echo "suggested command:" | red
        echo "  sudo curl -Lo $target https://get.docker.com/builds/$(uname -s)/$(uname -m)/docker-latest ; chmod +x $target" | green
        exit 1
    fi

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
    debug serverVer=$serverVer
    local numserver=$(echo $serverVer | sed -n "s/[a-zA-Z \.:]//gp")
    debug numserver=$numserver

    if [ $numserver -lt 150 ]; then
        echo "[ERROR] Please upgrade your docker version to 1.5.0 or latest" | red
        echo "[WARNING] your local docker seems to be fine, only the server version is outdated" | yellow
        exit 1
    fi
    
}
