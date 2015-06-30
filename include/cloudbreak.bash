
cloudbreak-config() {
  env-import PRIVATE_IP $(docker run --rm alpine sh -c 'ip ro | grep default | cut -d" " -f 3')
  cloudbreak-conf-tags
  cloudbreak-conf-images
  cloudbreak-conf-cert
  cloudbreak-conf-db
  cloudbreak-conf-defaults
  cloudbreak-conf-uaa
  cloudbreak-conf-smtp
  cloudbreak-conf-cloud-provider
  cloudbreak-conf-ui
  cloudbreak-conf-java
  cloudbreak-conf-baywatch
}

cloudbreak-conf-tags() {
    declare desc="Defines docker image tags"

    env-import DOCKER_TAG_ALPINE 3.1
    env-import DOCKER_TAG_CONSUL v0.5.0-v3
    env-import DOCKER_TAG_REGISTRATOR v5
    env-import DOCKER_TAG_POSTGRES 9.4.1
    env-import DOCKER_TAG_CLOUDBREAK 0.5.105
    env-import DOCKER_TAG_CBDB 0.5.103
    env-import DOCKER_TAG_PERISCOPE 0.5.5
    env-import DOCKER_TAG_PCDB 0.5.5
    env-import DOCKER_TAG_UAA 1.8.1-v2
    env-import DOCKER_TAG_ULUWATU 0.5.33
    env-import DOCKER_TAG_SULTANS 0.5.4
    env-import DOCKER_TAG_AMBASSADOR 0.5.0
    env-import DOCKER_TAG_CERT_TOOL 0.0.3
    env-import DOCKER_TAG_CLOUDBREAK_SHELL 0.4.11

    env-import CB_DOCKER_CONTAINER_AMBARI ""
    env-import CB_DOCKER_CONTAINER_AMBARI_WARM ""
}

cloudbreak-conf-images() {
    declare desc="Defines base images for each provider"

    env-import CB_AZURE_IMAGE_URI ""
    env-import CB_AWS_AMI_MAP ""
    env-import CB_OPENSTACK_IMAGE ""
    env-import CB_GCP_SOURCE_IMAGE_PATH ""

}

cloudbreak-conf-smtp() {
    env-import CLOUDBREAK_SMTP_SENDER_USERNAME " "
    env-import CLOUDBREAK_SMTP_SENDER_PASSWORD " "
    env-import CLOUDBREAK_SMTP_SENDER_HOST " "
    env-import CLOUDBREAK_SMTP_SENDER_PORT 25
    env-import CLOUDBREAK_SMTP_SENDER_FROM " "
}

cloudbreak-conf-db() {
    declare desc="Declares cloudbreak DB config"

    if boot2docker version &> /dev/null; then
        # this is for OSX
        env-import CB_DB_ROOT_PATH "/var/lib/boot2docker/cloudbreak"
    else
        # this is for linux
        env-import CB_DB_ROOT_PATH "/var/lib/cloudbreak"
    fi

    env-import CB_DB_ENV_USER "postgres"
    env-import CB_DB_ENV_DB "cloudbreak"
    env-import CB_DB_ENV_PASS ""
    env-import CB_HBM2DDL_STRATEGY "validate"
    env-import PERISCOPE_DB_HBM2DDL_STRATEGY "validate"
}

cloudbreak-conf-cert() {
    declare desc="Declares cloudbreak cert config"
    env-import CBD_CERT_ROOT_PATH "${PWD}/certs"
}

