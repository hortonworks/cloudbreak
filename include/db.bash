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
    declare dbName=${2:-postgres}

    if ! ( [[ $dbVolume =~ ^/ ]] || docker volume inspect $dbVolume &>/dev/null ); then
        error "docker volume $dbVolume doesnt exists"
        _exit 1
    fi

    if docker run --rm -v $dbVolume:/data alpine:$DOCKER_TAG_ALPINE test -f /data/PG_VERSION &> /dev/null; then
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
        postgres:$(db-getpostgres-image-version $dbVolume) | debug-cat

    local timeStamp=$(date "+%Y%m%d_%H%M")
    local volDir="/dump"
    if [[ "$dbName" == "postgres" ]] ; then
        volDir+="/${dbVolume##*/}"
    else
        volDir+="/${dbName}"
    fi

    local dumpDir="$volDir/${timeStamp}"
    local latestDir="$volDir/latest"
    
    debug "create dump dir: $dumpDir"
    docker exec $contName sh -c "mkdir -p $dumpDir"

    db-wait-for-db-cont $contName

    docker exec $contName sh -c 'pg_dump -U postgres '$dbName' > '$dumpDir'/dump.sql' | debug-cat

    debug "create latest simlink $latestDir -> $dumpDir"
    docker exec $contName sh -xc "rm -f $latestDir && ln -s $dumpDir $latestDir" 2>&1 | debug-cat

    docker stop $contName | debug-cat
    [[ "$REMOVE_CONTAINER" ]] && docker rm -f $contName | debug-cat
}

db-merge-3-to-1() {
    declare desc="Merge 3 db into 1 single common db"

    cloudbreak-config
    for db in cbdb uaadb periscopedb; do
        db-dump $CB_DB_ROOT_PATH/$db
        db-restore-volume-from-dump $COMMON_DB_VOL $db
    done
}

db-wait-for-db-cont() {
    declare desc="wait for db container readiness"
    declare contName=${1:? required db container name}

    local maxtry=${RETRY:=30}
    while (! docker exec $contName pg_isready &> /dev/null) && (( maxtry -= 1 > 0 )); do
        debug "waiting for DB: $contName to start [tries left: $maxtry] ..."
        sleep 1;
    done
}

db-restore-volume-from-dump() {
    declare desc="initialize the DB if needed, from a previous dump"
    declare volName=${1:? required: new volName}
    declare dumpDbName=${2:? required dumpDbName cbdb/pcdb/uaadb}
    declare newDbName=${3:-$dumpDbName}

    debug "db: $newDbName in volume: $volName will be restore from: $dumpDbName dump"
    local dbDoneFile="/var/lib/postgresql/data/.${newDbName}.done"
    local dbCreatedFile="/var/lib/postgresql/data/.created"
    
    local contName=cbreak_initdb
    debug "test if $dbDoneFile exists ..."
    if docker run --rm -v $volName:/var/lib/postgresql/data/ alpine:$DOCKER_TAG_ALPINE test -e $dbDoneFile &>/dev/null; then
        info "db initaliazation is already DONE"
    else
        debug "creates volume: $volName"
        local contName=cbreak_restore
        docker run -d \
            --name $contName \
            -v $volName:/var/lib/postgresql/data \
            -v $DB_DUMP_VOLUME:/dump \
            postgres:$DOCKER_TAG_POSTGRES \
            bash -c "echo 'touch $dbCreatedFile' > /docker-entrypoint-initdb.d/created.sh && /docker-entrypoint.sh postgres" \
              | debug-cat

        local maxtry=${RETRY:=30}
        while (! docker exec $contName test -e $dbCreatedFile &> /dev/null ) && (( maxtry -= 1 > 0 )); do
            debug "waiting for DB: $newDbName created [tries left: $maxtry] ..."
            sleep 1;
        done

        db-wait-for-db-cont $contName

        if [[ $newDbName != "postgres" ]]; then
            debug "create new database: $dbName"
            docker exec $contName psql -U postgres -c "create database $newDbName" | debug-cat
        fi

        docker exec $contName psql -U postgres $newDbName -f /dump/$dumpDbName/latest/dump.sql > restore_vol_$volName.log
        docker exec $contName touch $dbDoneFile | debug-cat

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

