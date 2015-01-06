: ${DOCKER_TAG:=1.7.0-consul}
: ${CONSUL_IMAGE:=sequenceiq/consul:v0.4.1.ptr}

set -x

get_ip() {
  ifconfig eth0 | awk '/inet addr/{print substr($2,6)}'
}

fix_hostname() {
  if grep -q $(get_ip) /etc/hosts ;then
    sed -i "/$(get_ip)/d" /etc/hosts
  else
    echo OK
  fi
}

get_vpc_peers() {
  if [ -z "$METADATA_RESULT" ]; then
    METADATA_STATUS=204
    MAX_RETRIES=60
    RETRIES=0
    while [ $METADATA_STATUS -eq 204 ] || [ $METADATA_STATUS -eq 504 ] && [ $RETRIES -ne $MAX_RETRIES ]; do
      METADATA_STATUS=$(curl -sk -o /tmp/metadata_result -w "%{http_code}" -X GET -H Content-Type:application/json $METADATA_ADDRESS/stacks/metadata/$METADATA_HASH);
      [ $METADATA_STATUS -eq 204 ] && sleep 5 && ((RETRIES++));
    done

    [ $METADATA_STATUS -ne 200 ] && exit 1;
  fi

  METADATA_RESULT=$(cat /tmp/metadata_result)
  echo $METADATA_RESULT | jq .[].privateIp -r
}

meta_order() {
  get_vpc_peers | xargs -n 1 | sort | cat -n | sed 's/ *//;s/\t/ /'
}

my_order() {
  local myip=$(get_ip)
  meta_order | grep ${myip} | cut -d" " -f 1
}

consul_join_ip() {
  meta_order | head -1 | cut -d" " -f 2
}

start_consul() {
  get_consul_opts
  docker rm -f consul &> /dev/null
  docker run -d \
    --name consul \
    --net=host \
    --restart=always \
    $CONSUL_IMAGE $CONSUL_OPTIONS
}

get_consul_opts() {
  CONSUL_OPTIONS="-advertise $(get_ip)"

  if does_cluster_exist; then
    con_join
  else
    if [ $(my_order) -gt 1 ]; then
      con_join
    fi

    if [ $(my_order) -le 3 ]; then
      CONSUL_OPTIONS="$CONSUL_OPTIONS -server -bootstrap-expect 3"
    fi
  fi
}

con_join() {
  CONSUL_OPTIONS="$CONSUL_OPTIONS -retry-join $(consul_join_ip)"
}

does_cluster_exist() {
  ip_arr=($(get_vpc_peers))
  for ip in "${ip_arr[@]}"; do
    leader=$(curl -s ${ip}:8500/v1/status/leader|jq . -r)
    if [ -n "$leader" ]; then
      return 0
    fi
  done
  return 1
}

consul_leader() {
  local leader=$(curl -s 127.0.0.1:8500/v1/status/leader|jq . -r)
  while [ -z "$leader" ]; do
    sleep 1
    leader=$(curl -s 127.0.0.1:8500/v1/status/leader|jq . -r)
  done
  echo ${leader%:*}
}

con() {
  declare path="$1"
  shift
  local consul_ip=127.0.0.1

  curl ${consul_ip}:8500/v1/${path} "$@"
}

register_ambari() {
  JSON=$(cat <<ENDOFJSON
  {
     "ID":"$(hostname -i):ambari:8080",
     "Name":"ambari-8080",
     "Port":8080,
     "Check":null
  }
ENDOFJSON
  )

  con agent/service/register -X PUT -d @- <<<"$JSON"
}

start_ambari_server() {
  docker rm -f ambari-server &>/dev/null
  if [[ "$(consul_leader)" ==  "$(get_ip)" ]]; then
    docker run -d \
     --name ambari-server \
     --net=host \
     --restart=always \
     -e BRIDGE_IP=$(get_ip) \
     sequenceiq/ambari:$DOCKER_TAG /start-server

    register_ambari
  fi
}

start_ambari_agent() {
  set_public_host_script
  set_disk_as_volumes
  docker run -d \
    --name ambari-agent \
    --net=host \
    --restart=always \
    -e BRIDGE_IP=$(get_ip) \
    $VOLUMES \
    sequenceiq/ambari:$DOCKER_TAG /start-agent
}

set_disk_as_volumes() {
  for fn in `ls /mnt/ | grep fs`; do
    VOLUMES="$VOLUMES -v /mnt/$fn:/mnt/$fn"
  done
}

set_public_host_script() {
  VOLUMES="$VOLUMES -v /usr/local/public_host_script:/etc/ambari-agent/conf/public-hostname.sh"
}

register_ambari_add() {
  cp /usr/local/register-ambari /etc/init.d/register-ambari
  chmod +x /etc/init.d/register-ambari
  chown root:root /etc/init.d/register-ambari
  update-rc.d -f register-ambari defaults
  update-rc.d -f register-ambari enable
}

format_disks() {
  /usr/local/disk_mount.sh
}

main() {
  if [[ "$1" == "::" ]]; then
    shift
    eval "$@"
  else
    format_disks
    fix_hostname
    start_consul
    start_ambari_server
    start_ambari_agent
    register_ambari_add
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"