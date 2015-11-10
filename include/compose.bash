compose-init() {
    deps-require docker-compose
    env-import CB_COMPOSE_PROJECT cbreak
    env-import CBD_LOG_NAME cbreak
    env-import ULUWATU_VOLUME_HOST /dev/null
    if [[ "$ULUWATU_VOLUME_HOST" != "/dev/null" ]]; then
      ULUWATU_VOLUME_CONTAINER=/uluwatu
    else
      ULUWATU_VOLUME_CONTAINER=/tmp/null
    fi
    env-import SULTANS_VOLUME_HOST /dev/null
    if [[ "$SULTANS_VOLUME_HOST" != "/dev/null" ]]; then
      SULTANS_VOLUME_CONTAINER=/sultans
    else
      SULTANS_VOLUME_CONTAINER=/tmp/null
    fi
}

dockerCompose() {
    debug "docker-compose -p ${CB_COMPOSE_PROJECT} $@"
    docker-compose -p ${CB_COMPOSE_PROJECT} "$@"
}

compose-ps() {
    declare desc="docker-compose: List containers"

    dockerCompose ps
}

compose-pull() {
    declare desc="Pulls service images"

    [ -f docker-compose.yml ] || deployer-generate

    dockerCompose pull
}

compose-pull-parallel() {
    declare desc="Pulls service images parallel"

    [ -f docker-compose.yml ] || deployer-generate

    sed -n "s/.*image://p" docker-compose.yml |sort -u|xargs -n1 -P 20 docker pull
}

create-logfile() {

    rm -f ${CBD_LOG_NAME}.log
    export LOG=${CBD_LOG_NAME}-$(date +%Y%m%d-%H%M%S).log
    touch $LOG
    ln -s $LOG ${CBD_LOG_NAME}.log
}

compose-up() {
    dockerCompose up -d "$@"
}

compose-kill() {
    declare desc="Kills and removes all cloudbreak related container"

    dockerCompose kill
    dockerCompose rm -f
}

util-cleanup() {
    declare desc="Removes all exited containers and old cloudbreak related images"

    if [ ! -f docker-compose.yml ]; then
      error "docker-compose.yml file does not exists"
      exit 126
    fi

    compose-remove-exited-containers

    local all_images=$(docker images | sed "s/ \+/ /g"|cut -d' ' -f 1,2|tr ' ' : | tail -n +2)
    local keep_images=$(sed -n "s/.*image://p" docker-compose.yml)
    local images_to_delete=$(compose-get-old-images <(echo $all_images) <(echo $keep_images))
    if [ -n "$images_to_delete" ]; then
      info "Found old/different versioned images based on docker-compose.yml file: $images_to_delete"
      docker rmi $images_to_delete
    else
      info "Not found any different versioned images (based on docker-compose.yml). Skip cleanup"
    fi
}

compose-get-old-images() {
    declare desc="Retrieve old images"
    declare all_images="${1:? required: all images}"
    declare keep_images="${2:? required: keep images}"
    local all_imgs=$(cat $all_images) keep_imgs=$(cat $keep_images)
    contentsarray=()
    for versionedImage in $keep_imgs
      do
        image_name="${versionedImage%:*}"
        image_version="${versionedImage#*:}"
        remove_images=$(echo $all_imgs | tr ' ' "\n" | grep "$image_name:" | grep -v "$image_version")
        if [ -n "$remove_images" ]; then
          contentsarray+="${remove_images[@]} "
        fi
    done
    echo ${contentsarray%?}
}

compose-remove-exited-containers() {
    declare desc="Remove exited containers"
    local exited_containers=$(docker ps --all -q -f status=exited)
    if [ -n "$exited_containers" ]; then
      info "Remove exited docker containers"
      docker rm $exited_containers;
    fi
}

compose-get-container() {
    declare desc=""
    declare service="${1:? required: service name}"
    dockerCompose ps -q "${service}"
}

compose-logs() {
    declare desc='Follow all logs in color. Separate service names by space to filter, e.g. "cbd logs cloudbreak uluwatu"'

    dockerCompose logs "$@"
}

compose-generate-check-diff() {
    cloudbreak-config
    local verbose="$1"

    if [ -f docker-compose.yml ]; then
         compose-generate-yaml-force /tmp/docker-compose-delme.yml
         if diff /tmp/docker-compose-delme.yml docker-compose.yml &>/dev/null; then
             debug "docker-compose.yml already exist, and generate wouldn't change it."
             return 0
        else
            if ! [[ "$regeneteInProgress" ]]; then
                warn "docker-compose.yml already exists, BUT generate would create a DIFFERENT one!"
                warn "please regenerate it:"
                echo "  cbd regenerate" | blue
            fi
            if [[ "$verbose" ]]; then
                warn "expected change:"
                diff /tmp/docker-compose-delme.yml docker-compose.yml || true
            else
                debug "expected change:"
                (diff /tmp/docker-compose-delme.yml docker-compose.yml || true) | debug-cat
            fi
            return 1
         fi
    fi
    return 0
}

