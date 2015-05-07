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

fix_docker_port() {
  rm -rf /etc/docker/key.json
  sed -i "/other_args=/d" /etc/sysconfig/docker
  sh -c ' echo DOCKER_TLS_VERIFY=0 >> /etc/sysconfig/docker'
  sh -c ' echo other_args=\"--storage-opt dm.basesize=30G --label type=gateway --host=unix:///var/run/docker.sock --host=tcp://0.0.0.0:2376\" >> /etc/sysconfig/docker'
  service docker restart
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
    fix_docker_port
  fi
}

[[ "$0" == "$BASH_SOURCE" ]] && main "$@"