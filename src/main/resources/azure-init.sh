: ${DOCKER_TAG:=consul}
: ${CONSUL_IMAGE:=sequenceiq/consul:v0.4.1.ptr}

set -x

get_ip() {
  ifconfig eth0 | awk '/inet addr/{print substr($2,6)}'
}

fix_hostname_i() {
  if grep -q $(get_ip) /etc/hosts ;then
    echo OK
  else
    echo $(get_ip) $(cat /etc/hostname) >> /etc/hosts
  fi
}

get_vpc_peers() {
  if [ -z "$METADATA_RESULT" ]; then
    # metadata service returns '204: no-content' if metadata is not ready yet and '200: ok' if it's completed
    # every other http status codes mean that something unexpected happened
    METADATA_STATUS=204
    MAX_RETRIES=60
    RETRIES=0
    while [ $METADATA_STATUS -eq 204 ] || [ $METADATA_STATUS -eq 504 ] && [ $RETRIES -ne $MAX_RETRIES ]; do
      METADATA_STATUS=$(curl -sk -o /tmp/metadata_result -w "%{http_code}" -X GET -H Content-Type:application/json $METADATA_ADDRESS/stacks/metadata/$METADATA_HASH);
      echo "Metadata service returned status code: $METADATA_STATUS";
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

  CONSUL_OPTIONS="-advertise $(get_ip)"

  if [ $(my_order) -gt 1 ]; then
    CONSUL_OPTIONS="$CONSUL_OPTIONS -retry-join $(consul_join_ip)"
  fi

  if [ $(my_order) -le 3 ]; then
    CONSUL_OPTIONS="$CONSUL_OPTIONS -server -bootstrap-expect 3"
  fi

  docker rm -f consul &> /dev/null
  docker run -d \
    --name consul \
    --net=host \
    --restart=always \
    $CONSUL_IMAGE $CONSUL_OPTIONS
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
     sequenceiq/ambari:$DOCKER_TAG /start-server

    register_ambari
  fi
}

start_ambari_agent() {
  docker run -d \
    --name ambari-agent \
    --net=host \
    --restart=always \
    sequenceiq/ambari:$DOCKER_TAG /start-agent
}

main() {
  if [[ "$1" == "::" ]]; then
    shift
    eval "$@"
  else
    fix_hostname_i
    start_consul
    start_ambari_server
    start_ambari_agent
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"