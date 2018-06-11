db-init() {
    cloudbreak-conf-tags
    cloudbreak-conf-db
    migrate-config
}

db-dump() {
    declare desc="Dumping the specified database"
    declare dbName=${1:-all}

    if docker inspect cbreak_commondb_1 &> /dev/null; then
        migrate-startdb
        db-wait-for-db-cont cbreak_commondb_1
    fi

    if [ "$dbName" = "all" ]; then
        for db in $CB_DB_ENV_DB $IDENTITY_DB_NAME $PERISCOPE_DB_ENV_DB; do
            db-dump-database $db
        done
    else
        db-dump-database $dbName
    fi
}

db-dump-database() {
    declare dbName=${1:? required: dbName}
    declare desc="creates an sql dump from the database: $dbName"
    info $desc

    if docker exec cbreak_commondb_1 psql -U postgres -c "\c $dbName;" &>/dev/null; then
        local timeStamp=$(date "+%Y%m%d_%H%M")
        local backupFolder="db_backup"
        local backupLocation=$backupFolder"/"$dbName"_"$timeStamp".dump"
        debug "Creating dump of database: $dbName, into the file: $backupLocation"
        mkdir -p $backupFolder
        docker exec cbreak_commondb_1 pg_dump -Fc -U postgres "$dbName" > "$backupLocation" | debug-cat
    else
        error "The specified database $dbName doesn't exist."
        _exit 1
    fi
}

db-initialize-databases() {
    declare desc="Initialize and migrate databases"
    info $desc

    cloudbreak-config

    migrate-startdb
    db-wait-for-db-cont cbreak_commondb_1

    for db in $CB_DB_ENV_DB $IDENTITY_DB_NAME $PERISCOPE_DB_ENV_DB; do
        db-create-database $db
    done
}

db-create-database() {
    declare desc="Creates new empty database"
    declare newDbName=${1:? required: newDbName}

    if [[ $newDbName != "postgres" ]]; then
        if docker exec cbreak_commondb_1 psql -U postgres -c "\c $newDbName;" &>/dev/null; then
            debug "The database with name $newDbName already exists, no need for creation."
        else
            debug "create new database: $newDbName"
            docker exec cbreak_commondb_1 psql -U postgres -c "create database $newDbName;" | debug-cat
        fi
    fi
}

db-wait-for-db-cont() {
    declare desc="wait for db container readiness"
    declare contName=${1:? required db container name}

    debug $desc
    local maxtry=${RETRY:=30}
    #Polling non-loopback port binding due to automatic restart after init https://github.com/docker-library/postgres/issues/146#issuecomment-215856076
    while ! docker exec -i cbreak_commondb_1 netstat -tulpn |grep -i "0 0.0.0.0:5432" &> /dev/null; do
        debug "waiting for DB: $contName to listen on non-loopback interface [tries left: $maxtry] ..."
        maxtry=$((maxtry-1))
        if [[ $maxtry -gt 0 ]]; then
            sleep 1;
        else
            error "The database hasn't started within 30 seconds."
            _exit 1
        fi
    done

    while ! docker exec -i cbreak_commondb_1 pg_isready &> /dev/null; do
        debug "waiting for DB: $contName to be ready [tries left: $maxtry] ..."
        maxtry=$((maxtry-1))
        if [[ $maxtry -gt 0 ]]; then
            sleep 1;
        else
            error "The database hasn't started within 30 seconds."
            _exit 1
        fi
    done
}

db-restore() {
    declare desc="Restoring the specified database from the specified dump file"
    declare dbName=${1:? required: dbName}
    declare dumpFilePath=${2:? required: dumpFilePath}
    info "Restoring database: $dbName from dump, file: $dumpFilePath"

    migrate-startdb
    db-wait-for-db-cont cbreak_commondb_1
    db-create-database $dbName

    if [[ -f "$dumpFilePath" ]]; then
        local destFileName=$dbName"-to-restore-"$(date "+%Y%m%d_%H%M")
        debug "copy $dumpFilePath into the container as: $destFileName"
        docker cp $dumpFilePath cbreak_commondb_1:/$destFileName | debug-cat
        debug "Restoring database: $dbName from file: $destFileName with pg_restore."
        docker exec -i cbreak_commondb_1 pg_restore -U postgres -d $dbName $destFileName | debug-cat
    fi
}