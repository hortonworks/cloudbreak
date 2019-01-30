compose-init() {
    if (docker-compose --version 2>&1| grep -q 1.2.0); then
        echo "* removing old docker-compose binary" | yellow
        rm -f .deps/bin/docker-compose
    fi
    deps-require docker-compose 1.13.0
    env-import CB_COMPOSE_PROJECT cbreak
    env-import COMPOSE_HTTP_TIMEOUT 120
    env-import DOCKER_STOP_TIMEOUT 60
    env-import CBD_LOG_NAME cbreak
    env-import ULUWATU_VOLUME_HOST /dev/null
    env-import ULUWATU_CONTAINER_PATH /hortonworks-cloud-web

    if [[ "$ULUWATU_VOLUME_HOST" != "/dev/null" ]]; then
      ULUWATU_VOLUME_CONTAINER=${ULUWATU_CONTAINER_PATH}
    else
      ULUWATU_VOLUME_CONTAINER=/tmp/null
    fi
    env-import SULTANS_VOLUME_HOST /dev/null
    env-import SULTANS_CONTAINER_PATH /hortonworks-cloud-auth
    if [[ "$SULTANS_VOLUME_HOST" != "/dev/null" ]]; then
      SULTANS_VOLUME_CONTAINER=${SULTANS_CONTAINER_PATH}
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
    cloudbreak-conf-tags

    [ -f docker-compose.yml ] || deployer-generate

    dockerCompose pull
}

compose-pull-parallel() {
    declare desc="Pulls service images parallel"
    cloudbreak-conf-tags

    [ -f docker-compose.yml ] || deployer-generate
    sed -n "s/.*image://p" docker-compose.yml|sort -u|xargs -n1 -P 20 docker pull
}

compose-up() {
    dockerCompose up -d "$@"
}

compose-kill() {
    declare desc="Kills and removes all cloudbreak related container"

    dockerCompose stop --timeout ${DOCKER_STOP_TIMEOUT}
    dockerCompose rm -f
}

util-cleanup() {
    declare desc="Removes all exited containers and old cloudbreak related images"

    if [ ! -f docker-compose.yml ]; then
      error "docker-compose.yml file does not exists"
      _exit 126
    fi

    compose-remove-exited-containers

    local all_images=$(docker images | grep -v "<none>"| sed "s/ \+/ /g"|cut -d' ' -f 1,2|tr ' ' : | tail -n +2)
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
    declare desc='Follow all logs. Starts from begining. Separate service names by space to filter, e.g. "cbd logs cloudbreak uluwatu"'

    disable_cbd_output_copy_to_log

    dockerCompose logs -f "$@"
}

compose-logs-tail() {
    declare desc='Same as "logs" but doesnt includes previous messages'

    disable_cbd_output_copy_to_log

    dockerCompose logs -f --tail=1 "$@"
}

compose-generate-check-diff() {
    cloudbreak-config
    setup_proxy_environments
    local verbose="$1"

    if [ -f docker-compose.yml ]; then
        local compose_delme_path=$TEMP_DIR/docker-compose-delme.yml
         compose-generate-yaml-force $compose_delme_path
         if diff $compose_delme_path docker-compose.yml &>/dev/null; then
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
                diff $compose_delme_path docker-compose.yml || true
            else
                debug "expected change:"
                (diff $compose_delme_path docker-compose.yml || true) | debug-cat
            fi

            if [[ !"$CBD_FORCE_START" ]]; then
                return 1
            fi
        fi
    fi
    return 0
}

compose-generate-yaml() {
    declare desc="Generating docker-compose.yml based on Profile settings"

    cloudbreak-config
    setup_proxy_environments

    if ! compose-generate-check-diff; then
        if [[ "$CBD_FORCE_START" ]]; then
            warn "You have forced to start ..."
        else
            warn "Please check the expected config changes with:"
            echo "  cbd doctor" | blue
            debug "If you want to ignore the changes, set the CBD_FORCE_START to true in Profile"
            _exit 1
        fi
    else
        info "generating docker-compose.yml"
        compose-generate-yaml-force docker-compose.yml
        docker-compose -f docker-compose.yml config 1> /dev/null
    fi
}

