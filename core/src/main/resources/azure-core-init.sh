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

configure_docker() {
  rm -rf /etc/docker/key.json
  sed -i "/other_args=/d" /etc/sysconfig/docker
  sh -c ' echo DOCKER_TLS_VERIFY=0 >> /etc/sysconfig/docker'
  sh -c ' echo other_args=\"--storage-opt dm.basesize=30G --label type=hostgroup --host=unix:///var/run/docker.sock --host=tcp://0.0.0.0:2376\" >> /etc/sysconfig/docker'
  service docker restart
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
    configure_docker
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"