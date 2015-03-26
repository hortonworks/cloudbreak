
cloudbreak-init() {
  cloudbreak-conf-tags
  cloudbreak-conf-images
  cloudbreak-conf-cbdb
  cloudbreak-conf-defaults
  cloudbreak-conf-uaa
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

cloudbreak-conf-uaa() {
    env-import UAA_CLOUDBREAK_ID cloudbreak
    env-import UAA_CLOUDBREAK_SECRET $(gen-password)

    env-import UAA_PERISCOPE_ID periscope
    env-import UAA_PERISCOPE_SECRET $(gen-password)

    env-import UAA_ULUWATU_ID uluwatu
    env-import UAA_ULUWATU_SECRET $(gen-password)

    env-import UAA_SULTANS_ID sultans
    env-import UAA_SULTANS_SECRET $(gen-password)

    env-import UAA_CLOUDBREAK_SHELL_ID cloudbreak_shell

    env-import UAA_DEFAULT_USER_EMAIL admin@example.com
    env-import UAA_DEFAULT_USER_PW cloudbreak
    env-import UAA_DEFAULT_USER_FIRSTNAME Joe
    env-import UAA_DEFAULT_USER_LASTNAME Admin
}

cloudbreak-conf-defaults() {
    env-import PRIVATE_IP
    env-import PUBLIC_IP

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

gen-password() {
    date +%s|shasum|head -c 10
}


prepare_uaa_config() {

    cat > uaa_client_details << EOF
Cloudbreak client ID: $UAA_CLOUDBREAK_ID
Cloudbreak client secret: $UAA_CLOUDBREAK_SECRET
Periscope client ID: $UAA_PERISCOPE_ID
Periscope client secret: $UAA_PERISCOPE_SECRET
Uluwatu client ID: $UAA_ULUWATU_ID
Uluwatu client secret: $UAA_ULUWATU_SECRET
Sultans client ID: $UAA_SULTANS_ID
Sultans client secret: $UAA_SULTANS_SECRET
Cloudbreak Shell client ID: $UAA_CLOUDBREAK_SHELL_ID
EOF

    cat > uaa.yml << EOF
spring_profiles: postgresql

database:
  driverClassName: org.postgresql.Driver
  url: jdbc:postgresql://\${IDENTITY_DB_URL}/postgres
  username: \${IDENTITY_DB_USER:postgres}
  password: \${IDENTITY_DB_PASS:}

oauth:
  client:
    override: true
    autoapprove:
      - ${UAA_CLOUDBREAK_SHELL_ID}
  clients:
    ${UAA_SULTANS_ID}:
      id: ${UAA_SULTANS_ID}
      secret: ${UAA_SULTANS_SECRET}
      authorized-grant-types: client_credentials
      scope: scim.read,scim.write,password.write
      authorities: uaa.resource,scim.read,scim.write,password.write
    ${UAA_ULUWATU_ID}:
      id: ${UAA_ULUWATU_ID}
      secret: ${UAA_ULUWATU_SECRET}
      authorized-grant-types: authorization_code,client_credentials
      scope: cloudbreak.blueprints,cloudbreak.credentials,cloudbreak.stacks,cloudbreak.templates,openid,password.write,cloudbreak.usages.global,cloudbreak.usages.account,cloudbreak.usages.user,cloudbreak.events,periscope.cluster,cloudbreak.recipes
      authorities: cloudbreak.subscribe
      redirect-uri: http://${PUBLIC_IP}:3000/authorize
    ${UAA_CLOUDBREAK_ID}:
      id: ${UAA_CLOUDBREAK_ID}
      secret: ${UAA_CLOUDBREAK_SECRET}
      authorized-grant-types: client_credentials
      scope: scim.read,scim.write,password.write
      authorities: uaa.resource,scim.read,scim.write,password.write
    ${UAA_PERISCOPE_ID}:
      id: ${UAA_PERISCOPE_ID}
      secret: ${UAA_PERISCOPE_SECRET}
      authorized-grant-types: client_credentials
      scope: none
      authorities: cloudbreak.autoscale,uaa.resource,scim.read
    ${UAA_CLOUDBREAK_SHELL_ID}:
      id: ${UAA_CLOUDBREAK_SHELL_ID}
      authorized-grant-types: implicit
      scope: cloudbreak.templates,cloudbreak.blueprints,cloudbreak.credentials,cloudbreak.stacks,cloudbreak.events,cloudbreak.usages.global,cloudbreak.usages.account,cloudbreak.usages.user,cloudbreak.recipes,openid,password.write
      authorities: uaa.none
      redirect-uri: http://cloudbreak.shell

scim:
  username_pattern: '[a-z0-9+\-_.@]+'
  users:
    - ${UAA_DEFAULT_USER_EMAIL}|${UAA_DEFAULT_USER_PW}|${UAA_DEFAULT_USER_EMAIL}|${UAA_DEFAULT_USER_FIRSTNAME}|${UAA_DEFAULT_USER_LASTNAME}|openid,cloudbreak.templates,cloudbreak.blueprints,cloudbreak.credentials,cloudbreak.stacks,sequenceiq.cloudbreak.admin,sequenceiq.cloudbreak.user,sequenceiq.account.seq1234567.SequenceIQ,cloudbreak.events,cloudbreak.usages.global,cloudbreak.usages.account,cloudbreak.usages.user,periscope.cluster,cloudbreak.recipes

EOF

    info "Saved client ids & secrets for the UAA clients in uaa_client_details and generated uaa.yml config."
}

start_uaadb() {
    declare desc="Starts the DB for uaa"
    
    local name=uaadb
    
    if check_skip $name; then
        warn "skipping container: $name"
        return
    fi

    if check_if_running $name; then
        warn "$name is already running, not starting"
        return
    fi
    
    debug starting $name ...
    docker run --privileged -d -P \
      --name=$name \
      -e "SERVICE_NAME=$name" \
      -e SERVICE_CHECK_CMD='psql -h 127.0.0.1 -p 5432  -U postgres -c "select 1"' \
      -v /var/lib/cloudbreak/uaadb:/var/lib/postgresql/data \
      postgres:$DOCKER_TAG_POSTGRES
}

start_identity() {
    declare desc="Starts the Identity Server"

    wait_for_service uaadb
    local name=identity
    
    if check_skip $name; then
        warn "skipping container: $name"
        return
    fi
    
    if check_if_running $name; then
        warn "$name is already running, not starting"
        return
    fi

    debug starting $name ...
    docker run --privileged -d -P \
      --name=$name \
      -e "SERVICE_NAME=$name" \
      -e SERVICE_CHECK_HTTP=/login \
      -e IDENTITY_DB_URL=$(dhp uaadb) \
      -v $PWD/uaa.yml:/uaa/uaa.yml \
      -v /var/lib/uaa/uaadb:/var/lib/postgresql/data \
      -p 8089:8080 \
      sequenceiq/uaa:$DOCKER_TAG_UAA

}

# dig service host ip
dh() {
    dig @${PRIVATE_IP} +short $1.service.consul
}

# dig service port
dp() {
    dig @${PRIVATE_IP} +short $1.service.consul SRV | cut -d" " -f 3
}

# dig host:port
dhp(){
    echo $(dh $1):$(dp $1)
}

wait_for_service() {
    declare desc="waits for a service entry to appear in consul"
    declare service=$1
    : ${service:? required}

    debug "wait for $service gets registered in consul ..."
    ( docker run -it --rm \
        --net container:consul \
        --entrypoint /bin/consul \
        sequenceiq/consul:$DOCKER_TAG_CONSUL \
          watch -type=service -service=$service -passingonly=true bash -c 'cat|grep "\[\]" '
    ) &> /dev/null || true
    debug "$service is registered: $(dhp $service)"
}

cloudbreak-deploy() {
    declare desc="Deploys all Cloudbreak components in docker containers"

    start_consul | gray
    start_registrator | gray
    prepare_uaa_config
    start_uaadb
    start_identity
    wait_for_service identity
    
}

token() {
    cloudbreak-init
    local TOKEN=$(curl -siX POST \
        -H "accept: application/x-www-form-urlencoded" \
        -d credentials='{"username":"'${UAA_DEFAULT_USER_EMAIL}'","password":"'${UAA_DEFAULT_USER_PW}'"}' \
        "$(dhp identity)/oauth/authorize?response_type=token&client_id=cloudbreak_shell&scope.0=openid&source=login&redirect_uri=http://cloudbreak.shell" \
           | grep Location | cut -d'=' -f 2 | cut -d'&' -f 1)
    debug TOKEN=$TOKEN
}

cloudbreak-destroy() {
    declare desc="Destroys Cloudbreak related containers"

    local containers="consul registrator uaadb identiry"

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
