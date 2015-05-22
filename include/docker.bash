docker-check-boot2docker() {

    boot2docker version &> /dev/null || local missing=1
    if [[ "$missing" ]]; then
        echo "[ERROR] boot2docker command not found, please install by:" | red
        echo "  brew install boot2docker" | blue
        exit 127
    fi

    : << "UNTIL-BOOT2DOCKER-CLI-366-GET-MERGED"
    if [[ "$(boot2docker status)" == "running" ]]; then
        if [[ "$(boot2docker shellinit 2>/dev/null)" == "" ]];then
            info "boot2docker shellinit: OK"
        else
            echo "[ERROR] boot2docker shell env is not set correctly, please run:" | red
            echo ' eval "$(boot2docker shellinit)"' | blue
            exit 125
        fi
    else
        echo "[ERROR] boot2docker is not running, please start by:" | red
        echo "  boot2docker start" | blue
        exit 126
    fi
UNTIL-BOOT2DOCKER-CLI-366-GET-MERGED
    if [[ "$(boot2docker status)" == "running" ]]; then
        if [[ "$DOCKER_HOST" != "" ]] && [[ "$DOCKER_CERT_PATH" != "" ]] && [[ "$DOCKER_TLS_VERIFY" != "" ]]; then
            info "boot2docker shellinit: OK"
        else
            echo "[ERROR] boot2docker shell env is not set correctly, please run:" | red
            echo ' eval "$(boot2docker shellinit)"' | blue
            exit 125
        fi
    else
        echo "[ERROR] boot2docker is not running, please start by:" | red
        echo "  boot2docker start" | blue
        exit 126
    fi

    debug "TODO: check for version and instruction for update ..."

    local b2dDate=$(boot2docker ssh 'date -u +%Y-%m-%d\ %H:%M')
    local localDate=$(date -u +%Y-%m-%d\ %H:%M)
    if [[ "$localDate" != "$b2dDate" ]];then
        warn "Your UTC time in boot2docker [$b2dDate] isn't the same as local time: [$localDate] "
        warn 'Fixing it ...'
        boot2docker ssh sudo date --set \'$(date -u +%Y-%m-%d\ %H:%M)\' | gray
        b2dDate=$(boot2docker ssh 'date -u +%Y-%m-%d\ %H:%M')
        localDate=$(date -u +%Y-%m-%d\ %H:%M)
        if [[ "$localDate" != "$b2dDate" ]];then
            echo "Couldnt correct date in boot2docker, giving up" |red
            exit 2
        else
             info "boot2docker date settings: OK" | green
        fi
    else
        info "boot2docker date settings: OK" | green
    fi

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