compose-generate-yaml() {
    declare desc="Generating docker-compose.yml based on Profile settings"

    cloudbreak-config

    if ! compose-generate-check-diff; then
        if [[ "$CBD_FORCE_START" ]]; then
            warn "You have forced to start ..."
        else
            warn "Please check the expected config changes with:"
            echo "  cbd doctor" | blue
            debug "If you want to ignore the changes, set the CBD_FORCE_START to true in Profile"
            exit 1
        fi
    else
        info "generating docker-compose.yml"
        compose-generate-yaml-force docker-compose.yml
    fi
}

compose-generate-yaml-force() {

    declare composeFile=${1:? required: compose file path}
    debug "Generating docker-compose yaml: ${composeFile} ..."
    if [[ -z "$AWS_SECRET_ACCESS_KEY" && -n "$AWS_SECRET_KEY"  ]]; then
        debug "AWS_SECRET_ACCESS_KEY is not set, fall back to deprecated AWS_SECRET_KEY"
        export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_KEY
    fi
    cat > ${composeFile} <<EOF
consul:
    privileged: true
    volumes:
        - "/var/run/docker.sock:/var/run/docker.sock"
    ports:
        - "$PRIVATE_IP:53:8600/udp"
        - "8400:8400"
        - "8500:8500"
    hostname: node1
    image: gliderlabs/consul-server:$DOCKER_TAG_CONSUL
    command: --bootstrap --advertise $PRIVATE_IP $DOCKER_CONSUL_OPTIONS

registrator:
    privileged: true
    volumes:
        - "/var/run/docker.sock:/tmp/docker.sock"
    image: gliderlabs/registrator:$DOCKER_TAG_REGISTRATOR
    links:
        - consul
    command: consul://consul:8500

logsink:
    ports:
        - 3333
    environment:
        - SERVICE_NAME=logsink
    volumes:
        - .:/tmp
    image: sequenceiq/socat:1.0.0
    command: socat -u TCP-LISTEN:3333,reuseaddr,fork OPEN:/tmp/cbreak.log,creat,append

logspout:
    ports:
        - 8000:80
    environment:
        - SERVICE_NAME=logspout
        - DEBUG=true
        - LOGSPOUT=ignore
        - ROUTE_URIS=tcp://logsink:3333
        - "RAW_FORMAT={{.Container.Name}} | {{.Data}}\n"
    links:
        - logsink
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    image: gliderlabs/logspout:master
    entrypoint: ["/bin/sh"]
    command: -c 'sleep 1; /bin/logspout'

uaadb:
    privileged: true
    ports:
        - "$PRIVATE_IP:5434:5432"
    environment:
      - SERVICE_NAME=uaadb
        #- SERVICE_CHECK_CMD=bash -c 'psql -h 127.0.0.1 -p 5432  -U postgres -c "select 1"'
    volumes:
        - "$CB_DB_ROOT_PATH/uaadb:/var/lib/postgresql/data"
    image: quay.io/sequenceiq/uaadb:$DOCKER_TAG_UAADB

identity:
    ports:
        - 8089:8080
    environment:
        - SERVICE_NAME=identity
        # - SERVICE_CHECK_HTTP=/login
        - IDENTITY_DB_URL=uaadb.service.consul:5434
    dns: $PRIVATE_IP
    volumes:
      - ./uaa.yml:/uaa/uaa.yml
    image: sequenceiq/uaa:$DOCKER_TAG_UAA

cbdb:
    ports:
        - "$PRIVATE_IP:5432:5432"
    environment:
      - SERVICE_NAME=cbdb
        #- SERVICE_CHECK_CMD=bash -c 'psql -h 127.0.0.1 -p 5432  -U postgres -c "select 1"'
    volumes:
        - "$CB_DB_ROOT_PATH/cbdb:/var/lib/postgresql/data"
    image: sequenceiq/cbdb:$DOCKER_TAG_CBDB

cloudbreak:
    environment:
        - AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
        - AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
        - SERVICE_NAME=cloudbreak
          #- SERVICE_CHECK_HTTP=/info
        - CB_CLIENT_ID=$UAA_CLOUDBREAK_ID
        - CB_CLIENT_SECRET=$UAA_CLOUDBREAK_SECRET
        - CB_BLUEPRINT_DEFAULTS=$CB_BLUEPRINT_DEFAULTS
        - CB_TEMPLATE_DEFAULTS=$CB_TEMPLATE_DEFAULTS
        - CB_AZURE_IMAGE_URI=$CB_AZURE_IMAGE_URI
        - CB_GCP_SOURCE_IMAGE_PATH=$CB_GCP_SOURCE_IMAGE_PATH
        - CB_AWS_AMI_MAP=$CB_AWS_AMI_MAP
        - CB_OPENSTACK_IMAGE=$CB_OPENSTACK_IMAGE
        - CB_HBM2DDL_STRATEGY=$CB_HBM2DDL_STRATEGY
        - CB_HOST_ADDR=$CB_HOST_ADDR
        - CB_SMTP_SENDER_USERNAME=$CLOUDBREAK_SMTP_SENDER_USERNAME
        - CB_SMTP_SENDER_PASSWORD=$CLOUDBREAK_SMTP_SENDER_PASSWORD
        - CB_SMTP_SENDER_HOST=$CLOUDBREAK_SMTP_SENDER_HOST
        - CB_SMTP_SENDER_PORT=$CLOUDBREAK_SMTP_SENDER_PORT
        - CB_SMTP_SENDER_FROM=$CLOUDBREAK_SMTP_SENDER_FROM
        - CB_BAYWATCH_ENABLED=$CB_BAYWATCH_ENABLED
        - CB_BAYWATCH_EXTERN_LOCATION=$CB_BAYWATCH_EXTERN_LOCATION
        - CB_DOCKER_CONTAINER_AMBARI=$CB_DOCKER_CONTAINER_AMBARI
        - CB_DOCKER_CONTAINER_AMBARI_WARM=$CB_DOCKER_CONTAINER_AMBARI_WARM
        - ENDPOINTS_AUTOCONFIG_ENABLED=false
        - ENDPOINTS_DUMP_ENABLED=false
        - ENDPOINTS_TRACE_ENABLED=false
        - ENDPOINTS_CONFIGPROPS_ENABLED=false
        - ENDPOINTS_METRICS_ENABLED=false
        - ENDPOINTS_MAPPINGS_ENABLED=false
        - ENDPOINTS_BEANS_ENABLED=false
        - ENDPOINTS_ENV_ENABLED=false
        - CB_ADDRESS_RESOLVING_TIMEOUT=$ADDRESS_RESOLVING_TIMEOUT
        - CB_IDENTITY_SERVICEID=identity.service.consul
        - CB_DB_SERVICEID=cbdb.service.consul
        - SECURE_RANDOM=$SECURE_RANDOM
        - CB_MAIL_SMTP_AUTH=$CB_MAIL_SMTP_AUTH
        - CB_MAIL_SMTP_STARTTLS_ENABLE=$CB_MAIL_SMTP_STARTTLS_ENABLE
    ports:
        - 8080:8080
    volumes:
        - "$CBD_CERT_ROOT_PATH:/certs"
    dns: $PRIVATE_IP
    image: sequenceiq/cloudbreak:$DOCKER_TAG_CLOUDBREAK
    command: bash

sultans:
    environment:
        - SL_CLIENT_ID=$UAA_SULTANS_ID
        - SL_CLIENT_SECRET=$UAA_SULTANS_SECRET
        - SERVICE_NAME=sultans
          #- SERVICE_CHECK_HTTP=/
        - SL_PORT=3000
        - SL_SMTP_SENDER_HOST=$CLOUDBREAK_SMTP_SENDER_HOST
        - SL_SMTP_SENDER_PORT=$CLOUDBREAK_SMTP_SENDER_PORT
        - SL_SMTP_SENDER_USERNAME=$CLOUDBREAK_SMTP_SENDER_USERNAME
        - SL_SMTP_SENDER_PASSWORD=$CLOUDBREAK_SMTP_SENDER_PASSWORD
        - SL_SMTP_SENDER_FROM=$CLOUDBREAK_SMTP_SENDER_FROM
        - SL_CB_ADDRESS=$ULU_HOST_ADDRESS
        - SL_ADDRESS=$ULU_SULTANS_ADDRESS
        - SL_ADDRESS_RESOLVING_TIMEOUT=$ADDRESS_RESOLVING_TIMEOUT
        - SL_UAA_SERVICEID=identity.service.consul
    ports:
        - 3001:3000
    volumes:
        - $SULTANS_VOLUME_HOST:$SULTANS_VOLUME_CONTAINER
    dns: $PRIVATE_IP
    image: sequenceiq/sultans-bin:$DOCKER_TAG_SULTANS

uluwatu:
    environment:
        - SERVICE_NAME=uluwatu
          #- SERVICE_CHECK_HTTP=/
        - ULU_OAUTH_REDIRECT_URI=$ULU_OAUTH_REDIRECT_URI
        - ULU_SULTANS_ADDRESS=$ULU_SULTANS_ADDRESS
        - ULU_OAUTH_CLIENT_ID=$UAA_ULUWATU_ID
        - ULU_OAUTH_CLIENT_SECRET=$UAA_ULUWATU_SECRET
        - ULU_HOST_ADDRESS=$ULU_HOST_ADDRESS
        - NODE_TLS_REJECT_UNAUTHORIZED=0

        - ULU_ADDRESS_RESOLVING_TIMEOUT=$ADDRESS_RESOLVING_TIMEOUT
        - ULU_SULTANS_SERVICEID=sultans.service.consul
        - ULU_IDENTITY_SERVICEID=identity.service.consul
        - ULU_CLOUDBREAK_SERVICEID=cloudbreak.service.consul
        - ULU_PERISCOPE_SERVICEID=periscope.service.consul
    ports:
        - 3000:3000
    volumes:
        - $ULUWATU_VOLUME_HOST:$ULUWATU_VOLUME_CONTAINER
    dns: $PRIVATE_IP
    image: sequenceiq/uluwatu-bin:$DOCKER_TAG_ULUWATU

pcdb:
    environment:
        - SERVICE_NAME=pcdb
     #- SERVICE_NAMEE_CHECK_CMD='psql -h 127.0.0.1 -p 5432  -U postgres -c "select 1"'
    ports:
        - "$PRIVATE_IP:5433:5432"
    volumes:
        - "$CB_DB_ROOT_PATH/periscopedb:/var/lib/postgresql/data"
    image: sequenceiq/pcdb:$DOCKER_TAG_PCDB

periscope:
    environment:
        - PERISCOPE_DB_HBM2DDL_STRATEGY=$PERISCOPE_DB_HBM2DDL_STRATEGY
        - SERVICE_NAME=periscope
          #- SERVICE_CHECK_HTTP=/info
        - PERISCOPE_SMTP_HOST=$CLOUDBREAK_SMTP_SENDER_HOST
        - PERISCOPE_SMTP_USERNAME=$CLOUDBREAK_SMTP_SENDER_USERNAME
        - PERISCOPE_SMTP_PASSWORD=$CLOUDBREAK_SMTP_SENDER_PASSWORD
        - PERISCOPE_SMTP_FROM=$CLOUDBREAK_SMTP_SENDER_FROM
        - PERISCOPE_SMTP_PORT=$CLOUDBREAK_SMTP_SENDER_PORT
        - PERISCOPE_CLIENT_ID=$UAA_PERISCOPE_ID
        - PERISCOPE_CLIENT_SECRET=$UAA_PERISCOPE_SECRET
        - PERISCOPE_HOSTNAME_RESOLUTION=public
        - PERISCOPE_MAIL_SMTP_AUTH=$CB_MAIL_SMTP_AUTH
        - PERISCOPE_MAIL_SMTP_STARTTLS_ENABLE=$CB_MAIL_SMTP_STARTTLS_ENABLE
        - ENDPOINTS_AUTOCONFIG_ENABLED=false
        - ENDPOINTS_DUMP_ENABLED=false
        - ENDPOINTS_TRACE_ENABLED=false
        - ENDPOINTS_CONFIGPROPS_ENABLED=false
        - ENDPOINTS_METRICS_ENABLED=false
        - ENDPOINTS_MAPPINGS_ENABLED=false
        - ENDPOINTS_BEANS_ENABLED=false
        - ENDPOINTS_ENV_ENABLED=false
        - PERISCOPE_ADDRESS_RESOLVING_TIMEOUT=$ADDRESS_RESOLVING_TIMEOUT
        - PERISCOPE_DB_SERVICEID=pcdb.service.consul
        - PERISCOPE_CLOUDBREAK_SERVICEID=cloudbreak.service.consul
        - PERISCOPE_IDENTITY_SERVICEID=identity.service.consul
        - SECURE_RANDOM=$SECURE_RANDOM
    ports:
        - 8085:8080
    dns: $PRIVATE_IP
    volumes:
        - "$CBD_CERT_ROOT_PATH:/certs"
    image: sequenceiq/periscope:$DOCKER_TAG_PERISCOPE

EOF
}
