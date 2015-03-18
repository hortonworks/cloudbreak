: ${CONSUL_IMAGE:=sequenceiq/consul:v0.4.1.ptr}
: ${CONSUL_WATCH_IMAGE:=sequenceiq/docker-consul-watch-plugn:1.7.0-consul}

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
    while [ $METADATA_STATUS -ne 200 ]; do
      METADATA_STATUS=$(curl -sk -m 10 -o /tmp/metadata_result -w "%{http_code}" -X GET -H Content-Type:application/json $METADATA_ADDRESS/stacks/metadata/$METADATA_HASH);
      [ $METADATA_STATUS -ne 200 ] && sleep 5;
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
  meta_order | grep "\b${myip}$" | cut -d" " -f 1
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
    CONSUL_OPTIONS="$CONSUL_OPTIONS -retry-join $leader"
  else
    if [ $(my_order) -gt 1 ]; then
      CONSUL_OPTIONS="$CONSUL_OPTIONS -retry-join $(consul_join_ip)"
    fi

    if [ $(my_order) -le "$CONSUL_SERVER_COUNT" ]; then
      CONSUL_OPTIONS="$CONSUL_OPTIONS -server -bootstrap-expect $CONSUL_SERVER_COUNT"
    fi
  fi
}

start_consul_watch() {
  docker rm -f consul-watch &> /dev/null
  docker run -d \
    --name consul-watch \
    --privileged \
    --net=host \
    --restart=always \
    -e TRACE=1 \
    -e BRIDGE_IP=$(get_ip) \
    -v /var/run/docker.sock:/var/run/docker.sock \
    $CONSUL_WATCH_IMAGE
}

does_cluster_exist() {
  ip_arr=($(get_vpc_peers))
  for ip in "${ip_arr[@]}"; do
    leader=$(curl -s -m 2 ${ip}:8500/v1/status/leader|jq . -r)
    if [ -n "$leader" ]; then
      leader=${leader%:*}
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

use_dns_first() {
  docker exec ambari-server sed -i "/^hosts:/ s/ *files dns/ dns files/" /etc/nsswitch.conf
  docker exec consul sed -i "/^hosts:/ s/ *files dns/ dns files/" /etc/nsswitch.conf
  docker exec consul-watch sed -i "/^hosts:/ s/ *files dns/ dns files/" /etc/nsswitch.conf
}

start_ambari_server() {
  docker rm -f ambari-server &>/dev/null
  docker run -d --name=ambari_db --privileged --restart=always -v /data/ambari-server/pgsql/data:/var/lib/postgresql/data -e POSTGRES_PASSWORD=bigdata -e POSTGRES_USER=ambari postgres:9.4.1
  sleep 10
  docker run -d --name=ambari-server --privileged --net=host --restart=always -e POSTGRES_DB=$(docker inspect -f "{{.NetworkSettings.IPAddress}}" ambari_db) -e BRIDGE_IP=$(get_ip) sequenceiq/ambari:$AMBARI_DOCKER_TAG /start-server
  register_ambari
}

set_disk_as_volumes() {
  for fn in `ls /hadoopfs/ | grep fs`; do
    VOLUMES="$VOLUMES -v /hadoopfs/$fn:/hadoopfs/$fn"
  done
}

format_disks() {
  /usr/local/disk_mount.sh
}

remove_run_init_scripts() {
  # avoid re-run init-script
  sed -i "/run-scripts/d" /usr/share/google/run-startup-scripts
}

main() {
  if [[ "$1" == "::" ]]; then
    shift
    eval "$@"
  else
    remove_run_init_scripts
    format_disks
    fix_hostname
    start_consul
    start_ambari_server
    start_consul_watch
    use_dns_first
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"
