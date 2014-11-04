: ${NODE_PREFIX=amb}
: ${MYDOMAIN:=mycorp.kom}
: ${IMAGE:="sequenceiq/ambari:1.6.0"}
: ${INSTANCE_ID:="0"}

# instance id from azure metadata
INSTANCE_ID=$(sudo cat /var/lib/waagent/ovf-env.xml |grep -oPm1 "(?<=<HostName>)[^<]+")

# jq expression that selects the json entry of the current instance from the array returned by the metadata service
INSTANCE_SELECTOR='. | map(select(.instanceId == "'$INSTANCE_ID'"))'
# jq expression to select ambariServer from metadata
AMBARI_SERVER_SELECTOR='. | map(select(.ambariServer))'

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
# persist the mount points to fstab
VOLUME_COUNT=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].volumeCount' | sed s/\"//g)
START_LABEL=62
for (( i=1; i<=VOLUME_COUNT; i++ )); do
  LABEL=$(printf "\x$((START_LABEL+i))")
  mkfs -F -t ext4 /dev/sd${LABEL}
  mkdir /mnt/fs${i}
  echo /dev/sd${LABEL} /mnt/fs${i} ext4  defaults 0 2 >> /etc/fstab
  mount /mnt/fs${i}
  DOCKER_VOLUME_PARAMS="${DOCKER_VOLUME_PARAMS} -v /mnt/fs${i}:/mnt/fs${i}"
done

FQDN=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].longName' | sed s/\"//g)
DOMAIN=$(echo $FQDN | cut -d'.' -f 2-6)

echo "nameserver 127.0.0.1" > /run/resolvconf/resolv.conf
echo "search $DOMAIN" >> /run/resolvconf/resolv.conf
echo "nameserver 168.63.129.16" >> /run/resolvconf/resolv.conf

# on restart azure removes a nameserver from resolv.conf
# on instance start write it back otherwise
# datanodes fail to resolve the hostnames
cat <<EOF > /etc/init.d/update_resolvconf
  #!/bin/bash
  sed -i '1i nameserver 127.0.0.1' /run/resolvconf/resolv.conf
EOF
chmod +x /etc/init.d/update_resolvconf
update-rc.d update_resolvconf defaults

docker-runing() {
    if docker info &> /dev/null ; then
        echo UP;
    else
        echo DOWN;
    fi;
}
: ${DOCKER_MAX_RETRIES:=60}
local docker_retries
docker_retries=0
while [[ "$(docker-runing)" == "DOWN" ]] && [ $docker_retries -ne $DOCKER_MAX_RETRIES ];
do
	service docker restart
	sleep 8
	((docker_retries++))
done

# determines if this instance is the Ambari server or not and sets the tags accordingly
AMBARI_SERVER=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].ambariServer' | sed s/\"//g)
[ "$AMBARI_SERVER" == true ] && AMBARI_ROLE="--tag ambari-server=true" || AMBARI_ROLE=""

INSTANCE_IDX=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].instanceIndex' | sed s/\"//g)

AMBARI_SERVER_IP=$(echo $METADATA_RESULT | jq "$AMBARI_SERVER_SELECTOR" | jq '.[].privateIp' | sed s/\"//g)
CMD="docker run -d $DOCKER_VOLUME_PARAMS --restart=always -e SERF_JOIN_IP=$AMBARI_SERVER_IP --net=host --name ${NODE_PREFIX}${INSTANCE_IDX} --entrypoint /usr/local/serf/bin/start-serf-agent.sh  $IMAGE $AMBARI_ROLE"

cat << EOF
=========================================
CMD=$CMD
=========================================
EOF

$CMD