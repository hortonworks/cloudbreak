#!/usr/bin/env bash

: ${GIT_VERSION:=latest}
: ${STOP_MOCK:=false}

get-cbd() {
    declare desc="Downloading CBD binary"

    if curl -Ls s3.amazonaws.com/public-repo-1.hortonworks.com/HDP/cloudbreak/cloudbreak-deployer_${GIT_VERSION}_$(uname)_x86_64.tgz|tar -xz cbd; then
        echo "CBD has been found with version: $GIT_VERSION"
    else
        echo "Getting the latest 'snapshot' version of CBD."
        curl -Ls s3.amazonaws.com/public-repo-1.hortonworks.com/HDP/cloudbreak/cloudbreak-deployer_snapshot_$(uname)_x86_64.tgz|tar -xz cbd
        echo "CBD has been found with version: $(./cbd --version)"
    fi
}

mock-start-logs() {
    declare desc="Gather Cloudbreak Mock start logs"

    mkdir -pv test_log

    if [[ $(id -u jenkins 2>/dev/null || echo $?) -gt 1 ]]; then
        sudo chown -R jenkins .
        sudo docker logs cbreak_cloudbreak_1 > test_log/cloudbreak_start.log
        sudo docker logs cbreak_environment_1 > test_log/environment_start.log
        sudo docker logs cbreak_datalake_1 > test_log/datalake_start.log
    else
        docker logs cbreak_cloudbreak_1 > test_log/cloudbreak_start.log
        docker logs cbreak_environment_1 > test_log/environment_start.log
        docker logs cbreak_datalake_1 > test_log/datalake_start.log
    fi
}

mock-start() {
    declare desc="Start Cloudbreak Mock"

    echo "Starting CBD with minimal set:"
    mv docker-compose.yml docker-compose-mocks.yml

    cat <<EOF > Profile
export CB_LOCAL_DEV_LIST=cloudbreak,periscope,datalake,freeipa,redbeams,environment,uluwatu,cluster-proxy,core-gateway
export VAULT_AUTO_UNSEAL=true
export COMMON_DB_VOL=mock-common
export DOCKER_NETWORK_NAME=mock-cbreak-network
export CB_ENABLEDPLATFORMS=AZURE,OPENSTACK,AWS,GCP,YARN,MOCK
export ENVIRONMENT_ENABLEDPLATFORMS=AZURE,OPENSTACK,AWS,GCP,YARN,MOCK
export UMS_HOST=''
EOF

    ./cbd generate
    ./cbd start
	./.deps/bin/docker-compose -f docker-compose-mocks.yml -p cbreak up -d

    echo "Mock APIs version is: " $(./cbd version)

    sleep 30s
}

mock-stop() {
	./.deps/bin/docker-compose -f docker-compose.yml -f docker-compose-mocks.yml -p cbreak down --volumes
	./cbd kill
}

main() {
    cd tmp
    if [[ "$STOP_MOCK" == "true" ]]; then
        mock-stop
        cd ..
    else
        get-cbd
        mock-start
        cd ..
        mock-start-logs
    fi
}

main "$@"