escape-string-compose-yaml() {
    declare desc="Escape compose yaml string by delimiter type"
    : ${2:=required}
    local in=$1
    local delimiter=$2

    if [[ $delimiter == "'" ]]; then
        out=`echo $in | sed -e "s/'/''/g" -e 's/[$]/$$/g'`
    elif [[ $delimiter == '"' ]]; then
		out=`echo $in | sed -e 's/\\\\/\\\\\\\/g' -e 's/"/\\\"/g' -e 's/[$]/$$/g'`
    else
        out="$in"
    fi

    echo $out
}

compose-generate-yaml-force() {
    declare composeFile=${1:? required: compose file path}
    debug "Generating docker-compose yaml: ${composeFile} ..."
    if [[ -z "$AWS_SECRET_ACCESS_KEY" && -n "$AWS_SECRET_KEY"  ]]; then
        debug "AWS_SECRET_ACCESS_KEY is not set, fall back to deprecated AWS_SECRET_KEY"
        export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_KEY
    fi
    cat > ${composeFile} <<EOF
traefik:
    ports:
        - "$PRIVATE_IP:8081:8080"
        - $PUBLIC_HTTP_PORT:80
        - $PUBLIC_HTTPS_PORT:443
    links:
        - consul
        - identity
        - sultans
        - uluwatu
    volumes:
        - /var/run/docker.sock:/var/run/docker.sock
        - $CBD_CERT_ROOT_PATH/traefik:/certs/traefik
        - ./logs/traefik:/opt/traefik/log/
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: traefik:$DOCKER_TAG_TRAEFIK
    restart: on-failure
    command: --debug --web --InsecureSkipVerify=true \
        --defaultEntryPoints=http,https \
        --entryPoints='Name:http Address::80 Redirect.EntryPoint:https' \
        --entryPoints='Name:https Address::443 TLS:$CBD_TRAEFIK_TLS' \
        --maxidleconnsperhost=$TRAEFIK_MAX_IDLE_CONNECTION \
        --traefikLogsFile=/opt/traefik/log/traefik.log \
        --accessLogsFile=/opt/traefik/log/access.log \
        --docker \
        --consul --consul.endpoint=consul:8500
haveged:
    labels:
      - traefik.enable=false
    privileged: true
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: hortonworks/haveged:$DOCKER_TAG_HAVEGED

consul:
    labels:
      - traefik.enable=false
    privileged: true
    environment:
        - http_proxy=$HTTP_PROXY
        - https_proxy=$HTTPS_PROXY
    volumes:
        - "/var/run/docker.sock:/var/run/docker.sock"
        - consul-data:/data
    ports:
        - "$PRIVATE_IP:53:8600/udp"
        - "8400:8400"
        - "8500:8500"
    hostname: node1
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: gliderlabs/consul-server:$DOCKER_TAG_CONSUL
    command: --bootstrap --advertise $PRIVATE_IP $DOCKER_CONSUL_OPTIONS

registrator:
    labels:
      - traefik.enable=false
    privileged: true
    volumes:
        - "/var/run/docker.sock:/tmp/docker.sock"
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: gliderlabs/registrator:$DOCKER_TAG_REGISTRATOR
    links:
        - consul
    restart: on-failure
    command: consul://consul:8500

logsink:
    labels:
      - traefik.enable=false
    ports:
        - 3333
    environment:
        - SERVICE_NAME=logsink
    volumes:
        - ./logs:/tmp
    image: hortonworks/socat:1.0.0
    log_opt:
        max-size: "10M"
        max-file: "5"
    command: socat -u TCP-LISTEN:3333,reuseaddr,fork OPEN:/tmp/cbreak.log,creat,append

logspout:
    labels:
      - traefik.enable=false
    ports:
        - 8000:80
    environment:
        - SERVICE_NAME=logspout
        - DEBUG=true
        - LOGSPOUT=ignore
        - "RAW_FORMAT={{.Container.Name}} | {{.Data}}\n"
    links:
        - logsink
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    entrypoint: ["/bin/sh"]
    command: -c 'sleep 1; (ROUTE_URIS=\$\$LOGSINK_PORT_3333_TCP /bin/logspout) & LSPID=\$\$!; trap "kill \$\$LSPID; wait \$\$LSPID" SIGINT SIGTERM; wait \$\$LSPID'
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: hortonworks/logspout:v3.2.2

logrotate:
    environment:
        - "CRON_EXPR=0 * * * *"
        - "LOGROTATE_LOGFILES=/var/log/cloudbreak-deployer/*.log /var/log/cloudbreak-deployer/*/*.log"
        - LOGROTATE_FILESIZE=10M
    volumes:
        - ./logs:/var/log/cloudbreak-deployer
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: hortonworks/logrotate:$DOCKER_TAG_LOGROTATE

mail:
    labels:
      - traefik.enable=false
    ports:
        - "$PRIVATE_IP:25:25"
    environment:
        - SERVICE_NAME=smtp
        - maildomain=example.com
        - 'smtp_user=admin:$(escape-string-compose-yaml $LOCAL_SMTP_PASSWORD \')'
    entrypoint: ["/bin/sh"]
    command: -c '/opt/install.sh; (/usr/bin/supervisord -c /etc/supervisor/supervisord.conf) & SUPERVISORDPID="\$\$!"; trap "kill \$\$SUPERVISORDPID; wait \$\$SUPERVISORDPID" INT TERM; wait \$\$SUPERVISORDPID'
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: catatnight/postfix:$DOCKER_TAG_POSTFIX

smartsense:
    labels:
        - traefik.enable=false
    ports:
        - "9000:9000"
    environment:
        - ACCOUNT_ID=$AWS_ACCOUNT_ID
        - CB_VERSION=$(echo $(bin-version))
        - CB_SMARTSENSE_CONFIGURE
        - CB_SMARTSENSE_ID
        - CB_SMARTSENSE_CLUSTER_NAME_PREFIX
        - CB_INSTANCE_UUID
        - CB_INSTANCE_PROVIDER
        - CB_INSTANCE_REGION
        - CB_PRODUCT_ID
        - CB_COMPONENT_ID
        - CAPTURE_CRON_EXPRESSION
        - UAA_FLEX_USAGE_CLIENT_ID
        - UAA_FLEX_USAGE_CLIENT_SECRET
        - SMARTSENSE_UPLOAD_HOST
        - SMARTSENSE_UPLOAD_USERNAME
        - SMARTSENSE_UPLOAD_PASSWORD
    dns: $PRIVATE_IP
    volumes:
        - .:/var/lib/cloudbreak-deployment
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: $DOCKER_IMAGE_CBD_SMARTSENSE:$DOCKER_TAG_CBD_SMARTSENSE

commondb:
    labels:
      - traefik.enable=false
    privileged: true
    ports:
        - "$PRIVATE_IP:5432:5432"
    environment:
      - SERVICE_NAME=$COMMON_DB
        #- SERVICE_CHECK_CMD=bash -c 'psql -h 127.0.0.1 -p 5432  -U postgres -c "select 1"'
    volumes:
        - "$COMMON_DB_VOL:/var/lib/postgresql/data"
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: postgres:$DOCKER_TAG_POSTGRES
    entrypoint: ["/bin/bash"]
    command: -c 'cd /var/lib/postgresql; touch .ash_history .psql_history; chown -R postgres:postgres /var/lib/postgresql; (/docker-entrypoint.sh postgres -c max_connections=300) & PGPID="\$\$!"; echo "PGPID \$\$PGPID"; trap "kill \$\$PGPID; wait \$\$PGPID" SIGINT SIGTERM; cd /var/lib/postgresql; (tail -f .*history) & wait "\$\$PGPID"'

identity:
    labels:
      - traefik.port=8080
      - traefik.frontend.rule=PathPrefix:/identity/check_token,/identity/oauth,/identity/Users,/identity/login.do,/identity/Groups;PathPrefixStrip:/identity
      - traefik.backend=identity-backend
      - traefik.frontend.priority=10
    ports:
        - $UAA_PORT:8080
    environment:
        - http_proxy=$HTTP_PROXY
        - https_proxy=$HTTPS_PROXY
        - SERVICE_NAME=identity
        # - SERVICE_CHECK_HTTP=/login
        - IDENTITY_DB_URL
        - IDENTITY_DB_NAME
        - IDENTITY_DB_USER
        - IDENTITY_DB_PASS
    dns: $PRIVATE_IP
    volumes:
      - ./uaa.yml:/uaa/uaa.yml
      - ./logs/identity:/tomcat/logs/
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: hortonworks/cloudbreak-uaa:$DOCKER_TAG_UAA

cloudbreak:
    environment:
        - AWS_ACCESS_KEY_ID
        - AWS_SECRET_ACCESS_KEY
        - AWS_GOV_ACCESS_KEY_ID
        - AWS_GOV_SECRET_ACCESS_KEY
        - "SERVICE_NAME=cloudbreak"
          #- SERVICE_CHECK_HTTP=/info
        - "http_proxy=$HTTP_PROXY"
        - "https_proxy=$HTTPS_PROXY"
        - 'CB_JAVA_OPTS=$(escape-string-compose-yaml "$CB_JAVA_OPTS" \')'
        - "HTTPS_PROXYFORCLUSTERCONNECTION=$HTTPS_PROXYFORCLUSTERCONNECTION"
        - "CB_CLIENT_ID=$UAA_CLOUDBREAK_ID"
        - 'CB_CLIENT_SECRET=$(escape-string-compose-yaml $UAA_CLOUDBREAK_SECRET \')'
        - CB_BLUEPRINT_DEFAULTS
        - CB_BLUEPRINT_INTERNAL
        - CB_TEMPLATE_DEFAULTS
        - CB_HBM2DDL_STRATEGY
        - CB_CAPABILITIES
        $( if [[ -n "$INFO_APP_CAPABILITIES" ]]; then echo "- INFO_APP_CAPABILITIES"; fi )
        - "CB_SMTP_SENDER_USERNAME=$CLOUDBREAK_SMTP_SENDER_USERNAME"
        - 'CB_SMTP_SENDER_PASSWORD=$(escape-string-compose-yaml $CLOUDBREAK_SMTP_SENDER_PASSWORD \')'
        - "CB_SMTP_SENDER_HOST=$CLOUDBREAK_SMTP_SENDER_HOST"
        - "CB_SMTP_SENDER_PORT=$CLOUDBREAK_SMTP_SENDER_PORT"
        - "CB_SMTP_SENDER_FROM=$CLOUDBREAK_SMTP_SENDER_FROM"
        - "ENDPOINTS_AUTOCONFIG_ENABLED=false"
        - "ENDPOINTS_DUMP_ENABLED=false"
        - "ENDPOINTS_TRACE_ENABLED=false"
        - "ENDPOINTS_CONFIGPROPS_ENABLED=false"
        - "ENDPOINTS_METRICS_ENABLED=false"
        - "ENDPOINTS_MAPPINGS_ENABLED=false"
        - "ENDPOINTS_BEANS_ENABLED=false"
        - "ENDPOINTS_ENV_ENABLED=false"
        - "CB_ADDRESS_RESOLVING_TIMEOUT"
        - "CB_IDENTITY_SERVICEID=identity.service.consul"
        - CB_DB_PORT_5432_TCP_ADDR
        - CB_DB_PORT_5432_TCP_PORT
        - CB_DB_ENV_USER
        - CB_DB_ENV_PASS
        - CB_DB_ENV_DB
        - CB_DB_ENV_SCHEMA
        - "CB_DB_SERVICEID=$COMMON_DB.service.consul"
        - "CB_MAIL_SMTP_AUTH=$CLOUDBREAK_SMTP_AUTH"
        - "CB_MAIL_SMTP_STARTTLS_ENABLE=$CLOUDBREAK_SMTP_STARTTLS_ENABLE"
        - "CB_MAIL_SMTP_TYPE=$CLOUDBREAK_SMTP_TYPE"
        - CB_SCHEMA_SCRIPTS_LOCATION
        - CB_SCHEMA_MIGRATION_AUTO
        - "SPRING_CLOUD_CONSUL_HOST=consul.service.consul"
        - CB_AWS_HOSTKEY_VERIFY
        - CB_GCP_HOSTKEY_VERIFY
        - REST_DEBUG
        - CB_AWS_DEFAULT_CF_TAG
        - CB_AWS_CUSTOM_CF_TAGS
        - CERT_VALIDATION
        - CB_HOST_DISCOVERY_CUSTOM_DOMAIN
        - CB_SMARTSENSE_CONFIGURE
        - CB_SMARTSENSE_ID
        - "CB_BYOS_DFS_DATA_DIR=$CB_BYOS_DFS_DATA_DIR"
        - HWX_CLOUD_TEMPLATE_VERSION
        - "HWX_CLOUD_ADDRESS=$PUBLIC_IP"
        - CB_PLATFORM_DEFAULT_REGIONS
        - CB_DEFAULT_SUBSCRIPTION_ADDRESS
        $( if [[ -n "$CB_IMAGE_CATALOG_URL" ]]; then echo "- CB_IMAGE_CATALOG_URL"; fi )
        - CB_AWS_DEFAULT_INBOUND_SECURITY_GROUP
        - CB_AWS_VPC
        - CB_ENABLEDPLATFORMS
        - CB_ENABLED_LINUX_TYPES
        - CB_MAX_SALT_NEW_SERVICE_RETRY
        - CB_MAX_SALT_NEW_SERVICE_RETRY_ONERROR
        - CB_MAX_SALT_RECIPE_EXECUTION_RETRY
        - CB_INSTANCE_UUID
        - CB_INSTANCE_NODE_ID
        - CB_INSTANCE_PROVIDER
        - CB_INSTANCE_REGION
        - CB_PRODUCT_ID
        - CB_COMPONENT_ID
        - CB_COMPONENT_CREATED
        - CB_COMPONENT_CLUSTER_ID
        - CB_LOG_LEVEL
        - CB_DEFAULT_GATEWAY_CIDR
        $( if [[ "$CB_AUDIT_FILE_ENABLED" = true ]]; then echo "- CB_AUDIT_FILEPATH=/cloudbreak-log/cb-audit.log"; fi )
        $( if [[ -n "$CB_KAFKA_BOOTSTRAP_SERVERS" ]]; then echo "- CB_KAFKA_BOOTSTRAP_SERVERS"; fi )
        - CB_DISABLE_SHOW_CLI
        - CB_DISABLE_SHOW_BLUEPRINT
        - SMARTSENSE_UPLOAD_HOST
        - SMARTSENSE_UPLOAD_USERNAME
        - SMARTSENSE_UPLOAD_PASSWORD
    labels:
      - traefik.port=8080
      - traefik.frontend.rule=PathPrefix:/cb/
      - traefik.backend=cloudbreak-backend
      - traefik.frontend.priority=10
    ports:
        - $CB_PORT:8080
    volumes:
        - "$CBD_CERT_ROOT_PATH:/certs"
        - /dev/urandom:/dev/random
        - ./logs/cloudbreak:/cloudbreak-log
        - ./etc/:/etc/cloudbreak
    dns: $PRIVATE_IP
    links:
        - consul
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: $DOCKER_IMAGE_CLOUDBREAK:$DOCKER_TAG_CLOUDBREAK
    command: bash

sultans:
    environment:
        - http_proxy=$HTTP_PROXY
        - https_proxy=$HTTPS_PROXY
        - SL_CLIENT_ID=$UAA_SULTANS_ID
        - 'SL_CLIENT_SECRET=$(escape-string-compose-yaml $UAA_SULTANS_SECRET \')'
        - SERVICE_NAME=sultans
        - SERVICE_3000_NAME=sultans
          #- SERVICE_CHECK_HTTP=/
        - SL_PORT=3000
        - SL_SMTP_SENDER_HOST=$CLOUDBREAK_SMTP_SENDER_HOST
        - SL_SMTP_SENDER_PORT=$CLOUDBREAK_SMTP_SENDER_PORT
        - SL_SMTP_SENDER_USERNAME=$CLOUDBREAK_SMTP_SENDER_USERNAME
        - "SL_SMTP_SENDER_PASSWORD=$(escape-string-compose-yaml $CLOUDBREAK_SMTP_SENDER_PASSWORD \")"
        - SL_SMTP_SENDER_FROM=$CLOUDBREAK_SMTP_SENDER_FROM
        - HWX_CLOUD_COLLECTOR=$CLOUDBREAK_TELEMETRY_MAIL_ADDRESS
        - HWX_CLOUD_USER=$UAA_DEFAULT_USER_EMAIL
        - HWX_CLOUD_TYPE
        - HWX_CLOUD_TEMPLATE_VERSION
        - AWS_AMI_ID
        - AWS_INSTANCE_ID
        - AWS_ACCOUNT_ID
        - HWX_DOC_LINK
        - SL_SMARTSENSE_CONFIGURE=$CB_SMARTSENSE_CONFIGURE
        - SL_CB_ADDRESS=$ULU_HOST_ADDRESS
        - SL_ADDRESS=$ULU_SULTANS_ADDRESS
        - SL_HWX_CLOUD_DEFAULT_REGION=$ULU_HWX_CLOUD_DEFAULT_REGION
        - SL_ADDRESS_RESOLVING_TIMEOUT
        - NODE_TLS_REJECT_UNAUTHORIZED=$SL_NODE_TLS_REJECT_UNAUTHORIZED
        - SL_UAA_SERVICEID=identity.service.consul
        - SL_DISPLAY_TERMS_AND_SERVICES=$HWX_DISPLAY_TERMS_AND_CONDITIONS
    labels:
      - traefik.port=3000
      - traefik.frontend.rule=PathPrefixStrip:/sl
      - traefik.backend=sultans-backend
      - traefik.frontend.priority=10
    ports:
        - 3001:3000
    volumes:
        - $SULTANS_VOLUME_HOST:$SULTANS_VOLUME_CONTAINER
    dns: $PRIVATE_IP
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: $DOCKER_IMAGE_CLOUDBREAK_AUTH:$DOCKER_TAG_SULTANS

uluwatu:
    environment:
        - http_proxy=$HTTP_PROXY
        - https_proxy=$HTTPS_PROXY
        - SERVICE_NAME=uluwatu
          #- SERVICE_CHECK_HTTP=/
        - ULU_OAUTH_REDIRECT_URI
        - ULU_DEFAULT_SSH_KEY
        - ULU_SULTANS_ADDRESS
        - ULU_OAUTH_CLIENT_ID=$UAA_ULUWATU_ID
        - 'ULU_OAUTH_CLIENT_SECRET=$(escape-string-compose-yaml $UAA_ULUWATU_SECRET \')'
        - ULU_HOST_ADDRESS
        - NODE_TLS_REJECT_UNAUTHORIZED=$ULU_NODE_TLS_REJECT_UNAUTHORIZED
        - ULU_HWX_CLOUD_DEFAULT_CREDENTIAL
        - ULU_HWX_CLOUD_DEFAULT_REGION
        - ULU_HWX_CLOUD_DEFAULT_SSH_KEY
        - ULU_HWX_CLOUD_DEFAULT_VPC_ID
        - ULU_HWX_CLOUD_DEFAULT_IGW_ID
        - ULU_HWX_CLOUD_DEFAULT_SUBNET_ID
        - ULU_HWX_CLOUD_DEFAULT_ARM_VIRTUAL_NETWORK_ID
        - HWX_CLOUD_TEMPLATE_VERSION
        - HWX_CLOUD_ENABLE_GOVERNANCE_AND_SECURITY
        - ULU_ADDRESS_RESOLVING_TIMEOUT
        - ULU_SULTANS_SERVICEID=sultans.service.consul
        - ULU_IDENTITY_SERVICEID=identity.service.consul
        - ULU_CLOUDBREAK_SERVICEID=cloudbreak.service.consul
        - ULU_PERISCOPE_SERVICEID=periscope.service.consul
        - ULU_HWX_CLOUD_REGISTRATION_URL
        - ULU_SUBSCRIBE_TO_NOTIFICATIONS
        - AWS_INSTANCE_ID
        - HWX_HCC_AVAILABLE
        - AWS_ACCOUNT_ID
        - AWS_AMI_ID
        - HWX_DOC_LINK
        - AZURE_TENANT_ID
        - AZURE_SUBSCRIPTION_ID
        - AWS_ACCESS_KEY_ID
        - AWS_SECRET_ACCESS_KEY
    labels:
      - traefik.port=3000
      - traefik.frontend.rule=Host:$PUBLIC_IP,$CB_TRAEFIK_HOST_ADDRESS
      - traefik.backend=uluwatu-backend
      - traefik.frontend.priority=5
    ports:
        - 3000:3000
    volumes:
        - $ULUWATU_VOLUME_HOST:$ULUWATU_VOLUME_CONTAINER
    dns: $PRIVATE_IP
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: $DOCKER_IMAGE_CLOUDBREAK_WEB:$DOCKER_TAG_ULUWATU

periscope:
    environment:
        - http_proxy=$HTTP_PROXY
        - https_proxy=$HTTPS_PROXY
        - PERISCOPE_HBM2DDL_STRATEGY
        - PERISCOPE_DB_PORT_5432_TCP_ADDR
        - PERISCOPE_DB_PORT_5432_TCP_PORT
        - PERISCOPE_DB_ENV_USER
        - PERISCOPE_DB_ENV_PASS
        - PERISCOPE_DB_ENV_DB
        - PERISCOPE_DB_ENV_SCHEMA
        - "HTTPS_PROXYFORCLUSTERCONNECTION=$HTTPS_PROXYFORCLUSTERCONNECTION"
        - SERVICE_NAME=periscope
          #- SERVICE_CHECK_HTTP=/info
        - 'CB_JAVA_OPTS=$(escape-string-compose-yaml "$CB_JAVA_OPTS" \')'
        - PERISCOPE_CLIENT_ID=$UAA_PERISCOPE_ID
        - 'PERISCOPE_CLIENT_SECRET=$(escape-string-compose-yaml $UAA_PERISCOPE_SECRET \')'
        - PERISCOPE_HOSTNAME_RESOLUTION=public
        - ENDPOINTS_AUTOCONFIG_ENABLED=false
        - ENDPOINTS_DUMP_ENABLED=false
        - ENDPOINTS_TRACE_ENABLED=false
        - ENDPOINTS_CONFIGPROPS_ENABLED=false
        - ENDPOINTS_METRICS_ENABLED=false
        - ENDPOINTS_MAPPINGS_ENABLED=false
        - ENDPOINTS_BEANS_ENABLED=false
        - ENDPOINTS_ENV_ENABLED=false
        - PERISCOPE_ADDRESS_RESOLVING_TIMEOUT
        - PERISCOPE_DB_SERVICEID=$COMMON_DB.service.consul
        - PERISCOPE_CLOUDBREAK_SERVICEID=cloudbreak.service.consul
        - PERISCOPE_IDENTITY_SERVICEID=identity.service.consul
        - PERISCOPE_SCHEMA_SCRIPTS_LOCATION
        - PERISCOPE_SCHEMA_MIGRATION_AUTO
        - PERISCOPE_INSTANCE_NODE_ID=$CB_INSTANCE_NODE_ID
        - PERISCOPE_LOG_LEVEL
        - REST_DEBUG
        - CERT_VALIDATION
        - CB_DEFAULT_SUBSCRIPTION_ADDRESS
    labels:
      - traefik.port=8080
      - traefik.frontend.rule=PathPrefix:/as/
      - traefik.backend=periscope-backend
      - traefik.frontend.priority=10
    ports:
        - 8085:8080
    dns: $PRIVATE_IP
    volumes:
        - "$CBD_CERT_ROOT_PATH:/certs"
        - ./logs/autoscale:/autoscale-log
        - /dev/urandom:/dev/random
    log_opt:
        max-size: "10M"
        max-file: "5"
    image: $DOCKER_IMAGE_CLOUDBREAK_PERISCOPE:$DOCKER_TAG_PERISCOPE

EOF
}
