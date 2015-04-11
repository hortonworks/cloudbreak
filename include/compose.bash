compose-init() {
    deps-require docker-compose
    cmd-export compose-ps ps
}

compose-ps() {
    declare desc="docker-compose: List containers"

    docker-compose ps
}
