compose-init() {
    deps-require docker-compose
    cmd-export compose-ps ps
}

compose-ps() {
    declare desc="docker-compose: List containers"

    docker-compose ps
}

compuse-up() {
    declare desc="Starts containers with docker-compose"

    docker-compose up -d
}
