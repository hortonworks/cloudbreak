
migrate-config() {
    declare desc="Defines env variables for migration"

    env-import DOCKER_TAG_MIGRATION latest
    env-import CB_SCHEMA_SCRIPTS_LOCATION "container"
    env-import PERISCOPE_SCHEMA_SCRIPTS_LOCATION "container"
}

migrate-startdb() {
    declare desc="Starts the DB containers"

    deployer-generate
    compose-up --no-recreate cbdb pcdb
}

migrate-execute-container() {

    local docker_image_name=$1 && shift
    local service_name=$1 && shift
    local container_name=$(docker-get-name $service_name)
    info "Migration command will be executed on container: $container_name"
    local scripts_location=$1 && shift
    debug "Scripts location:  $scripts_location"

    if [ "$scripts_location" = "container" ]; then
        info "Schema will be extracted from image:  $docker_image_name"
        local scripts_location=$(pwd)/.schema/$service_name
        rm -rf $scripts_location
        mkdir -p $scripts_location
        docker run --rm --entrypoint bash -v $scripts_location:/migrate/scripts $docker_image_name -c "cp /schema/* /migrate/scripts/"
    fi

    info "Scripts location:  $scripts_location"
    docker run --link $container_name:db -v $scripts_location:/migrate/scripts sequenceiq/mybatis-migrations:$DOCKER_TAG_MIGRATION $@
}

migrate-cmd() {
    declare desc="Shows the status of the migration"

    cloudbreak-config
    migrate-config

    local service_name=$1 && shift
    case $service_name in
        cbdb)
            local scripts_location=${CB_SCHEMA_SCRIPTS_LOCATION}
            local docker_image_name=sequenceiq/cloudbreak:${DOCKER_TAG_CLOUDBREAK}
            ;;
        pcdb)
            local scripts_location=${PERISCOPE_SCHEMA_SCRIPTS_LOCATION}
            local docker_image_name=sequenceiq/cloudbreak:${DOCKER_TAG_CLOUDBREAK}
            ;;
        *)
            error "Invalid database service name: $service_name. Supported databases: cbdb and pcdb"
            return 1
            ;;
    esac

    debug "Script location: $scripts_location"
    debug "Docker image name: $docker_image_name"
    migrate-execute-container $docker_image_name $service_name $scripts_location $@
}




