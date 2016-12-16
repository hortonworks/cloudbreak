db-init() {
    env-import DB_DUMP_VOLUME cbreak_dump
    env-import REMOVE_CONTAINER "--rm"
    cloudbreak-conf-tags
    cloudbreak-conf-db
}

db-getpostgres-image-version() {
    declare desc="Detects which docker image version is matching the db volume"
    declare dbVolume=${1:? required: dbVolume}

    local pgver=$(docker run --rm \
        -v $dbVolume:/data \
        alpine:$DOCKER_TAG_ALPINE cat /data/PG_VERSION
    )
    echo "${pgver}-alpine"

}

db-dump() {
    declare desc="creates an sql dump from the db volume"
    declare dbVolume=${1:? required: dbVolume}

    if ! ( [[ $dbVolume =~ ^/ ]] || docker volume inspect $dbVolume &>/dev/null ); then
        error "docker volume $dbVolume doesnt exists"
        _exit 1
    fi

    if docker run --rm -v $dbVolume:/data alpine:$DOCKER_TAG_ALPINE test -f /data/PG_VERSION; then
        debug "postgres DB exists on volume: $dbVolume"
    else
        error "$dbVolume doesnt contains a postgres DB"
        _exit 1
    fi

    local contName=cbreak_dump
    docker run -d \
        --name $contName \
        -v $dbVolume:/var/lib/postgresql/data \
        -v $DB_DUMP_VOLUME:/dump \
        postgres:$(db-getpostgres-image-version $dbVolume)

    local timeStamp=$(date "+%Y%m%d_%H%M")
    local volDir="/dump/${dbVolume##*/}"
    local dumpDir="$volDir/${timeStamp}"
    local latestDir="$volDir/latest"
    
    debug "create dump dir: $dumpDir"
    docker exec $contName sh -c "mkdir -p $dumpDir"

    db-wait-for-db-cont $contName

    docker exec $contName sh -c 'pg_dump -U postgres > '$dumpDir'/dump.sql'

    debug "create latest simlink $latestDir -> $dumpDir"
    docker exec $contName sh -xc "rm -f $latestDir && ln -s $dumpDir $latestDir"

    docker stop $contName
    [[ "$REMOVE_CONTAINER" ]] && docker rm -f $contName
}

db-init-volume-from-dump() {
    declare desc="initialize the DB if needed, from a previous dump"
    declare volName=${1:? required: volName}
    declare dbName=${2:? required dbName}
    # call TODO
}

db-wait-for-db-cont() {
    declare desc="wait for db container readiness"
    declare contName=${1:? required db container name}

    # todo max time
    while ! docker exec $contName pg_isready &> /dev/null; do
        debug "waiting for DB: $contName to start ..."
        sleep 1;
    done
}

db-restore-volume-from-dump() {
    declare desc="initialize the DB if needed, from a previous dump"
    declare volName=${1:? required: volName}
    declare dumpDbName=${2:? required dumpDbName}
    declare newDbName=${3:-$dumpDbName}

    debug "db: $newDbName in volume: $volName will be restore from: $dumpDbName dump"
    local dbDoneFile=".${newDbName}.done"
    local contName=cbreak_initdb
    debug "test if $dbDoneFile exists ..."
    if docker run $REMOVE_CONTAINER -v $volName:/data alpine:$DOCKER_TAG_ALPINE test -e /data/$dbDoneFile &>/dev/null; then
        debug "db initaliazation is already DONE"
    else
        debug "db initaliazation needed ..."

        local contName=cbreak_restore
        docker run -d \
            --name $contName \
            -v $volName:/var/lib/postgresql/data \
            -v $DB_DUMP_VOLUME:/dump \
            postgres:$DOCKER_TAG_POSTGRES

        db-wait-for-db-cont $contName
        sleep 5
        db-wait-for-db-cont $contName

        if [[ $newDbName != "postgres" ]]; then
            debug "create new database: $dbName"
            docker exec $contName psql -U postgres -c "create database $newDbName"
        fi

        docker exec $contName psql -U postgres $newDbName -f /dump/$dumpDbName/latest/dump.sql

        docker stop $contName
        [[ "$REMOVE_CONTAINER" ]] && docker rm -f $contName
    fi

}

db-list-dumps() {
    docker run $REMOVE_CONTAINER \
        -v $DB_DUMP_VOLUME:/dump \
        alpine:$DOCKER_TAG_ALPINE \
          find /dump -name \*.sql -exec ls -lh {} \;
}

