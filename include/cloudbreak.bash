
cloudbreak-config() {
  : ${BRIDGE_IP:=$(docker run --rm --name=cbreak_cbd_bridgeip --label cbreak.sidekick=true alpine sh -c 'ip ro | grep default | cut -d" " -f 3')}
  env-import PRIVATE_IP $BRIDGE_IP
  env-import DOCKER_MACHINE ""
  cloudbreak-conf-tags
  cloudbreak-conf-images
  cloudbreak-conf-capabilities
  cloudbreak-conf-cert
  cloudbreak-conf-db
  cloudbreak-conf-defaults
  cloudbreak-conf-autscale
  cloudbreak-conf-uaa
  cloudbreak-conf-smtp
  cloudbreak-conf-cloud-provider
  cloudbreak-conf-rest-client
  cloudbreak-conf-ui
  cloudbreak-conf-java
  cloudbreak-conf-consul
  cloudbreak-conf-proxy
  migrate-config
}

cloudbreak-conf-tags() {
    declare desc="Defines docker image tags"

    env-import DOCKER_TAG_ALPINE 3.1
    env-import DOCKER_TAG_HAVEGED 1.1.0
    env-import DOCKER_TAG_TRAEFIK v1.6.6-alpine
    env-import DOCKER_TAG_CONSUL 0.5
    env-import DOCKER_TAG_REGISTRATOR v7
    env-import DOCKER_TAG_POSTFIX latest
    env-import DOCKER_TAG_UAA 3.6.5-certs
    env-import DOCKER_TAG_AMBASSADOR 0.5.0
    env-import DOCKER_TAG_CERT_TOOL 0.2.0

    env-import DOCKER_TAG_PERISCOPE 2.9.1
    env-import DOCKER_TAG_CLOUDBREAK 2.9.1
    env-import DOCKER_TAG_ULUWATU 2.9.1
    env-import DOCKER_TAG_SULTANS 2.9.1

    env-import DOCKER_TAG_POSTGRES 9.6.1-alpine
    env-import DOCKER_TAG_LOGROTATE 1.0.1
    env-import DOCKER_TAG_CBD_SMARTSENSE 0.13.4
    env-import DOCKER_TAG_LOGSINK 1.0.0
    env-import DOCKER_TAG_LOGSPOUT v3.2.2

    env-import DOCKER_IMAGE_CLOUDBREAK hortonworks/cloudbreak
    env-import DOCKER_IMAGE_CLOUDBREAK_WEB hortonworks/hdc-web
    env-import DOCKER_IMAGE_CLOUDBREAK_AUTH hortonworks/hdc-auth
    env-import DOCKER_IMAGE_CLOUDBREAK_PERISCOPE hortonworks/cloudbreak-autoscale
    env-import DOCKER_IMAGE_CBD_SMARTSENSE hortonworks/cbd-smartsense

    env-import DOCKER_IMAGE_CLOUDBREAK_UAA hortonworks/cloudbreak-uaa
    env-import DOCKER_IMAGE_CBD_TRAEFIK traefik
    env-import DOCKER_IMAGE_CBD_HAVEGED hortonworks/haveged
    env-import DOCKER_IMAGE_CBD_CONSUL gliderlabs/consul-server
    env-import DOCKER_IMAGE_CBD_REGISTRATOR gliderlabs/registrator
    env-import DOCKER_IMAGE_CBD_LOGSINK hortonworks/socat
    env-import DOCKER_IMAGE_CBD_LOGSPOUT hortonworks/logspout
    env-import DOCKER_IMAGE_CBD_LOGROTATE hortonworks/logrotate
    env-import DOCKER_IMAGE_CBD_POSTFIX catatnight/postfix
    env-import DOCKER_IMAGE_CBD_POSTGRES postgres

    env-import CB_DEFAULT_SUBSCRIPTION_ADDRESS http://uluwatu.service.consul:3000/notifications
    env-import CERTS_BUCKET ""

}

