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
  sh -c ' echo other_args=\"--label type=hostgroup --host=unix:///var/run/docker.sock --host=tcp://0.0.0.0:2376\" >> /etc/sysconfig/docker'
  service docker restart
  docker pull sequenceiq/docker-consul-watch-plugn:1.7.0-consul
  docker pull sequenceiq/munchausen:0.1
  docker pull swarm:0.2.0-rc2
  docker pull sequenceiq/registrator:v5.1
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
    configure_docker
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"