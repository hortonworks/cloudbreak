azure-configure-arm() {
    declare desc="Configure new ARM application"

    docker run -it sequenceiq/azure-cli-tools:1.0 arm_configure "$@"
}

azure-deploy-dash() {
    declare desc="Deploy the MicrosoftDX/DASH project to a cloud service and create its storage accounts "

    docker run -it sequenceiq/azure-cli-tools:1.0 deploy_dash "$@"
}