docker-ip() {
    if [[ $DOCKER_HOST =~ "tcp://" ]];then
        local dip=${DOCKER_HOST#*//}
        echo ${dip%:*}
    else
        echo none
    fi
}

consul-recursors() {
    declare desc="Generates consul agent recursor option, by reading the hosts resolv.conf"
    declare resolvConf=${1:? 'required 1.param: resolv.conf file'}
    declare bridge=${2:? 'required 2.param: bridge ip'}
    declare dockerIP=${3:- none}

    local nameservers=$(sed -n "/^nameserver/ s/^.*nameserver[^0-9]*//p;" $resolvConf)
    debug "nameservers on host:\n$nameservers"
    if [[ "$nameservers" ]]; then
        debug bridge=$bridge
        echo "$nameservers" | grep -v "$bridge\|$dockerIP" | sed -n '{s/^/ -recursor /;H;}; $ {x;s/[\n\r]//g;p}'
    else
        echo
    fi
}

cloudbreak-conf-consul() {
    [[ "$cloudbreakConfConsulExecuted" ]] && return

    env-import DOCKER_CONSUL_OPTIONS ""
    if ! [[ $DOCKER_CONSUL_OPTIONS =~ .*recursor.* ]]; then
        DOCKER_CONSUL_OPTIONS="$DOCKER_CONSUL_OPTIONS $(consul-recursors <(cat /etc/resolv.conf 2>/dev/null || echo) ${BRIDGE_IP} $(docker-ip))"
    fi
    debug "DOCKER_CONSUL_OPTIONS=$DOCKER_CONSUL_OPTIONS"
    cloudbreakConfConsulExecuted=1
}

cloudbreak-conf-images() {
    declare desc="Defines image catalog urls"

    env-import CB_IMAGE_CATALOG_URL ""
}

cloudbreak-conf-capabilities() {
    declare desc="Enables capabilities"

    env-import CB_CAPABILITIES ""
    CB_CAPABILITIES=$(echo $CB_CAPABILITIES | awk '{print toupper($0)}')
    env-import INFO_APP_CAPABILITIES "$CB_CAPABILITIES"
}

cloudbreak-conf-smtp() {
    env-import LOCAL_SMTP_PASSWORD "$UAA_DEFAULT_USER_PW"
    if ! [[ "$LOCAL_SMTP_PASSWORD" ]]; then
        LOCAL_SMTP_PASSWORD="cloudbreak"
    fi

    env-import CLOUDBREAK_SMTP_SENDER_USERNAME "admin"
    env-import CLOUDBREAK_SMTP_SENDER_PASSWORD "$LOCAL_SMTP_PASSWORD"
    env-import CLOUDBREAK_SMTP_SENDER_HOST "smtp.service.consul"
    env-import CLOUDBREAK_SMTP_SENDER_PORT 25
    env-import CLOUDBREAK_SMTP_SENDER_FROM "noreply@hortonworks.com"
    env-import CLOUDBREAK_SMTP_AUTH "true"
    env-import CLOUDBREAK_SMTP_STARTTLS_ENABLE "false"
    env-import CLOUDBREAK_SMTP_TYPE "smtp"
    env-import CLOUDBREAK_TELEMETRY_MAIL_ADDRESS "aws-marketplace@hortonworks.com"
}

is_linux() {
    [[ "$(uname)" == Linux ]]
}

is_macos() {
    [[ "$(uname)" == Darwin ]]
}

cloudbreak-conf-db() {
    declare desc="Declares cloudbreak DB config"

    if is_linux; then
        env-import CB_DB_ROOT_PATH "/var/lib/cloudbreak"
    else
        env-import CB_DB_ROOT_PATH "/var/lib/boot2docker/cloudbreak"
    fi

    env-import COMMON_DB commondb
    env-import COMMON_DB_VOL common
    env-import CB_DB_ENV_USER "postgres"
    env-import CB_DB_ENV_DB "cbdb"
    env-import CB_DB_ENV_PASS ""
    env-import CB_DB_ENV_SCHEMA "public"
    env-import CB_DB_ENV_SSL "false"
    env-import CB_DB_ENV_CERT_FILE ""
    env-import CB_HBM2DDL_STRATEGY "validate"

    env-import PERISCOPE_DB_ENV_USER "postgres"
    env-import PERISCOPE_DB_ENV_DB "periscopedb"
    env-import PERISCOPE_DB_ENV_PASS ""
    env-import PERISCOPE_DB_ENV_SCHEMA "public"
    env-import PERISCOPE_DB_ENV_SSL "false"
    env-import PERISCOPE_DB_ENV_CERT_FILE ""
    env-import PERISCOPE_HBM2DDL_STRATEGY "validate"

    env-import IDENTITY_DB_URL "${COMMON_DB}.service.consul:5432"
    env-import IDENTITY_DB_NAME "uaadb"
    env-import IDENTITY_DB_USER "postgres"
    env-import IDENTITY_DB_PASS ""
}

cloudbreak-conf-cert() {
    declare desc="Declares cloudbreak cert config"
    env-import CBD_CERT_ROOT_PATH "${PWD}/certs"

    env-import CBD_TRAEFIK_TLS "/certs/traefik/client.pem,/certs/traefik/client-key.pem"
}

cloudbreak-delete-dbs() {
    declare desc="deletes all cloudbreak db (volume)"

    if [[ "$(docker-compose -p cbreak ps $COMMON_DB | tail -1)" == *"Up"* ]]; then
        error "Database container is running, delete not allowed"
        _exit 1
    fi
    docker volume rm $COMMON_DB_VOL 1>/dev/null || :
}

cloudbreak-delete-consul-data() {
    declare desc="deletes consul data-dir (volume)"

    if [[ $(docker-compose -p cbreak ps -q consul | wc -l) -eq 1 ]]; then
        error "Consul container is running, delete not allowed"
        _exit 1
    fi
    docker volume rm consul-data 1>/dev/null || :
}

cloudbreak-delete-certs() {
    declare desc="deletes all cloudbreak related certificates"
    rm -rf ${PWD}/certs
}

cloudbreak-conf-uaa() {
    env-import UAA_PORT 8089

    env-import UAA_SETTINGS_FILE uaa-changes.yml

    env-import UAA_DEFAULT_SECRET
    env-validate UAA_DEFAULT_SECRET *" "* "space"

    env-import UAA_CLOUDBREAK_ID cloudbreak
    env-import UAA_CLOUDBREAK_SECRET $UAA_DEFAULT_SECRET
    env-validate UAA_CLOUDBREAK_SECRET *" "* "space"

    env-import UAA_PERISCOPE_ID periscope
    env-import UAA_PERISCOPE_SECRET $UAA_DEFAULT_SECRET
    env-validate UAA_PERISCOPE_SECRET *" "* "space"

    env-import UAA_ULUWATU_ID uluwatu
    env-import UAA_ULUWATU_SECRET $UAA_DEFAULT_SECRET
    env-validate UAA_ULUWATU_SECRET *" "* "space"

    env-import UAA_SULTANS_ID sultans
    env-import UAA_SULTANS_SECRET $UAA_DEFAULT_SECRET
    env-validate UAA_SULTANS_SECRET *" "* "space"

    env-import UAA_CLOUDBREAK_SHELL_ID cloudbreak_shell

    env-import UAA_DEFAULT_USER_EMAIL admin@example.com
    env-import UAA_DEFAULT_USER_PW
    env-validate UAA_DEFAULT_USER_PW *" "* "space"
    env-import UAA_DEFAULT_USER_FIRSTNAME Joe
    env-import UAA_DEFAULT_USER_LASTNAME Admin
    env-import UAA_ZONE_DOMAIN example.com

    env-import UAA_DEFAULT_USER_GROUPS "openid,cloudbreak.networks,cloudbreak.securitygroups,cloudbreak.templates,cloudbreak.blueprints,cloudbreak.credentials,cloudbreak.stacks,sequenceiq.cloudbreak.admin,sequenceiq.cloudbreak.user,cloudbreak.events,cloudbreak.usages.global,cloudbreak.usages.account,cloudbreak.usages.user,periscope.cluster,cloudbreak.recipes,cloudbreak.blueprints.read,cloudbreak.templates.read,cloudbreak.credentials.read,cloudbreak.recipes.read,cloudbreak.networks.read,cloudbreak.securitygroups.read,cloudbreak.stacks.read,cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read,cloudbreak.platforms,cloudbreak.platforms.read"

    env-import UAA_FLEX_USAGE_CLIENT_ID flex_usage_client
    env-import UAA_FLEX_USAGE_CLIENT_SECRET $UAA_DEFAULT_SECRET
}

cloudbreak-conf-defaults() {
    env-import PUBLIC_IP

    if [[ ! -z "$CB_BLUEPRINT_DEFAULTS"  ]]; then
        env-import CB_BLUEPRINT_DEFAULTS
    fi;
    env-import CB_BLUEPRINT_INTERNAL ""
    if [[ ! -z "$CB_TEMPLATE_DEFAULTS" ]]; then
        env-import CB_TEMPLATE_DEFAULTS
    fi;
    if [[ ! -z "$CB_DEFAULT_GATEWAY_CIDR" ]]; then
        env-import CB_DEFAULT_GATEWAY_CIDR
    fi;
    env-import CB_AUDIT_FILE_ENABLED false
    env-import CB_KAFKA_BOOTSTRAP_SERVERS ""
    env-import CB_LOCAL_DEV_BIND_ADDR "192.168.64.1"
    env-import ADDRESS_RESOLVING_TIMEOUT 120000
    env-import CB_UI_MAX_WAIT 400
    env-import CB_HOST_DISCOVERY_CUSTOM_DOMAIN ""
    env-import CB_SMARTSENSE_CONFIGURE "false"
    env-import TRAEFIK_MAX_IDLE_CONNECTION 100
    env-import CB_AWS_DEFAULT_INBOUND_SECURITY_GROUP ""
    env-import CB_AWS_VPC ""
    env-import CB_MAX_SALT_NEW_SERVICE_RETRY 90
    env-import CB_MAX_SALT_NEW_SERVICE_RETRY_ONERROR 10
    env-import CB_MAX_SALT_RECIPE_EXECUTION_RETRY 90
    env-import CB_LOG_LEVEL "INFO"
    env-import CB_PORT 8080

    env-import CB_INSTANCE_UUID
    env-import CB_INSTANCE_NODE_ID
    env-validate CB_INSTANCE_UUID *" "* "space"

    env-import CB_SMARTSENSE_ID ""

    env-import DOCKER_STOP_TIMEOUT 60

    env-import PUBLIC_HTTP_PORT 80
    env-import PUBLIC_HTTPS_PORT 443
}

cloudbreak-conf-autscale() {
    env-import PERISCOPE_LOG_LEVEL "INFO"
}

cloudbreak-conf-cloud-provider() {
    declare desc="Defines cloud provider related parameters"

    env-import AWS_ACCESS_KEY_ID ""
    env-import AWS_SECRET_ACCESS_KEY ""
    env-import AWS_GOV_ACCESS_KEY_ID ""
    env-import AWS_GOV_SECRET_ACCESS_KEY ""
    env-import CB_AWS_DEFAULT_CF_TAG ""
    env-import CB_AWS_CUSTOM_CF_TAGS ""

    env-import CB_AWS_HOSTKEY_VERIFY "false"
    env-import CB_GCP_HOSTKEY_VERIFY "false"

    env-import CB_BYOS_DFS_DATA_DIR "/hadoop/hdfs/data"
}

cloudbreak-conf-rest-client() {
    declare desc="Defines rest client related parameters"

    env-import REST_DEBUG "false"
    env-import CERT_VALIDATION "true"
}

cloudbreak-conf-ui() {
    declare desc="Defines Uluwatu and Sultans related parameters"

    env-import ULU_HOST_ADDRESS  "https://$PUBLIC_IP:$PUBLIC_HTTPS_PORT"
    env-import ULU_OAUTH_REDIRECT_URI  "$ULU_HOST_ADDRESS/authorize"
    env-import ULU_SULTANS_ADDRESS  "https://$PUBLIC_IP:$PUBLIC_HTTPS_PORT/sl"
    env-import CB_HOST_ADDRESS  "http://$PUBLIC_IP"
    env-import ULU_HWX_CLOUD_DEFAULT_CREDENTIAL ""
    env-import HWX_HCC_AVAILABLE "false"
    env-import ULU_HWX_CLOUD_DEFAULT_SSH_KEY ""
    env-import ULU_DEFAULT_SSH_KEY ""
    env-import ULU_HWX_CLOUD_DEFAULT_REGION ""
    env-import ULU_HWX_CLOUD_DEFAULT_VPC_ID ""
    env-import ULU_HWX_CLOUD_DEFAULT_IGW_ID ""
    env-import ULU_HWX_CLOUD_DEFAULT_SUBNET_ID ""
    env-import ULU_HWX_CLOUD_REGISTRATION_URL ""
    env-import HWX_DOC_LINK ""
    env-import ULU_NODE_TLS_REJECT_UNAUTHORIZED "0"
    env-import SL_NODE_TLS_REJECT_UNAUTHORIZED "0"
    env-import ULU_SUBSCRIBE_TO_NOTIFICATIONS "false"
    env-import HWX_CLOUD_ENABLE_GOVERNANCE_AND_SECURITY "false"
}

cloudbreak-conf-java() {
    env-import CB_JAVA_OPTS ""
}

cloudbreak-conf-proxy() {
    env-import HTTP_PROXY_HOST ""
    env-import HTTPS_PROXY_HOST ""
    env-import PROXY_PORT ""
    env-import PROXY_USER ""
    env-import PROXY_PASSWORD ""
    env-import NON_PROXY_HOSTS "*.consul"
    env-import HTTPS_PROXYFORCLUSTERCONNECTION "false"
}

escape-string-env() {
    declare desc="Escape yaml string by delimiter type"
    : ${2:=required}
    local in=$1
    local delimiter=$2

    if [[ $delimiter == "'" ]]; then
        out=`echo $in | sed -e "s/'/'\\\\\\''/g"`
    elif [[ $delimiter == '"' ]]; then
        out=`echo $in | sed -e 's/\\\\/\\\\\\\/g' -e 's/"/\\\"/g' -e 's/[$]/\$/g' -e "s/\\\`/\\\\\\\\\\\\\\\`/g" -e 's/!/\\\\!/g'`
    else
        out="$in"
    fi

    echo $out
}

gen-password() {
    date +%s | checksum sha1 | head -c 10
}

cloudbreak-generate-cert() {
    cloudbreak-config
    if [ -f "${CBD_CERT_ROOT_PATH}/traefik/client.pem" ] && [ -f "${CBD_CERT_ROOT_PATH}/traefik/client-key.pem" ]; then
      debug "Cloudbreak certificate and private key already exist, won't generate new ones."
    else
      info "Generating Cloudbreak client certificate and private key in ${CBD_CERT_ROOT_PATH} with ${PUBLIC_IP} into ${CBD_CERT_ROOT_PATH}/traefik."
      mkdir -p "${CBD_CERT_ROOT_PATH}/traefik"
      if is_linux; then
        run_as_user="-u $(id -u $(whoami)):$(id -g $(whoami))"
      fi

      cbd_ca_cert_gen_out=$(mktemp)
      docker run \
          --label cbreak.sidekick=true \
          $run_as_user \
          -v ${CBD_CERT_ROOT_PATH}:/certs \
          ehazlett/certm:${DOCKER_TAG_CERT_TOOL} \
          -d /certs/traefik ca generate -o=local &> $cbd_ca_cert_gen_out || CA_CERT_EXIT_CODE=$? && true;
      if [[ $CA_CERT_EXIT_CODE -ne 0 ]]; then
          cat $cbd_ca_cert_gen_out;
          exit 1;
      fi

      cbd_client_cert_gen_out=$(mktemp)
      docker run \
          --label cbreak.sidekick=true \
          $run_as_user \
          -v ${CBD_CERT_ROOT_PATH}:/certs \
          ehazlett/certm:${DOCKER_TAG_CERT_TOOL} \
          -d /certs/traefik client generate --common-name=${PUBLIC_IP} -o=local &> $cbd_client_cert_gen_out || CLIENT_CERT_EXIT_CODE=$? && true;
      if [[ $CLIENT_CERT_EXIT_CODE -ne 0 ]]; then
         cat $cbd_client_cert_gen_out;
         exit 1;
      fi

      owner=$(ls -od ${CBD_CERT_ROOT_PATH} | tr -s ' ' | cut -d ' ' -f 3)
      [[ "$owner" != "$(whoami)" ]] && sudo chown -R $(whoami):$(id -gn) ${CBD_CERT_ROOT_PATH}
      mv "${CBD_CERT_ROOT_PATH}/traefik/cert.pem" "${CBD_CERT_ROOT_PATH}/traefik/client.pem"
      cat "${CBD_CERT_ROOT_PATH}/traefik/ca.pem" >> "${CBD_CERT_ROOT_PATH}/traefik/client.pem"
      mv "${CBD_CERT_ROOT_PATH}/traefik/key.pem" "${CBD_CERT_ROOT_PATH}/traefik/client-key.pem"
      mv "${CBD_CERT_ROOT_PATH}/traefik/ca.pem" "${CBD_CERT_ROOT_PATH}/traefik/client-ca.pem"
      mv "${CBD_CERT_ROOT_PATH}/traefik/ca-key.pem" "${CBD_CERT_ROOT_PATH}/traefik/client-ca-key.pem"
      debug "Certificates successfully generated."
    fi
}

generate_uaa_check_diff() {
    local verbose="$1"

    if [ -f uaa.yml ]; then
        local uaa_delme_path=$TEMP_DIR/uaa-delme.yml
        generate_uaa_config_force $uaa_delme_path
        if diff $uaa_delme_path uaa.yml &> /dev/null; then
            debug "uaa.yml exists and generate wouldn't change it"
            return 0
        else
            if ! [[ "$regeneteInProgress" ]]; then
                warn "uaa.yml already exists, BUT generate would create a DIFFERENT one!"
                warn "please regenerate it:"
                echo "  cbd regenerate" | blue
            fi

            if [[ "$verbose" ]]; then
                warn "expected change:"
                diff $uaa_delme_path uaa.yml || true
            else
                debug "expected change:"
                (diff $uaa_delme_path uaa.yml || true) | debug-cat
            fi
            return 1
        fi
    else
        generate_uaa_config_force uaa.yml
    fi
    return 0

}

generate_uaa_config() {
    cloudbreak-config

    if ! generate_uaa_check_diff; then
        if [[ "$CBD_FORCE_START" ]]; then
            warn "You have forced to start ..."
        else
            warn "Please check the expected config changes with:"
            echo "  cbd doctor" | blue
            debug "If you want to ignore the changes, set the CBD_FORCE_START to true in Profile"
            _exit 1
        fi
    else
        info "generating uaa.yml"
        if [ -f "$UAA_SETTINGS_FILE" ]; then
            info "apply custom uaa settings from file: $UAA_SETTINGS_FILE"
        fi
        generate_uaa_config_force uaa.yml
    fi
}

generate_uaa_config_force() {
    declare uaaFile=${1:? required: uaa config file path}

    debug "Generating Identity server config: ${uaaFile} ..."

    cat > ${uaaFile} << EOF
spring_profiles: postgresql

database:
  driverClassName: org.postgresql.Driver
  url: jdbc:postgresql://\${IDENTITY_DB_URL}/\${IDENTITY_DB_NAME}
  username: \${IDENTITY_DB_USER}
  password: \${IDENTITY_DB_PASS}
  maxactive: 200

zones:
 internal:
   hostnames:
     - ${PRIVATE_IP}
     - ${PUBLIC_IP}
     - node1.node.dc1.consul
     - identity.service.consul
     - ${UAA_ZONE_DOMAIN}

oauth:
  client:
    override: true
    autoapprove:
      - ${UAA_CLOUDBREAK_SHELL_ID}
  clients:
    ${UAA_SULTANS_ID}:
      id: ${UAA_SULTANS_ID}
      secret: '$(escape-string-yaml $UAA_SULTANS_SECRET \')'
      authorized-grant-types: client_credentials
      scope: scim.read,scim.write,password.write
      authorities: uaa.resource,scim.read,scim.write,password.write
    ${UAA_ULUWATU_ID}:
      id: ${UAA_ULUWATU_ID}
      secret: '$(escape-string-yaml $UAA_ULUWATU_SECRET \')'
      authorized-grant-types: authorization_code,client_credentials
      scope: cloudbreak.blueprints,cloudbreak.credentials,cloudbreak.stacks,cloudbreak.templates,cloudbreak.networks,cloudbreak.securitygroups,openid,cloudbreak.usages.global,cloudbreak.usages.account,cloudbreak.usages.user,cloudbreak.events,periscope.cluster,cloudbreak.recipes,cloudbreak.blueprints.read,cloudbreak.templates.read,cloudbreak.credentials.read,cloudbreak.recipes.read,cloudbreak.networks.read,cloudbreak.securitygroups.read,cloudbreak.stacks.read,cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read,cloudbreak.platforms,cloudbreak.platforms.read
      authorities: cloudbreak.subscribe
      redirect-uri: ${ULU_OAUTH_REDIRECT_URI}
    ${UAA_CLOUDBREAK_ID}:
      id: ${UAA_CLOUDBREAK_ID}
      secret: '$(escape-string-yaml $UAA_CLOUDBREAK_SECRET \')'
      authorized-grant-types: client_credentials
      scope: scim.read
      authorities: uaa.resource,scim.read
    ${UAA_PERISCOPE_ID}:
      id: ${UAA_PERISCOPE_ID}
      secret: '$(escape-string-yaml $UAA_PERISCOPE_SECRET \')'
      authorized-grant-types: client_credentials
      scope: none
      authorities: cloudbreak.autoscale,uaa.resource,scim.read
    ${UAA_CLOUDBREAK_SHELL_ID}:
      id: ${UAA_CLOUDBREAK_SHELL_ID}
      authorized-grant-types: implicit
      scope: cloudbreak.networks,cloudbreak.securitygroups,cloudbreak.templates,cloudbreak.blueprints,cloudbreak.credentials,cloudbreak.stacks,cloudbreak.events,cloudbreak.usages.global,cloudbreak.usages.account,cloudbreak.usages.user,cloudbreak.recipes,openid,cloudbreak.blueprints.read,cloudbreak.templates.read,cloudbreak.credentials.read,cloudbreak.recipes.read,cloudbreak.networks.read,cloudbreak.securitygroups.read,cloudbreak.stacks.read,cloudbreak.sssdconfigs,cloudbreak.sssdconfigs.read,cloudbreak.platforms,cloudbreak.platforms.read,periscope.cluster
      authorities: uaa.none
      redirect-uri: http://cloudbreak.shell
    ${UAA_FLEX_USAGE_CLIENT_ID}:
      id: ${UAA_FLEX_USAGE_CLIENT_ID}
      secret: '$(escape-string-yaml $UAA_FLEX_USAGE_CLIENT_SECRET \')'
      authorized-grant-types: client_credentials
      scope: none
      authorities: cloudbreak.flex

scim:
  username_pattern: '[a-z0-9+\-_.@]+'
  groups:
EOF
    for group in ${UAA_DEFAULT_USER_GROUPS//,/ }; do
        echo "    $group: Default group" >> ${uaaFile}
    done
    if [[ "$UAA_DEFAULT_USER_PW" ]]; then
        cat >> ${uaaFile} << EOF
  users:
    - ${UAA_DEFAULT_USER_EMAIL}|${UAA_DEFAULT_USER_PW}|${UAA_DEFAULT_USER_EMAIL}|${UAA_DEFAULT_USER_FIRSTNAME}|${UAA_DEFAULT_USER_LASTNAME}|${UAA_DEFAULT_USER_GROUPS}
EOF
    fi

    if [ -f "$UAA_SETTINGS_FILE" ]; then
        yq m -i -x ${uaaFile} ${UAA_SETTINGS_FILE}
    fi
}

escape-string-json() {
    declare desc="Escape json string"
    : ${1:=required}
    local in=$1

    out=`echo $in | sed -e 's/\\\\/\\\\\\\/g' -e 's/"/\\\"/g'`

    echo $out
}

util-add-default-user() {
    declare desc="Add default admin Cloudbreak user"
    debug $desc

    cloudbreak-config

    local passwd="$UAA_DEFAULT_USER_PW"
    if ! [[ "$passwd" ]]; then
        read -s -t 10 -p "password:" passwd
        echo
    fi

    local container=$(docker ps | grep cbreak_identity_ | cut -d" " -f 1)
    if ! [[ "$container" ]];then
        echo "[ERROR] Cloudbreak isn't running, please start it before adding new user" | red 1>&2
        _exit 1
    fi

    local address="http://localhost:8080"
    local bearer=$(docker exec -i $container curl -s "$address/oauth/token?grant_type=client_credentials&token_format=opaque" -u "${UAA_SULTANS_ID}:${UAA_SULTANS_SECRET}" | jq -r .access_token 2>/dev/null)
    local json='{"userName":"'${UAA_DEFAULT_USER_EMAIL}'","name":{"familyName":"'${UAA_DEFAULT_USER_LASTNAME}'","givenName":"'${UAA_DEFAULT_USER_FIRSTNAME}'"},"emails":[{"value":"'${UAA_DEFAULT_USER_EMAIL}'","primary":true}],"password":"'$(escape-string-json $passwd)'","active":true,"verified":true,"schemas":["urn:scim:schemas:core:1.0"]}'
    local user=$(docker exec -i $container  curl -s $address/Users -X POST -H 'Accept: application/json' -H "Authorization: Bearer $bearer" -H 'Content-Type: application/json' -d ''$json'' 2>/dev/null)
    local existing_id=$(echo $user | jq -r .user_id 2> /dev/null)
    if [[ "$existing_id" != "null" ]]; then
        info "User already exists, nothing to do."
        return
    fi
    local new_id=$(echo $user | jq -r .id 2> /dev/null)
    if [[ "$new_id" != "null" ]]; then
        local groups=$(docker exec -i $container  curl -s "$address/Groups" -H "Authorization: Bearer $bearer" | jq ".resources[] | {id, displayName}")
        for group in ${UAA_DEFAULT_USER_GROUPS//,/ }; do
            debug "Adding user to group $group"
            group_id=$(echo $groups | jq . | grep -B2 -A1 '"'$group'"' | jq -r .id)
            docker exec -i $container curl -s "$address/Groups/$group_id/members" -X POST -H "Authorization: Bearer $bearer" -H 'Content-Type: application/json' -d '{"origin":"uaa","type":"USER","value":"'$new_id'"}' &>/dev/null
        done
        info "Default user created."
    else
        echo "[ERROR] ${user}" | red 1>&2
        _exit 1
    fi
}

util-generate-ldap-mapping() {
    declare desc="Generates an SQL script to map LDAP/AD groups to Cloudbreak defined OAuth2 scopes. Useful if you want to make changes in the mapping."
    debug $desc

    local mapping_file="mapping.sql"
    generate-ldap-mapping "$1" "$mapping_file"
    info "Group mapping file has been created: $mapping_file"
    info "To apply the $mapping_file please run the following command: docker exec cbreak_commondb_1 psql -U postgres -d uaadb -c \"\$(cat $mapping_file)\""
    info "To clean up the group mapping please run the following command: docker exec cbreak_commondb_1 psql -U postgres -d uaadb -c \"delete from external_group_mapping\""
    info "Note: you must log out and log back in with your LDAP/AD users after the mapping change"
}

util-execute-ldap-mapping() {
    declare desc="Generates and automatically applies the changes in identity to map LDAP/AD groups to Cloudbreak defined OAuth2 scopes"
    debug $desc

    exitOnRemoteDatabase uaadb

    local mapping_file="$TEMP_DIR/mapping-delme.yml"
    generate-ldap-mapping "$1" "$mapping_file"
    info "Applying LDAP/AD mapping"
    docker exec cbreak_commondb_1 psql -U postgres -d uaadb -c "$(cat $mapping_file)"
    rm -f "$mapping_file"
    info "Successfully applied LDAP/AD mapping"
    info "Note: you must log out and log back in with your LDAP/AD users after the mapping change"
}

util-delete-ldap-mapping() {
    declare desc="Removes all the LDAP/AD group mappings to OAuth2 scopes"
    debug $desc

    exitOnRemoteDatabase uaadb

    local container=$(docker ps | grep cbreak_commondb_ | cut -d" " -f 1)
    if ! [[ "$container" ]]; then
        error "Cloudbreak isn't running, please start it"
        _exit 1
    fi

    info "Remove LDAP/AD mappings"
    docker exec cbreak_commondb_1 psql -U postgres -d uaadb -c "delete from external_group_mapping"
    info "Successfully removed LDAP/AD mappings"
    info "Note: you must log out and log back in with your LDAP/AD users after the mapping change"
}

generate-ldap-mapping() {
    exitOnRemoteDatabase uaadb

    if [[ -z "$1" ]]; then
        error "LDAP/AD group DN parameter must be provided (e.g: CN=cloudbreak,CN=Users,DC=ad,DC=mycompany,DC=com)"
        _exit 1
    fi
    local group="$1"
    local mapping_file=${2:-mapping.sql}

    local container=$(docker ps | grep cbreak_commondb_ | cut -d" " -f 1)
    if ! [[ "$container" ]]; then
        error "Cloudbreak isn't running, please start it"
        _exit 1
    fi

    local scopes=$(docker exec $container psql -U postgres -d uaadb -c "select displayname from groups where displayname like 'cloudbreak%' or displayname like 'periscope%' or displayname='sequenceiq.cloudbreak.user';" | tail -n +3 | grep -v rows)
    rm -f ${mapping_file}
    for scope in ${scopes}; do
    local line="INSERT INTO external_group_mapping (group_id, external_group, added, origin) VALUES ((select id from groups where displayname='$scope'), '$group', '2016-09-30 19:28:24.255', 'ldap');"
    echo $line >> ${mapping_file}
    done
}

exitOnRemoteDatabase() {
    case $1 in
    cbdb*)
        db="$CB_DB_PORT_5432_TCP_ADDR"
    ;;
    periscopedb*)
        db="$PERISCOPE_DB_PORT_5432_TCP_ADDR"
    ;;
    uaadb*)
        db="$IDENTITY_DB_URL"
    ;;
    *)
        error "Database not supported $1"
        _exit 235
    ;;
    esac

    if [[ -n "$db" ]] && [[ $db != "$COMMON_DB.service.consul"* ]]; then
        error "Remote database not supported as $1"
        _exit 543
    fi
}

