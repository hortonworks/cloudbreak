
cloudbreak-config() {
  env-import PRIVATE_IP $(docker run --rm alpine sh -c 'ip ro | grep default | cut -d" " -f 3')
  cloudbreak-conf-tags
  cloudbreak-conf-images
  cloudbreak-conf-db
  cloudbreak-conf-defaults
  cloudbreak-conf-uaa
  cloudbreak-conf-smtp
  cloudbreak-conf-cloud-provider
  cloudbreak-conf-ui
}

cloudbreak-conf-tags() {
    declare desc="Defines docker image tags"

    env-import DOCKER_TAG_ALPINE 3.1
    env-import DOCKER_TAG_CONSUL v0.5.0-v3
    env-import DOCKER_TAG_REGISTRATOR v5
    env-import DOCKER_TAG_POSTGRES 9.4.1
    env-import DOCKER_TAG_CLOUDBREAK 0.5.34
    env-import DOCKER_TAG_CBDB 0.5.34
    env-import DOCKER_TAG_PERISCOPE 0.5.3
    env-import DOCKER_TAG_PCDB 0.5.2
    env-import DOCKER_TAG_UAA 1.8.1-v1
    env-import DOCKER_TAG_CBSHELL 0.2.47
    env-import DOCKER_TAG_ULUWATU 0.5.10
    env-import DOCKER_TAG_SULTANS 0.5.1
    env-import DOCKER_TAG_AMBASSADOR latest
}

cloudbreak-conf-images() {
    declare desc="Defines base images for each provider"

    env-import CB_AZURE_IMAGE_URI "https://102589fae040d8westeurope.blob.core.windows.net/images/packer-cloudbreak-2015-05-07-centos6-mun10_2015-May-7_16-3-os-2015-05-07.vhd"
    env-import CB_AWS_AMI_MAP "ap-northeast-1:ami-4ee7284e,ap-southeast-1:ami-b0e5dae2,ap-southeast-2:ami-1d780727,eu-west-1:ami-33593144,sa-east-1:ami-eba423f6,us-east-1:ami-606c7808,us-west-1:ami-9d709fd9,us-west-2:ami-c37848f3"
    env-import CB_OPENSTACK_IMAGE "cb-centos66-amb200-2015-05-08"
    env-import CB_GCP_SOURCE_IMAGE_PATH "sequenceiqimage/cb-centos66-amb200-2015-05-07-1639.image.tar.gz"

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
    env-import CB_BLUEPRINT_DEFAULTS "multi-node-hdfs-yarn,hdp-multinode-default"
}

cloudbreak-conf-cloud-provider() {
    declare desc="Defines cloud provider related parameters"

    env-import AWS_ACCESS_KEY_ID ""
    env-import AWS_SECRET_KEY ""

}

cloudbreak-conf-ui() {
    declare desc="Defines Uluwatu and Sultans related parameters"

    env-import ULU_HOST_ADDRESS  "http://$PUBLIC_IP:3000"
    env-import ULU_OAUTH_REDIRECT_URI  "$ULU_HOST_ADDRESS/authorize"
    env-import ULU_SULTANS_ADDRESS  "http://$PUBLIC_IP:3001"

}


gen-password() {
    date +%s | checksum sha1 | head -c 10
}

generate_uaa_config() {
    cloudbreak-config

    if [ -f uaa.yml ]; then

        generate_uaa_config_force /tmp/uaa-delme.yml
        if diff /tmp/uaa-delme.yml uaa.yml &> /dev/null; then
            debug "uaa.yml exists and generate wouldn't change it"
        else
            warn "uaa.yml already exists, BUT generate would create a MODIFIED one."
            warn "if you want to regenerate, move it away ..., expected change:"
            (diff /tmp/uaa-delme.yml uaa.yml || true) | cyan
        fi
    else
        generate_uaa_config_force uaa.yml
    fi
}


generate_uaa_config_force() {
    declare uaaFile=${1:? required: uaa config file path}

    info "Generating Identity server config: ${uaaFile} ..."

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

token() {
    cloudbreak-init
    local TOKEN=$(curl -siX POST \
        -H "accept: application/x-www-form-urlencoded" \
        -d credentials='{"username":"'${UAA_DEFAULT_USER_EMAIL}'","password":"'${UAA_DEFAULT_USER_PW}'"}' \
        "$(dhp identity)/oauth/authorize?response_type=token&client_id=cloudbreak_shell&scope.0=openid&source=login&redirect_uri=http://cloudbreak.shell" \
           | grep Location | cut -d'=' -f 2 | cut -d'&' -f 1)
    debug TOKEN=$TOKEN
}
