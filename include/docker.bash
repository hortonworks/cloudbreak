docker-check-version() {
    declares desc="Checks if docker is at least 1.5.0"

    local ver=$(docker --version 2> /dev/null)
    if [ $? -gt 0 ]; then
        echo "[ERROR] docker command not found" | red
        exit 127
    fi

    debug $ver
    local numver=$(echo ${ver%%,*}|sed "s/[a-zA-Z \.]//g")
    debug numeric version: $numver
    
    if [ $numver -lt 150 ]; then
        echo "[ERROR] Please upgrade your docker version to 1.5.0 or latest" | red
        echo "suggested command:" | red
        echo "  sudo curl -Lo $(which docker) https://get.docker.com/builds/$(uname -s)/$(uname -m)/docker-latest ; chmod +x  $(which docker)" | green
        exit 1
    fi
}
