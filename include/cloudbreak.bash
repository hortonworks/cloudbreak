
cloudbreak-init() {
  cloudbreak-conf-tags
  cloudbreak-conf-images
  cloudbreak-conf-cbdb
  cloudbreak-conf-defaults
}

cloudbreak-conf-tags() {
    declare desc="Defines docker image tags"

    env-import DOCKER_TAG_ALPINE 3.1
    env-import DOCKER_TAG_CONSUL v0.5.0-v3
    env-import DOCKER_TAG_REGISTRATOR v5
    env-import DOCKER_TAG_POSTGRES 9.4.0
    env-import DOCKER_TAG_UAA 1.8.1-v1
    env-import DOCKER_TAG_CBSHELL 0.2.47
    env-import DOCKER_TAG_CLOUDBREAK 0.3.92
    env-import DOCKER_TAG_ULUWATU 0.1.415
    env-import DOCKER_TAG_SULTANS 0.1.61
    env-import DOCKER_TAG_PERISCOPE 0.1.36
}

cloudbreak-conf-images() {
    declare desc="Defines base images for each provider"
    
    env-import CB_AZURE_IMAGE_URI "https://102589fae040d8westeurope.blob.core.windows.net/images/packer-cloudbreak-2015-03-10-centos6_2015-March-10_17-15-os-2015-03-10.vhd"
    env-import CB_GCP_SOURCE_IMAGE_PATH "sequenceiqimage/sequenceiq-ambari17-consul-centos-2015-03-10-1449.image.tar.gz"
    env-import CB_AWS_AMI_MAP "ap-northeast-1:ami-c528c3c5,ap-southeast-2:ami-e7c3b2dd,sa-east-1:ami-c5e55dd8,ap-southeast-1:ami-42c3f510,eu-west-1:ami-bb35a7cc,us-west-1:ami-4b20c70f,us-west-2:ami-eb1f3ddb,us-east-1:ami-00391e68"
    env-import CB_OPENSTACK_IMAGE "packer-cloudbreak-centos-2015-03-11"
}

cloudbreak-conf-cbdb() {
    declare desc="Declares cloudbreak DB config"

    env-import CB_DB_ENV_USER "postgres"
    env-import CB_DB_ENV_DB "cloudbreak"
    env-import CB_DB_ENV_PASS ""
    env-import CB_HBM2DDL_STRATEGY "update"
    env-import PERISCOPE_DB_HBM2DDL_STRATEGY "update"
}

cloudbreak-conf-defaults() {
    env-import CB_BLUEPRINT_DEFAULTS "lambda-architecture,multi-node-hdfs-yarn,hdp-multinode-default"
}

check_if_running() {
    declare desc="Checks if a container is in running state"

    declare container="$1"

    [[ "true" == $(docker inspect -f '{{.State.Running}}' $container 2>/dev/null) ]]
}

check_skip() {
    declare desc="Checks if a container should be skipped: SKIP_XXX"
    declare name=${1:?}
    
    if grep -qi $name <<< "${!SKIP_*}" && ! check_if_running $name; then
        error  "$name should be skipped but its not running ?!"
        exit 1
    fi
    
    grep -qi $name <<< "${!SKIP_*}"
}

start_consul() {
    declare desc="starts consul binding to: $PRIVATE_IP http:8500 dns:53 rpc:8400"

    local name=consul
    env-import PRIVATE_IP
    
    if check_skip $name; then
        warn "skipping container: $name"
        return
    fi

    if check_if_running $name; then
        warn "$name is already running, not starting"
        return
    fi

    info $desc
    docker run -d \
        -h node1 \
        --name=$name \
        --privileged \
        -e SERVICE_IGNORE=true \
        -p ${PRIVATE_IP}:53:53/udp \
        -p ${PRIVATE_IP}:8400:8400 \
        -p ${PRIVATE_IP}:8500:8500 \
        -v /var/run/docker.sock:/var/run/docker.sock \
        sequenceiq/consul:$DOCKER_TAG_CONSUL -server -bootstrap -advertise ${PRIVATE_IP}
}

start_registrator() {
    declare desc="starts registrator connecting to consul"

    local name=registrator
    env-import PRIVATE_IP
    
    if check_skip $name; then
        warn "skipping container: $name"
        return
    fi

    if check_if_running $name; then
        warn "$name is already running, not starting"
        return
    fi

    info $desc
    docker run -d \
      --name=$name \
      --privileged \
      -v /var/run/docker.sock:/tmp/docker.sock \
      gliderlabs/registrator:$DOCKER_TAG_REGISTRATOR consul://${PRIVATE_IP}:8500
}

cloudbreak-deploy() {
    declare desc="Deploys all Cloudbreak components in docker containers"

    start_consul | gray
    start_registrator | gray
}

cloudbreak-destroy() {
    declare desc="Destroys Cloudbreak related containers"

    local containers="consul registrator"

    info "killing containers: $containers"
    for name in $containers; do
        if ! docker inspect $name &>/dev/null ;then
            warn "no such container: $name"
        else 
            if grep -qi $name <<< "${!SKIP_*}"; then
                warn skipping: $name
            else
                debug stop: $name
                docker stop -t 0 $name | gray
                debug remove: $name
                docker rm -f $name | gray
                
            fi
        fi
    done

}
