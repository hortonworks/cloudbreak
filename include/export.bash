create-bundle() {
    declare desc="Exports and anonymizes logs for fault analysis. Usage: cbd create-bundle [archive_name]"
    declare archivename=$1

    if ! [ $(id -u) = 0 ]; then
        error "Please run as root, otherwise the tool cannot collect some necessary information for analysis."
        _exit 1
    fi

    if [[ -z $archivename ]] || [[ "/" == $archivename ]]; then
        archivename="cbd_export_$(date +%s)"
        warn "Invalid archive filename. Generated filename: $archivename"
    fi

    local collectdir="tmp_collected_$(date +%s)"
    mkdir -p $archivename
    mkdir -p $collectdir 

    export-psql-history $collectdir
    export-prerequisites $collectdir

    export-cbd-env-show $collectdir
    export-cbd-doctor $collectdir
    export-cbd-version $collectdir
    export-cbd-ps $collectdir

    anonymize-exported-files $collectdir $archivename

    archive-directory $archivename $collectdir
}

export-psql-history() {
    ! docker exec -t cbreak_commondb_1 cat /var/lib/postgresql/.psql_history > "$1/.psql_history"
    check-export-success "$1/.psql_history"
}

export-prerequisites() {
    local out="$1/prerequisites.out"

    if command_exists getenforce; then
        printf "\n### SELinux\n" >> $out
        ! echo SELinux status: $(getenforce) >> $out
    else
        warn "Command not supported: getenforce"
    fi

    if command_exists ifconfig; then
        printf "\n### MTU Sizes\n" >> $out
        ! ifconfig | grep mtu | awk {'print $1 " " $NF'} >> $out
    else
        warn "Command not supported: ifconfig"
    fi

    printf "\n### Open ports\n" >> $out
    ! docker run --rm --name=cbreak_export_netstat --network=host alpine:3.7 netstat -tulnp >> $out

    if command_exists iptables; then
        printf "\n### IPTABLES\n" >> $out
        ! iptables -L -x -n >> $out
    else
        warn "Command not supported: iptables"
    fi

    if command_exists firewall-cmd; then
        printf "\n### firewalld\n" >> $out
        ! echo firewalld status: $(firewall-cmd --state) >> $out
        ! firewall-cmd --list-all-zones >> $out
    fi

    check-export-success $out
}

check-export-success() {
    if [[ -a $1 ]]; then
        info "Exported: $(basename $1)"
    else
        warn "Failed to export: $(basename $1)"
    fi
}

export-cbd-env-show() {
    ! env-show &> "$1/cbd_env_show.out"
    check-export-success "$1/cbd_env_show.out"
}

export-cbd-doctor() {
    ! doctor &> "$1/cbd_doctor.out"
    check-export-success "$1/cbd_doctor.out"
}

export-cbd-version() {
    ! cbd-version &> "$1/cbd_version.out"
    check-export-success "$1/cbd_version.out"
}

export-cbd-ps() {
    ! compose-ps &> "$1/cbd_ps.out"
    check-export-success "$1/cbd_ps.out" 
}

archive-directory() {
    ! tar -czvf "$(basename $1).tar.gz" $1/ &> /dev/null
    if [[ -a "$(basename $1).tar.gz" ]]; then
        info "Archive created from exported files: $(basename $1).tar.gz"
        ! rm -rf $1
        ! rm -rf $2
        if [[ -a $2 ]]; then
            warn "Failed to remove temporary directory: $2"
        fi
    fi
}

anonymize-exported-files() {
    cloudbreak-config
    info "Anonymization started."

    ! docker run -e ACCOUNT_ID=$AWS_ACCOUNT_ID \
        -e CB_SMARTSENSE_CONFIGURE='true' \
        -e CB_SMARTSENSE_COLLECT_DIAG_BUNDLE='true' \
        -e CB_VERSION=$(echo $(bin-version)) \
        -e CB_SMARTSENSE_ID='A-99903636-C-36363636' \
        -e CB_SMARTSENSE_CLUSTER_NAME_PREFIX \
        -e CB_INSTANCE_UUID \
        -e CB_INSTANCE_PROVIDER \
        -e CB_INSTANCE_REGION \
        -e CB_PRODUCT_ID \
        -e CB_COMPONENT_ID \
        -e UAA_FLEX_USAGE_CLIENT_ID \
        -e UAA_FLEX_USAGE_CLIENT_SECRET \
        -e SMARTSENSE_UPLOAD_HOST \
        -e SMARTSENSE_UPLOAD_USERNAME \
        -e SMARTSENSE_UPLOAD_PASSWORD \
        --rm \
        --name=cbreak_export_smartsense \
        -v $(pwd):/var/lib/cloudbreak-deployment \
        -v $(pwd)/$1:/var/lib/cloudbreak-deployment/cfg \
        -v $(pwd)/$2:/var/lib/cloudbreak-deployment/hst_bundle \
        --dns=$PRIVATE_IP \
        -p 9001:9000 \
        $DOCKER_IMAGE_CBD_SMARTSENSE:$DOCKER_TAG_CBD_SMARTSENSE &> /dev/null
    
    rm -rf cfg
    rm -rf hst_bundle

    local collectioncount=$(ls $2 | wc -l)
    if [[ $(( collectioncount + 0 )) = 0 ]]; then
        error "Anonymization failed."
        cp -r $1/ $2/
        cp -r logs $2
        cp -r etc $2
    else
        info "Anonymization finished."
    fi

}