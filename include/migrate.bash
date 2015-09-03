
migrate-config() {
    declare desc="Defines env variables for migration"

    env-import DOCKER_TAG_MIGRATION 1.0.0
    env-import CB_SCHEMA_SCRIPTS_LOCATION "container"
    env-import PERISCOPE_SCHEMA_SCRIPTS_LOCATION "container"
    env-import SKIP_DB_MIGRATION_ON_START false
    env-import DB_MIGRATION_LOG "db_migration.log"
}

create-migrate-log() {
    rm -f ${DB_MIGRATION_LOG}
    touch ${DB_MIGRATION_LOG}
}

migrate-startdb() {
    compose-up --no-recreate cbdb pcdb
}

migrate-execute-mybatis-migrations() {

    local docker_image_name=$1 && shift
    local service_name=$1 && shift
    local container_name=$(compose-get-container $service_name)
    info "Migration command on $service_name with \"$@\" params will be executed on container: $container_name" 2> >(tee -a "$DB_MIGRATION_LOG" >&2)
    if [ -z "$container_name" ]
    then
        error "DB container with matching name is not running. Expected name: .*$service_name.*" 2> >(tee -a "$DB_MIGRATION_LOG" >&2)
        return 1
    fi
    local scripts_location=$1 && shift
    debug "Scripts location:  $scripts_location" 2> >(tee -a "$DB_MIGRATION_LOG" >&2)

    if [ "$scripts_location" = "container" ]; then
        info "Schema will be extracted from image:  $docker_image_name" 2> >(tee -a "$DB_MIGRATION_LOG" >&2)
        local scripts_location=$(pwd)/.schema/$service_name
        rm -rf $scripts_location
        mkdir -p $scripts_location
        docker run --rm --entrypoint bash -v $scripts_location:/migrate/scripts $docker_image_name -c "cp /schema/* /migrate/scripts/"
    fi

    info "Scripts location:  $scripts_location" 2> >(tee -a "$DB_MIGRATION_LOG" >&2)
    docker run --rm --link $container_name:db -v $scripts_location:/migrate/scripts sequenceiq/mybatis-migrations:$DOCKER_TAG_MIGRATION $@ \
      | tee -a "$DB_MIGRATION_LOG" | grep "Applying\|MyBatis Migrations SUCCESS\|MyBatis Migrations FAILURE" 2>/dev/null
}

migrate-one-db() {
    local service_name=$1 && shift

    case $service_name in
        cbdb)
            local scripts_location=${CB_SCHEMA_SCRIPTS_LOCATION}
            local docker_image_name=sequenceiq/cloudbreak:${DOCKER_TAG_CLOUDBREAK}
            ;;
        pcdb)
            local scripts_location=${PERISCOPE_SCHEMA_SCRIPTS_LOCATION}
            local docker_image_name=sequenceiq/periscope:${DOCKER_TAG_PERISCOPE}
            ;;
        *)
            error "Invalid database service name: $service_name. Supported databases: cbdb and pcdb" 2> >(tee -a "$DB_MIGRATION_LOG" >&2)
            return 1
            ;;
    esac

    debug "Script location: $scripts_location" 2> >(tee -a "$DB_MIGRATION_LOG" >&2)
    debug "Docker image name: $docker_image_name" 2> >(tee -a "$DB_MIGRATION_LOG" >&2)
    migrate-execute-mybatis-migrations $docker_image_name $service_name $scripts_location "$@"
}

execute-migration() {
    local params="$@"
    if [ -z "$params" ]
    then
        migrate-one-db cbdb up
        migrate-one-db cbdb pending
        migrate-one-db pcdb up
        migrate-one-db pcdb pending
    else
        migrate-one-db $params
    fi
}

migrate() {
    create-migrate-log
    migrate-startdb
    execute-migration
    if grep "MyBatis Migrations FAILURE" "$DB_MIGRATION_LOG" ; then
        error "Migration is failed, please check the log: $DB_MIGRATION_LOG"
        exit 127
    fi
}

migrate-startdb-cmd() {
    declare desc="Starts the DB containers"

    deployer-generate
    migrate-startdb
}

migrate-cmd() {
    declare desc="Executes the db migration"
    debug "migrate-cmd"

    cloudbreak-config
    migrate-config
    compose-generate-yaml
    execute-migration "$@"
}