cloudbreak-delete-dbs() {
    declare desc="deletes all cloudbreak related dbs: cbdb,pcdb,uaadb"

    if boot2docker version &> /dev/null; then
        # this is for OSX
        boot2docker ssh 'sudo rm -rf /var/lib/boot2docker/cloudbreak/*'
    else
        # this is for linux
        rm -rf /var/lib/cloudbreak/*
    fi
}

cloudbreak-delete-certs() {
    declare desc="deletes all cloudbreak related certificates"
    rm -rf ${PWD}/certs
}

cloudbreak-conf-uaa() {

    env-import UAA_DEFAULT_SECRET "cbsecret2015"

    env-import UAA_CLOUDBREAK_ID cloudbreak
    env-import UAA_CLOUDBREAK_SECRET $UAA_DEFAULT_SECRET

    env-import UAA_PERISCOPE_ID periscope
    env-import UAA_PERISCOPE_SECRET $UAA_DEFAULT_SECRET

    env-import UAA_ULUWATU_ID uluwatu
    env-import UAA_ULUWATU_SECRET $UAA_DEFAULT_SECRET

    env-import UAA_SULTANS_ID sultans
    env-import UAA_SULTANS_SECRET $UAA_DEFAULT_SECRET

    env-import UAA_CLOUDBREAK_SHELL_ID cloudbreak_shell

    env-import UAA_DEFAULT_USER_EMAIL admin@example.com
    env-import UAA_DEFAULT_USER_PW cloudbreak
    env-import UAA_DEFAULT_USER_FIRSTNAME Joe
    env-import UAA_DEFAULT_USER_LASTNAME Admin
}

cloudbreak-conf-defaults() {
    env-import PRIVATE_IP
    env-import PUBLIC_IP

    env-import CB_HOST_ADDR $PUBLIC_IP
    env-import CB_BLUEPRINT_DEFAULTS "hdp-small-default,hdp-spark-cluster,hdp-streaming-cluster"
    env-import CB_TEMPLATE_DEFAULTS "minviable-gcp,minviable-azure,minviable-aws"
}

cloudbreak-conf-cloud-provider() {
    declare desc="Defines cloud provider related parameters"

    env-import AWS_ACCESS_KEY_ID ""
    env-import AWS_SECRET_ACCESS_KEY ""

}

cloudbreak-conf-ui() {
    declare desc="Defines Uluwatu and Sultans related parameters"

    env-import ULU_HOST_ADDRESS  "http://$PUBLIC_IP:3000"
    env-import ULU_OAUTH_REDIRECT_URI  "$ULU_HOST_ADDRESS/authorize"
    env-import ULU_SULTANS_ADDRESS  "http://$PUBLIC_IP:3001"

}

cloudbreak-conf-java() {
    env-import SECURE_RANDOM "false"
}

cloudbreak-conf-baywatch() {
  declare desc="Defines Baywatch related parameters"

  env-import CB_BAYWATCH_ENABLED "false"
  env-import CB_BAYWATCH_EXTERN_LOCATION ""
}

util-cloudbreak-shell() {
    declare desc="Starts an interactive CloudbreakShell"

    _cloudbreak-shell -it
}

util-cloudbreak-shell-quiet() {
    declare desc="Starts a non-interactive CloudbreakShell, commands from stdin."
    _cloudbreak-shell -i
}

_cloudbreak-shell() {

    cloudbreak-config
    
    docker run "$@" \
        --rm \
        --link cbreak_ambassador_1:backend \
        -e BACKEND_9000=cloudbreak.service.consul \
        -e BACKEND_9001=identity.service.consul \
        -e CLOUDBREAK_ADDRESS=http://backend:9000 \
        -e IDENTITY_ADDRESS=http://backend:9001 \
        -e SEQUENCEIQ_USER=admin@example.com \
        -e SEQUENCEIQ_PASSWORD=cloudbreak \
        -v $PWD:/data \
        sequenceiq/cb-shell:$DOCKER_TAG_CLOUDBREAK_SHELL
}

gen-password() {
    date +%s | checksum sha1 | head -c 10
}

cloudbreak-generate-cert() {
    cloudbreak-config
    if [ -f "${CBD_CERT_ROOT_PATH}/client.pem" ] && [ -f "${CBD_CERT_ROOT_PATH}/client-key.pem" ]; then
      debug "Cloudbreak certificate and private key already exist, won't generate new ones."
    else
      info "Generating Cloudbreak client certificate and private key in ${CBD_CERT_ROOT_PATH}."
      docker run --rm -v ${CBD_CERT_ROOT_PATH}:/certs ehazlett/cert-tool:${DOCKER_TAG_CERT_TOOL} -d /certs -o=local &> /dev/null
      cat "${CBD_CERT_ROOT_PATH}/ca.pem" >> "${CBD_CERT_ROOT_PATH}/client.pem"
      mv "${CBD_CERT_ROOT_PATH}/ca.pem" "${CBD_CERT_ROOT_PATH}/client-ca.pem"
      mv "${CBD_CERT_ROOT_PATH}/ca-key.pem" "${CBD_CERT_ROOT_PATH}/client-ca-key.pem"
      debug "Certificates successfully generated."
    fi
}

generate_uaa_config() {
    cloudbreak-config

    if [ -f uaa.yml ]; then

        generate_uaa_config_force /tmp/uaa-delme.yml
        if diff /tmp/uaa-delme.yml uaa.yml &> /dev/null; then
            debug "uaa.yml exists and generate wouldn't change it"
        else
            warn "uaa.yml already exists, BUT generate would create a MODIFIED one."
            warn "if you want to regenerate, remove it first:"
            echo "  cbd regenerate" | blue
            warn "expected change:"

            (diff /tmp/uaa-delme.yml uaa.yml || true) | cyan
        fi
    else
        info "generating uaa.yml"
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
      redirect-uri: ${ULU_OAUTH_REDIRECT_URI}
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
}

util-token() {
    declare desc="Generates an OAuth token with CloudbreakShell scopes"

    cloudbreak-config
    local TOKEN=$(curl -siX POST \
        -H "accept: application/x-www-form-urlencoded" \
        -d credentials='{"username":"'${UAA_DEFAULT_USER_EMAIL}'","password":"'${UAA_DEFAULT_USER_PW}'"}' \
        "$(boot2docker ip):8089/oauth/authorize?response_type=token&client_id=cloudbreak_shell&scope.0=openid&source=login&redirect_uri=http://cloudbreak.shell" \
           | grep Location | cut -d'=' -f 2 | cut -d'&' -f 1)
    debug TOKEN=$TOKEN
}

util-local-dev() {
    declare desc="Stops cloudbreak container, and starts an ambassador for cbreak in IntelliJ (def port:9090)"
    declare port=${1:-9091}

    debug stopping original cloudbreak container
    dockerCompose stop cloudbreak

    debug starting an ambassador to be registered as cloudbreak.service.consul.
    debug "all traffic to ambassador will be proxied to localhost (192.168.59.3):$port"

: << HINT
sed -i  "s/cb.db.port.5432.tcp.addr=[^ ]*/cb.db.port.5432.tcp.addr=$(docker inspect -f '{{.NetworkSettings.IPAddress}}' cbreak_cbdb_1)/" \
    ~/prj/cloudbreak.idea/runConfigurations/cloudbreak_remote.xml 
HINT

    docker run -d \
        --name cloudbreak-proxy \
        -p 8080:8080 \
        -e PORT=8080 \
        -e SERVICE_NAME=cloudbreak \
        sequenceiq/ambassadord 192.168.59.3:$port

}
