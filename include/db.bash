db-init(){
    env-import DB_DUMP_VOLUME cbreak_dump
    env-import DB_DONE_FILE .cbreak.init.done
    env-import REMOVE_CONTAINER "--rm"
    cloudbreak-conf-tags
}

db-dump() {
    declare desc="creates an sql dump from the db volume"
    declare dbVolume=${1:? required: dbVolume}

    local dumpContainer=cbreak_dump
    docker run -d \
        --name $dumpContainer \
        -v $dbVolume:/var/lib/postgresql/data \
        -v $DB_DUMP_VOLUME:/dump \
        postgres:$DOCKER_TAG_POSTGRES

    local timeStamp=$(date "+%Y%m%d_%H%M")
    local volDir="/dump/${dbVolume##*/}"
    local dumpDir="$volDir/${timeStamp}"
    local latestDir="$volDir/latest"
    
    debug "create dump dir: $dumpDir"
    docker exec $dumpContainer sh -c "mkdir -p $dumpDir"

    while ! docker exec $dumpContainer psql -U postgres -c "select 1" &> /dev/null; do
        debug "waiting for DB: $dumpContainer to start ..."
        sleep 1;
    done

    docker exec $dumpContainer sh -c 'pg_dumpall -U postgres | grep -v ROLE > '$dumpDir'/dump.sql'

    debug "create latest simlink $latestDir -> $dumpDir"
    docker exec $dumpContainer sh -xc "rm -f $latestDir && ln -s $dumpDir $latestDir"

    docker stop $dumpContainer
    [[ "$REMOVE_CONTAINER" ]] && docker rm -f $dumpContainer
}

db-init-volume-from-dump() {
    declare desc="initialize the DB if needed, from a previous dump"
    declare volName=${1:? required: volName}

    local initContainer=cbreak_initdb
    debug "test if $DB_DONE_FILE exists ..."
    if docker run $REMOVE_CONTAINER -v $volName:/data alpine:$DOCKER_TAG_ALPINE test -e /data/$DB_DONE_FILE &>/dev/null; then
        debug "db initaliazation is already DONE"
    else
        debug "db initaliazation needed, search for latest backup"
        docker run -d \
            --name $initContainer \
            -v $volName:/var/lib/postgresql/data \
            -v $DB_DUMP_VOLUME:/dump \
            --entrypoint=bash \
            postgres:$DOCKER_TAG_POSTGRES \
              -c "echo 'touch /var/lib/postgresql/data/$DB_DONE_FILE && kill -INT 1' > /docker-entrypoint-initdb.d/99.sh && ln -s /dump/$volName/latest/dump.sql /docker-entrypoint-initdb.d/00.sql && /docker-entrypoint.sh postgres"
            # sed -i '3 i\set -x ' /docker-entrypoint.sh ;

        debug "waiting for container: $initContainer to FINISH ..."
        while ! docker exec $initContainer test -e /var/lib/postgresql/data/$DB_DONE_FILE &>/dev/null; do
            debug "waiting for file: /var/lib/postgresql/data/$DB_DONE_FILE"
            sleep 1;
        done

        docker stop $initContainer
        [[ "$REMOVE_CONTAINER" ]] && docker rm -f $initContainer
    fi

}

db-list-dumps() {
    docker run $REMOVE_CONTAINER \
        -v $DB_DUMP_VOLUME:/dump \
        alpine:$DOCKER_TAG_ALPINE \
          find /dump -name \*.sql -exec ls -lh {} \;
}

