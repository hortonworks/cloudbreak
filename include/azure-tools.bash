azure-configure-arm() {
    declare desc="Configure new ARM application"
    docker run -v "$PWD/.azure:/root/.azure" -it sequenceiq/azure-cli-tools:1.4 configure-arm "$@"
}