util-token() {
    declare desc="Generates an OAuth token with CloudbreakShell scopes"

    cloudbreak-config

    local passwd="$UAA_DEFAULT_USER_PW"
    if ! [[ "$passwd" ]]; then
        read -s -t 10 -p "password:" passwd
        echo
    fi

    local TOKEN=$(curl -sX POST \
        -w '%{redirect_url}' \
        -H "accept: application/x-www-form-urlencoded" \
        --data-urlencode credentials='{"username":"'${UAA_DEFAULT_USER_EMAIL}'","password":"'$(escape-string-json $passwd)'"}' \
        "${PUBLIC_IP}:${UAA_PORT}/oauth/authorize?response_type=token&client_id=cloudbreak_shell&scope.0=openid&source=login&redirect_uri=http://cloudbreak.shell" \
           | cut -d'&' -f 2)
    echo ${TOKEN#*=}
}

util-token-debug() {
    declare desc="Opens the browse jwt.io to inspect a newly generated Oauth token."

    local token="$(util-token)"
    open "http://jwt.io/?value=$token"
}

util-local-dev() {
    declare desc="Stops cloudbreak and periscope container, and starts an ambassador for cbreak and periscope in IntelliJ (def cb: port:9090, peri: port: 8085)"
    declare port=${1:-9091}

    cloudbreak-config

    if [ "$CB_SCHEMA_SCRIPTS_LOCATION" = "container" ]; then
      warn "CB_SCHEMA_SCRIPTS_LOCATION environment variable must be set and points to the cloudbreak project's schema location"
      _exit 127
    fi

    debug stopping original cloudbreak container
    dockerCompose stop --timeout ${DOCKER_STOP_TIMEOUT} cloudbreak
    dockerCompose stop --timeout ${DOCKER_STOP_TIMEOUT} periscope

    docker rm -f cloudbreak-proxy 2> /dev/null || :
    docker rm -f periscope-proxy 2> /dev/null || :

    debug starting an ambassador to be registered as cloudbreak.service.consul.
    debug "all traffic to ambassador will be proxied to localhost"

    docker run -d \
        --name cloudbreak-proxy \
        -p 8080:8080 \
        -e PORT=8080 \
        -e SERVICE_NAME=cloudbreak \
        -e SERVICE_8080_NAME=cloudbreak \
        -l traefik.port=8080 \
        -l traefik.frontend.rule=PathPrefix:/cb/ \
        -l traefik.backend=cloudbreak-backend \
        -l traefik.frontend.priority=10 \
        hortonworks/ambassadord:$DOCKER_TAG_AMBASSADOR $CB_LOCAL_DEV_BIND_ADDR:$port

    docker run -d \
        --name periscope-proxy \
        -p 8085:8085 \
        -e PORT=8085 \
        -e SERVICE_NAME=periscope \
        -e SERVICE_8085_NAME=periscope \
        -l traefik.port=8085 \
        -l traefik.frontend.rule=PathPrefix:/as/ \
        -l traefik.backend=periscope-backend \
        -l traefik.frontend.priority=10 \
        hortonworks/ambassadord:$DOCKER_TAG_AMBASSADOR $CB_LOCAL_DEV_BIND_ADDR:8085

    create-migrate-log
    migrate-one-db cbdb up
    migrate-one-db periscopedb up
}

util-get-usage() {
    declare desc="Generate Flex related usages."

    cloudbreak-conf-uaa

    local USAGE_PATH="usages/flex/daily"

    if [ $# -eq 1 ]; then
        local PARAMETER=$1
        case $PARAMETER in
            latest)
                USAGE_PATH="usages/flex/latest"
                ;;
            *)
                error "Invalid parameter: $PARAMETER. Supported parameters: latest"
                return 1
                ;;
        esac
    fi

    local CRED="$UAA_FLEX_USAGE_CLIENT_ID:$UAA_FLEX_USAGE_CLIENT_SECRET"
    local CRED_BASE64="$(echo -n "${CRED}"|base64|tr -d '\n')"
    local TOKEN=$(curl -sX POST -H "Authorization: Basic $CRED_BASE64" "${PUBLIC_IP}:${UAA_PORT}/oauth/token?grant_type=client_credentials" | jq '.access_token' -r)
    local USAGE=$(curl -sX GET -H "Authorization: Bearer $TOKEN" "${PUBLIC_IP}:8080/cb/api/v1/${USAGE_PATH}")
    echo ${USAGE#*=}
}
