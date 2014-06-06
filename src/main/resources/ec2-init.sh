#!/bin/bash
set -x

: ${NODE_PREFIX=amb}
: ${MYDOMAIN:=mycorp.kom}
: ${IMAGE:="sequenceiq/ambari:dns"}

RESERV=$(curl -s 169.254.169.254/latest/meta-data/reservation-id)
INS_ID=$(curl -s 169.254.169.254/latest/meta-data/instance-id)
ZONE=$(curl -s 169.254.169.254/latest/meta-data/placement/availability-zone)
LAUNCH_IDX=$(curl -s 169.254.169.254/latest/meta-data/ami-launch-index)

export AWS_DEFAULT_REGION=${ZONE%[a-z]}

INSTANCES=$(aws ec2 describe-instances --filter Name=reservation-id,Values=$RESERV --query Reservations[].Instances[].InstanceId --out text)
OTHER_INSTANCES=$(sed "s/$INS_ID//"<<<"$INSTANCES")

#: <<KOMMENT
# my bridge
ifconfig bridge0 down && brctl delbr bridge0
brctl addbr bridge0  && ifconfig bridge0 172.17.1${LAUNCH_IDX}.1  netmask 255.255.255.0

# route to others
# read from stdin <launch-idx> <priv-ip>
# can be used as: ec2-desc-ins | route()
create_routes() {
  while read idx ip; do
    SUBNET=172.17.1$idx
    route add -net $SUBNET.0  netmask 255.255.255.0  gw $ip
  done
}

aws ec2 describe-instances --instance-ids $OTHER_INSTANCES --query Reservations[].Instances[].[AmiLaunchIndex,PrivateIpAddress] --out text | create_routes

netstat -nr

sh -c "cat > /etc/sysconfig/docker" <<"EOF"
other_args="-b bridge0 -H unix:// -H tcp://0.0.0.0:4243"
EOF
/etc/init.d/docker restart
#KOMMENT

LAUNCH_IDX_OF_FIST_OTHER=$(aws ec2 describe-instances --instance-ids $OTHER_INSTANCES --query Reservations[].Instances[0].AmiLaunchIndex --out text)
SERF_JOIN_IP=172.17.1${LAUNCH_IDX_OF_FIST_OTHER}.2
echo SERF_JOIN_IP=$SERF_JOIN_IP


#######################
# WARMUP
#######################
IMAGE=ambari-warmup

#docker run -e SERF_JOIN_IP=$SERF_JOIN_IP -d --dns 127.0.0.1 -h node-$LAUNCH_IDX.mycorp.kom $IMAGE

[ $LAUNCH_IDX -eq 0 ] && AMBARI_ROLE="--tag ambari-role=server,agent" || AMBARI_ROLE=""

CMD="docker run -d -p 8080:8080 -e SERF_JOIN_IP=$SERF_JOIN_IP --dns 127.0.0.1 --name ${NODE_PREFIX}${LAUNCH_IDX} -h ${NODE_PREFIX}${LAUNCH_IDX}.${MYDOMAIN} --entrypoint /usr/local/serf/bin/start-serf-agent.sh  $IMAGE $AMBARI_ROLE"

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
iptables -t nat -A POSTROUTING -s 172.17.1${LAUNCH_IDX}.0/24 ! -d 172.17.0.0/16 -j MASQUERADE
