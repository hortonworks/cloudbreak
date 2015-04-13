compose-init() {
    deps-require docker-compose
    cmd-export compose-ps ps
    cmd-export compose-up start
    cmd-export compose-kill kill
    cmd-export compose-logs logs
    cmd-export compose-pull pull
}

compose-ps() {
    declare desc="docker-compose: List containers"

    docker-compose ps
}

compose-pull() {
    declare desc="Pulls service images"

    docker-compose pull
}

compose-up() {
    declare desc="Starts containers with docker-compose"

    docker-compose up -d
}

compose-kill() {
    declare desc="Kills and removes all cloudbreak related container"

    docker-compose kill
    docker-compose rm -f
}

compose-logs() {
    declare desc="Whach all container logs in colored version"

    docker-compose logs
}


