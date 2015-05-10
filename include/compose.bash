compose-init() {
    deps-require docker-compose
    env-import CB_COMPOSE_PROJECT cbreak
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

compose-up() {
    declare desc="Starts containers with docker-compose"
    declare services="$@"

    deployer-generate
    dockerCompose up -d $services

    info "CloudBreak containers are started ..."
    info "In a couple of minutes you can reach the UI (called Uluwatu)"
    echo "  http://$PUBLIC_IP:3000" | blue
    warn "Credentials are not printed here. You can get them by:"
    echo '  cbd env show|grep "UAA_DEFAULT_USER_PW\|UAA_DEFAULT_USER_EMAIL"' | blue
}

compose-kill() {
    declare desc="Kills and removes all cloudbreak related container"

    dockerCompose kill
    dockerCompose rm -f
}

compose-get-container() {
    declare desc=""
    declare service="${1:? required: service name}"
    dockerCompose ps -q "${service}"
}

compose-logs() {
    declare desc="Whach all container logs in colored version"

    dockerCompose logs
}
compose-generate-yaml() {
    declare desc="Generating docker-compose.yml based on Profile settings"

    cloudbreak-config

    if [ -f docker-compose.yml ]; then
         compose-generate-yaml-force /tmp/docker-compose-delme.yml
         if diff /tmp/docker-compose-delme.yml docker-compose.yml &>/dev/null; then
             debug "docker-compose.yml already exist, and generate wouldn't change it."
        else
            warn "docker-compose.yml already exists, BUT generate would create a DIFFERENT one!"
            warn "if you want to regenerate, move it away ..., expected change:"
            (diff /tmp/docker-compose-delme.yml docker-compose.yml || true) | cyan
         fi
    else
        compose-generate-yaml-force docker-compose.yml
    fi
}

compose-generate-yaml-force() {

    declare comoseFile=${1:? required: compose file path}
    info "Generating docker-compose yaml: ${comoseFile} ..."
    cat > ${comoseFile} <<EOF
consul:
    privileged: true
    volumes:
        - "/var/run/docker.sock:/var/run/docker.sock"
    ports:
        - "$PRIVATE_IP:53:53/udp"
        - "8400:8400"
        - "8500:8500"
    hostname: node1
    image: sequenceiq/consul:$DOCKER_TAG_CONSUL
    command: --server --bootstrap --advertise $PRIVATE_IP

registrator:
    privileged: true
    volumes:
        - "/var/run/docker.sock:/tmp/docker.sock"
    image: gliderlabs/registrator:$DOCKER_TAG_REGISTRATOR
    links:
        - consul
    command: consul://consul:8500

ambassador:
    privileged: true
    volumes:
        - "/var/run/docker.sock:/var/run/docker.sock"
    dns: $PRIVATE_IP
    image: progrium/ambassadord:$DOCKER_TAG_AMBASSADOR
    command: --omnimode

ambassadorips:
    privileged: true
    net: container:ambassador
    image: progrium/ambassadord:$DOCKER_TAG_AMBASSADOR
    command: --setup-iptables

uaadb:
    privileged: true
    ports:
        - 5432
    environment:
      - SERVICE_NAME=uaadb
        #- SERVICE_CHECK_CMD=bash -c 'psql -h 127.0.0.1 -p 5432  -U postgres -c "select 1"'
    volumes:
        - "$CB_DB_ROOT_PATH/uaadb:/var/lib/postgresql/data"
    image: postgres:$DOCKER_TAG_POSTGRES

identity:
    ports:
        - 8089:8080
    environment:
        - SERVICE_NAME=identity
        # - SERVICE_CHECK_HTTP=/login
        - IDENTITY_DB_URL=mydb:5432
        - BACKEND_5432=uaadb.service.consul
    links:
        - ambassador:mydb
    volumes:
      - uaa.yml:/uaa/uaa.yml
    image: sequenceiq/uaa:$DOCKER_TAG_UAA

cbdb:
    ports:
        - 5432
    environment:
      - SERVICE_NAME=cbdb
        #- SERVICE_CHECK_CMD=bash -c 'psql -h 127.0.0.1 -p 5432  -U postgres -c "select 1"'
    volumes:
        - "$CB_DB_ROOT_PATH/cbdb:/var/lib/postgresql/data"
    image: sequenceiq/cbdb:$DOCKER_TAG_CBDB

cloudbreak:
    environment:
        - AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
        - AWS_SECRET_KEY=$AWS_SECRET_KEY
        - SERVICE_NAME=cloudbreak
          #- SERVICE_CHECK_HTTP=/info
        - CB_CLIENT_ID=$UAA_CLOUDBREAK_ID
        - CB_CLIENT_SECRET=$UAA_CLOUDBREAK_SECRET
        - CB_BLUEPRINT_DEFAULTS=$CB_BLUEPRINT_DEFAULTS
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
        - ENDPOINTS_AUTOCONFIG_ENABLED=false
        - ENDPOINTS_DUMP_ENABLED=false
        - ENDPOINTS_TRACE_ENABLED=false
        - ENDPOINTS_CONFIGPROPS_ENABLED=false
        - ENDPOINTS_METRICS_ENABLED=false
        - ENDPOINTS_MAPPINGS_ENABLED=false
        - ENDPOINTS_BEANS_ENABLED=false
        - ENDPOINTS_ENV_ENABLED=false
        - CB_IDENTITY_SERVER_URL=http://backend:8089
        - CB_DB_PORT_5432_TCP_ADDR=backend
        - CB_DB_PORT_5432_TCP_PORT=5432
        - BACKEND_5432=cbdb.service.consul
        - BACKEND_8089=identity.service.consul
    links:
        - ambassador:backend
    ports:
        - 8080:8080
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
        - SL_CB_ADDRESS=http://$PUBLIC_IP:3000
        - SL_ADDRESS=http://$PUBLIC_IP:3001
        - SL_UAA_ADDRESS=http://backend:8089
        - BACKEND_8089=identity.service.consul
    links:
        - ambassador:backend
    ports:
        - 3001:3000
    image: sequenceiq/sultans-bin:$DOCKER_TAG_SULTANS

uluwatu:
    environment:
        - ULU_PRODUCTION=false
        - SERVICE_NAME=uluwatu
          #- SERVICE_CHECK_HTTP=/
        - ULU_OAUTH_REDIRECT_URI=http://$PUBLIC_IP:3000/authorize
        - ULU_SULTANS_ADDRESS=http://$PUBLIC_IP:3001
        - ULU_OAUTH_CLIENT_ID=$UAA_ULUWATU_ID
        - ULU_OAUTH_CLIENT_SECRET=$UAA_ULUWATU_SECRET
        - ULU_HOST_ADDRESS=http://$PUBLIC_IP:3000
        - NODE_TLS_REJECT_UNAUTHORIZED=0

        - ULU_IDENTITY_ADDRESS=http://backend:8089/
        - ULU_CLOUDBREAK_ADDRESS=http://backend:8080
        - ULU_PERISCOPE_ADDRESS=http://backend:8085/
        - BACKEND_8089=identity.service.consul
        - BACKEND_8080=cloudbreak.service.consul
        - BACKEND_8085=periscope.service.consul
    links:
        - ambassador:backend
    ports:
        - 3000:3000
    image: sequenceiq/uluwatu-bin:$DOCKER_TAG_ULUWATU

pcdb:
    environment:
        - SERVICE_NAME=pcdb
     #- SERVICE_NAMEE_CHECK_CMD='psql -h 127.0.0.1 -p 5432  -U postgres -c "select 1"'
    ports:
        - 5432
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
        - ENDPOINTS_AUTOCONFIG_ENABLED=false
        - ENDPOINTS_DUMP_ENABLED=false
        - ENDPOINTS_TRACE_ENABLED=false
        - ENDPOINTS_CONFIGPROPS_ENABLED=false
        - ENDPOINTS_METRICS_ENABLED=false
        - ENDPOINTS_MAPPINGS_ENABLED=false
        - ENDPOINTS_BEANS_ENABLED=false
        - ENDPOINTS_ENV_ENABLED=false
        - PERISCOPE_DB_TCP_ADDR=backend
        - PERISCOPE_DB_TCP_PORT=5433
        - PERISCOPE_CLOUDBREAK_URL=http://backend:8080
        - PERISCOPE_IDENTITY_SERVER_URL=http://backend:8089/
        - BACKEND_8080=cloudbreak.service.consul
        - BACKEND_5433=pcdb.service.consul
        - BACKEND_8089=identity.service.consul
    links:
        - ambassador:backend
    ports:
        - 8085:8080
    image: sequenceiq/periscope:$DOCKER_TAG_PERISCOPE

EOF
}
