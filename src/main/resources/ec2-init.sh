: ${NODE_PREFIX=amb}
: ${MYDOMAIN:=mycorp.kom}
: ${IMAGE:="sequenceiq/ambari"}

# installs jq to process jsons easily
curl -o /usr/bin/jq http://stedolan.github.io/jq/download/linux64/jq
chmod +x /usr/bin/jq

# gets the instance id from ec2 metadata
INSTANCE_ID=$(curl -s 169.254.169.254/latest/meta-data/instance-id)

# jq expression that selects the json entry of the current instance from the array returned by the metadata service
INSTANCE_SELECTOR='. | map(select(.instanceId == "'$INSTANCE_ID'"))'
# jq expression that selects all the other json entries that do not belong to the current instance
OTHER_INSTANCES_SELECTOR='. | map(select(.instanceId != "'$INSTANCE_ID'"))'

# retrieves metadata of the current stack from the cloudbreak metadata service
METADATA_RESULT=$(curl -sX GET -H "Content-Type:application/json" $METADATA_ADDRESS/stacks/metadata/$METADATA_HASH)

# select the docker subnet of the current instance from the metadata result
DOCKER_SUBNET=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].dockerSubnet' | sed s/\"//g)

# creates bridge for docker
ifconfig bridge0 down && brctl delbr bridge0
brctl addbr bridge0  && ifconfig bridge0 ${DOCKER_SUBNET}.1  netmask 255.255.255.0

# route to others
# read from stdin <launch-idx> <priv-ip>
# can be used as: ec2-desc-ins | create_routes
create_routes() {
  while read SUBNET IP; do
    route add -net ${SUBNET}.0  netmask 255.255.255.0  gw $IP
  done
}

route delete -net 172.17.0.0/16

# selects every other instance's docker subnet and private ip from the metadata result and calls create_routes on them
echo $METADATA_RESULT | jq "$OTHER_INSTANCES_SELECTOR" | jq '.[] | (.dockerSubnet + " " + .privateIp)' | sed s/\"//g | create_routes

# to be able to read the netstat table in the logs
netstat -nr

# set bridge0 in docker opts 
sh -c "cat > /etc/default/docker" <<"EOF"
DOCKER_OPTS="-b bridge0 -H unix:// -H tcp://0.0.0.0:4243"
EOF

service docker restart

# find the docker subnet of the first instance from "other instances" - this is used to determine which IP Serf will use to join the cluster  
DOCKER_SUBNET_OF_FIRST_OTHER=$(echo $METADATA_RESULT | jq "$OTHER_INSTANCES_SELECTOR" | jq '.[0].dockerSubnet' | sed s/\"//g)
SERF_JOIN_IP=${DOCKER_SUBNET_OF_FIRST_OTHER}.2

# temporary, while AMI is created with the latest docker version and the latest sequenceiq/ambari image
IMAGE=ambari-warmup

# determines from metadata if this instance is the Ambari server or not and sets the tags accordingly
AMBARI_SERVER=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].ambariServer' | sed s/\"//g)
[ "$AMBARI_SERVER" == true ] && AMBARI_ROLE="--tag ambari-server=true" || AMBARI_ROLE=""

INSTANCE_IDX=$(echo $METADATA_RESULT | jq "$INSTANCE_SELECTOR" | jq '.[].instanceIndex' | sed s/\"//g)

CMD="docker run -d -p 8080:8080 -p 8088:8088 -p 8050:8050 -p 8020:8020 -p 10020:10020 -p 19888:19888 -e SERF_JOIN_IP=$SERF_JOIN_IP --dns 127.0.0.1 --name ${NODE_PREFIX}${INSTANCE_IDX} -h ${NODE_PREFIX}${INSTANCE_IDX}.${MYDOMAIN} --entrypoint /usr/local/serf/bin/start-serf-agent.sh  $IMAGE $AMBARI_ROLE"

cat << EOF
=========================================
CMD=$CMD
=========================================
EOF

$CMD

#######################
# Update POSTROUTING rule created by Docker to support interhost package routing
#######################
iptables -t nat -D POSTROUTING 1
iptables -t nat -A POSTROUTING -s ${DOCKER_SUBNET}.0/24 ! -d 172.17.0.0/16 -j MASQUERADE
