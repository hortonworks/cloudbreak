azure-configure-arm() {
    declare desc="Configure new ARM application"
    docker run -v "$PWD/azure-cli-log:/root/.azure" -it sequenceiq/azure-cli-tools:1.3 configure-arm "$@"
}

azure-deploy-dash() {
    declare desc="Deploy the MicrosoftDX/DASH project to a cloud service and create its storage accounts "
    docker run -v "$PWD/azure-cli-log:/root/.azure" -it sequenceiq/azure-cli-tools:1.3 deploy_dash "$@"
}