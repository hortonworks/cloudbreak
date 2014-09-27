: ${NODE_PREFIX=amb}
: ${MYDOMAIN:=mycorp.kom}
: ${IMAGE:="sequenceiq/ambari:1.6.0"}
: ${INSTANCE_ID:="0"}


# instance id from ec2 metadata
INSTANCE_ID=$(curl -s http://metadata.google.internal/computeMetadata/v1/instance/hostname -H "Metadata-Flavor: Google")
# jq expression that selects the json entry of the current instance from the array returned by the metadata service
INSTANCE_SELECTOR='. | map(select(.longName == "'$INSTANCE_ID'"))'
# jq expression to select ambariServer from metadata
AMBARI_SERVER_SELECTOR='. | map(select(.ambariServer))'
# jq expression that selects all the other json entries
OTHER_INSTANCES_SELECTOR='. | map(select(.instanceId != "'$INSTANCE_ID'"))'

# metadata service returns '204: no-content' if metadata is not ready yet and '200: ok' if it's completed
# every other http status codes mean that something unexpected happened
METADATA_STATUS=204
MAX_RETRIES=60
RETRIES=0
while [ $METADATA_STATUS -eq 204 ] && [ $RETRIES -ne $MAX_RETRIES ]; do
  METADATA_STATUS=$(curl -sk -o /tmp/metadata_result -w "%{http_code}" -X GET -H Content-Type:application/json $METADATA_ADDRESS/stacks/metadata/$METADATA_HASH);
  echo "Metadata service returned status code: $METADATA_STATUS";
  [ $METADATA_STATUS -eq 204 ] && sleep 5 && ((RETRIES++));
done

[ $METADATA_STATUS -ne 200 ] && exit 1;

METADATA_RESULT=$(cat /tmp/metadata_result)

# format and mount disks
CONTAINER_COUNT=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].containerCount' | sed s/\"//g)
VOLUME_COUNT=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].volumeCount' | sed s/\"//g)
START_LABEL=61
for (( i=1; i<=VOLUME_COUNT; i++ )); do
  LABEL=$(printf "\x$((START_LABEL+i))")
  mkfs -F -t ext4 /dev/sd${LABEL}
  mkdir /mnt/fs${i}
  mount /dev/sd${LABEL} /mnt/fs${i}
  if [ "$CONTAINER_COUNT" -gt 0 ]
  then
     act_cluster_size=$CONTAINER_COUNT-1
     for act in $(seq $((act_cluster_size))); do
        mkdir /mnt/fs${i}/${act}
     done
     DOCKER_VOLUME_PARAMS="${DOCKER_VOLUME_PARAMS} -v /mnt/fs${i}/param:/mnt/fs${i}/param"
  else
    DOCKER_VOLUME_PARAMS="${DOCKER_VOLUME_PARAMS} -v /mnt/fs${i}:/mnt/fs${i}"
  fi

done

if [ "$CONTAINER_COUNT" -gt 0 ]
then
    DOCKER_SUBNET=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].dockerSubnet' | sed s/\"//g)
    # creates bridge for docker
    apt-get install bridge-utils
    ifconfig bridge0 down && brctl delbr bridge0
    brctl addbr bridge0  && ifconfig bridge0 ${DOCKER_SUBNET}  netmask 255.255.255.0
    netstat -nr

    # set bridge0 in docker opts
    echo DOCKER_OPTS="-b=bridge0" >> /etc/default/docker
fi

service docker restart
sleep 8

 # determines if this instance is the Ambari server or not and sets the tags accordingly
AMBARI_SERVER=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].ambariServer' | sed s/\"//g)
[ "$AMBARI_SERVER" == true ] && AMBARI_ROLE="--tag ambari-server=true" || AMBARI_ROLE=""
INSTANCE_IDX=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].instanceIndex' | sed s/\"//g)
AMBARI_SERVER_IP=$(echo $METADATA_RESULT | jq "$AMBARI_SERVER_SELECTOR" | jq '.[].privateIp' | sed s/\"//g)
AMBARI_SERVER_DOCKER_SUBNET=$(echo $METADATA_RESULT | jq "$AMBARI_SERVER_SELECTOR" | jq '.[].dockerSubnet' | sed s/\"//g)

if [ "$CONTAINER_COUNT" -eq 0 ]
then
    CMD="docker run -d $DOCKER_VOLUME_PARAMS -e SERF_JOIN_IP=$AMBARI_SERVER_IP --net=host --name ${NODE_PREFIX}${INSTANCE_IDX} --entrypoint /usr/local/serf/bin/start-serf-agent.sh  $IMAGE $AMBARI_ROLE"
    cat << EOF
    =========================================
    CMD=$CMD
    =========================================
EOF
    $CMD
else
    AMB_IP_SEGMENT=`echo $AMBARI_SERVER_DOCKER_SUBNET | cut -d \. -f 3`
    JOIN_IP=172.18.$AMB_IP_SEGMENT.2
    if [ "$AMBARI_SERVER" == true ]
    then
        DC_VOLUME_PARAMS=$(echo $DOCKER_VOLUME_PARAMS |sed "s/param/0/g")
        # find the docker subnet of the first instance from "other instances" - this is used to determine which IP Serf will use to join the cluster
        CMD="docker run -p 8080:8080 -d $DC_VOLUME_PARAMS -e SERF_JOIN_IP= --dns 127.0.0.1 --name ${NODE_PREFIX}${INSTANCE_IDX} -h ${NODE_PREFIX}${INSTANCE_IDX}.${MYDOMAIN} --entrypoint /usr/local/serf/bin/start-serf-agent.sh  sequenceiq/ambari:pam-fix --tag ambari-server=true"
        cat << EOF
        =========================================
        CMD=$CMD
        =========================================
EOF
        $CMD
        act_cluster_size=$CONTAINER_COUNT-1
        for i in $(seq $((act_cluster_size))); do
            DC_VOLUME_PARAMS=$(echo $DOCKER_VOLUME_PARAMS |sed "s/param/${i}/g")
            CMD="docker run -d $DC_VOLUME_PARAMS -e SERF_JOIN_IP=$JOIN_IP --dns 127.0.0.1 --name ${NODE_PREFIX}${INSTANCE_IDX}$i -h ${NODE_PREFIX}${INSTANCE_IDX}$i.${MYDOMAIN} --entrypoint /usr/local/serf/bin/start-serf-agent.sh  sequenceiq/ambari:pam-fix"
            cat << EOF
            =========================================
            CMD=$CMD
            =========================================
EOF

        $CMD
        done
    else
        act_cluster_size=$CONTAINER_COUNT
        for i in $(seq $((act_cluster_size))); do
            DC_VOLUME_PARAMS=$(echo $DOCKER_VOLUME_PARAMS |sed "s/param/${i}/g")
            CMD="docker run -d $DC_VOLUME_PARAMS -e SERF_JOIN_IP=$JOIN_IP --dns 127.0.0.1 --name ${NODE_PREFIX}${INSTANCE_IDX}$i -h ${NODE_PREFIX}${INSTANCE_IDX}$i.${MYDOMAIN} --entrypoint /usr/local/serf/bin/start-serf-agent.sh  sequenceiq/ambari:pam-fix"
            cat << EOF
            =========================================
            CMD=$CMD
            =========================================
EOF

        $CMD
        done
    fi
fi

