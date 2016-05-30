
migrate-config() {
    declare desc="Defines env variables for migration"

    env-import DOCKER_TAG_MIGRATION 1.0.0
    env-import CB_SCHEMA_SCRIPTS_LOCATION "container"
    env-import CB_SCHEMA_MIGRATION_AUTO true
    env-import PERISCOPE_SCHEMA_SCRIPTS_LOCATION "container"
    env-import PERISCOPE_SCHEMA_MIGRATION_AUTO true
    env-import UAA_SCHEMA_SCRIPTS_LOCATION "container"
    env-import DB_MIGRATION_LOG "db_migration.log"
    env-import VERBOSE_MIGRATION false
}

create-migrate-log() {
    rm -f ${DB_MIGRATION_LOG}
    touch ${DB_MIGRATION_LOG}
}

migrate-startdb() {
    compose-up --no-recreate cbdb pcdb uaadb
}

migrateDebug() {
    declare desc="prints to migrate log file and to stderr"
    echo "[MIGRATE] $*" | tee -a "$DB_MIGRATION_LOG" | debug-cat
}

migrateError() {
    echo "[ERROR] $*" | tee -a "$DB_MIGRATION_LOG" | red 1>&2
}

migrate-execute-mybatis-migrations() {
    local docker_image_name=$1 && shift
    local service_name=$1 && shift
    local container_name=$(compose-get-container $service_name)
    migrateDebug "Migration command on $service_name with params: '$*' will be executed on container: $container_name"
    if [[ ! "$container_name" ]]; then
        migrateError "DB container with matching name is not running. Expected name: .*$service_name.*"
        return 1
    fi
    local scripts_location=$1 && shift
    migrateDebug "Scripts location:  $scripts_location"
    if [ "$scripts_location" = "container" ]; then
        migrateDebug "Schema will be extracted from image:  $docker_image_name"
        local scripts_location=$(pwd)/.schema/$service_name
        rm -rf $scripts_location
        mkdir -p $scripts_location
        docker run --label cbreak.sidekick=true --entrypoint bash -v $scripts_location:/migrate/scripts $docker_image_name -c "cp -r /schema/* /migrate/scripts/"
    fi
    local migration_command=$1
    local new_scripts_location=$scripts_location"/app"
    if [[ "$migration_command" = "up" ]]; then
        new_scripts_location=$scripts_location"/mybatis"
    fi
    migrateDebug "Scripts location:  $new_scripts_location"
    local migrateResult=$(docker run \
        --label cbreak.sidekick=true \
        --link $container_name:db \
        -v $new_scripts_location:/migrate/scripts \
        sequenceiq/mybatis-migrations:$DOCKER_TAG_MIGRATION "$@" \
      | tee -a "$DB_MIGRATION_LOG")

    if ${VERBOSE_MIGRATION}; then
        warn "$migrateResult";
    fi

    if grep -q "MyBatis Migrations SUCCESS" <<< "${migrateResult}"; then
        info "Migration SUCCESS: $service_name $@"
    else
        error "Migration failed: $service_name $@"
        error "See logs in: $DB_MIGRATION_LOG"
    fi
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
        uaadb)
            local scripts_location=${UAA_SCHEMA_SCRIPTS_LOCATION}
            local docker_image_name=${DOCKER_IMAGE_CLOUDBREAK_AUTH}:${DOCKER_TAG_SULTANS}
            ;;
        *)
            migrateError "Invalid database service name: $service_name. Supported databases: cbdb, pcdb and uaadb"
            return 1
            ;;
    esac

    migrateDebug "Script location: $scripts_location"
    migrateDebug "Docker image name: $docker_image_name"
    migrate-execute-mybatis-migrations $docker_image_name $service_name $scripts_location "$@"
}

execute-migration() {
    if [ $# -eq 0 ]; then
        migrate-one-db uaadb up
        migrate-one-db uaadb pending
        migrate-one-db cbdb up
        migrate-one-db cbdb pending
        migrate-one-db pcdb up
        migrate-one-db pcdb pending
    else
        VERBOSE_MIGRATION=true
        migrate-one-db "$@"
    fi
}

migrate() {
    create-migrate-log
    migrate-startdb
    execute-migration
    if grep "MyBatis Migrations FAILURE" "$DB_MIGRATION_LOG" ; then
        error "Migration is failed, please check the log: $DB_MIGRATION_LOG"
        _exit 127
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
    migrate-startdb
    compose-generate-yaml
    execute-migration "$@"
}
