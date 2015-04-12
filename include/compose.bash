compose-init() {
    deps-require docker-compose
    cmd-export compose-ps ps
    cmd-export compose-up start
    cmd-export compose-kill kill
}

compose-ps() {
    declare desc="docker-compose: List containers"

    docker-compose ps
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
