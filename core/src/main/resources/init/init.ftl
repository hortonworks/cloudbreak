#!/bin/bash
set -x

export CLOUD_PLATFORM="${cloudPlatform}"
export START_LABEL=${platformDiskStartLabel}
export PLATFORM_DISK_PREFIX=${platformDiskPrefix}
export LAZY_FORMAT_DISK_LIMIT=12
export IS_GATEWAY=${gateway?c}
export TMP_SSH_KEY="${tmpSshKey}"
export PUBLIC_SSH_KEY="${publicSshKey}"
export RELOCATE_DOCKER=${relocateDocker?c}
export SSH_USER=${sshUser}

${customUserData}

/usr/bin/user-data-helper.sh "$@" &> /var/log/user-data.log

curl -Lo /etc/yum.repos.d/ambari.repo https://raw.githubusercontent.com/sequenceiq/docker-ambari/master/ambari-agent/ambari.repo
usermod -a -G root centos || :
chmod 555 /

curl -Lo /usr/sbin/cloudbreak-bootstrap https://dl.dropboxusercontent.com/u/13919958/cloudbreak-bootstrap && chmod +x /usr/sbin/cloudbreak-bootstrap
curl -Lo /usr/sbin/consul https://dl.dropboxusercontent.com/u/13919958/consul && chmod +x /usr/sbin/consul

export CBBOOT_PORT=8088
nohup /usr/sbin/cloudbreak-bootstrap > /var/log/cbboot.log &

cat>/etc/dhcp/dhclient.conf<<EOF
prepend domain-name-servers 127.0.0.1;
append domain-search "node.dc1.consul";
EOF

dhclient -q eth0

cat>/etc/systemd/system/consul.service<<EOF
[Install]
WantedBy=multi-user.target

[Unit]
Description=Consul Service
After=network-online.target network.service

[Service]
Restart=on-failure
TimeoutSec=5min
IgnoreSIGPIPE=no
KillMode=process
GuessMainPID=no
ExecStart=/usr/sbin/consul agent -config-dir=/etc/cloudbreak/consul/
EOF

if $IS_GATEWAY; then
  yum install -y ambari-server
  ambari-server setup --silent
#  ambari-server start
else
  yum install -y ambari-agent
  sed -i 's/^hostname=localhost/hostname=ambari-8080.service.consul/' /etc/ambari-agent/conf/ambari-agent.ini
#  ambari-agent start
fi