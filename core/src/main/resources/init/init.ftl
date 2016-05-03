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

yum install -y epel-release

adduser saltuser && usermod -G wheel saltuser && echo "saltuser:saltpass"| chpasswd

if $IS_GATEWAY; then
  yum install -y salt-master salt-api
  mkdir -p /etc/salt/master.d/
  cd /etc/salt/master.d/ && curl -O https://raw.githubusercontent.com/akanto/salt-demo/master/salt-master/config/custom.conf
fi

yum install -y salt-minion

#curl -Lo /etc/yum.repos.d/ambari.repo https://raw.githubusercontent.com/sequenceiq/docker-ambari/master/ambari-agent/ambari.repo
#curl -Lo /etc/systemd/system/ambari-server.service https://gist.githubusercontent.com/keyki/1760f6c9b4e00829a18c7876fe4b2fc5/raw/ambari-server.service
#mkdir /opt/ambari-server && curl -Lo /opt/ambari-server/init-server.sh https://gist.githubusercontent.com/keyki/1760f6c9b4e00829a18c7876fe4b2fc5/raw/ambari-server-init.sh && chmod +x /opt/ambari-server/init-server.sh

#curl -Lo /etc/systemd/system/salt-master.service https://dl.dropboxusercontent.com/u/16444381/salt-master.service
#mkdir /opt/salt-master && curl -Lo /opt/salt-master/init-server.sh https://dl.dropboxusercontent.com/u/16444381/init-server.sh && chmod +x /opt/salt-master/init-server.sh

#curl -Lo /etc/systemd/system/ambari-agent.service https://gist.githubusercontent.com/keyki/1760f6c9b4e00829a18c7876fe4b2fc5/raw/ambari-agent.service
#mkdir /opt/ambari-agent && curl -Lo /opt/ambari-agent/init-agent.sh https://gist.githubusercontent.com/keyki/1760f6c9b4e00829a18c7876fe4b2fc5/raw/ambari-agent-init.sh && chmod +x /opt/ambari-agent/init-agent.sh
usermod -a -G root centos || :
chmod 555 /

curl -Lo /usr/sbin/cloudbreak-bootstrap https://dl.dropboxusercontent.com/u/13919958/cloudbreak-bootstrap && chmod +x /usr/sbin/cloudbreak-bootstrap
curl -Lo /usr/sbin/consul https://dl.dropboxusercontent.com/u/13919958/consul && chmod +x /usr/sbin/consul

curl -Lo /etc/systemd/system/cloudbreak-bootstrap.service https://gist.githubusercontent.com/keyki/1760f6c9b4e00829a18c7876fe4b2fc5/raw/cloudbreak-bootstrap.service
echo "Environment='CBBOOT_PORT=7070'" >> /etc/systemd/system/cloudbreak-bootstrap.service
systemctl daemon-reload
service cloudbreak-bootstrap start
systemctl enable cloudbreak-bootstrap

curl -Lo /etc/systemd/system/consul.service https://gist.githubusercontent.com/keyki/1760f6c9b4e00829a18c7876fe4b2fc5/raw/consul.service
mkdir /opt/consul
curl -Lo /opt/consul/stop.sh https://gist.githubusercontent.com/keyki/1760f6c9b4e00829a18c7876fe4b2fc5/raw//stop.sh && chmod +x /opt/consul/stop.sh

cat>/etc/dhcp/dhclient.conf<<EOF
prepend domain-name-servers 127.0.0.1;
prepend domain-search "service.consul";
prepend domain-search "node.dc1.consul";
EOF
service network restart


#yum install -y haveged
#haveged
#chkconfig haveged on

sed -i "/^hosts:/ s/ *files dns/ dns files/" /etc/nsswitch.conf

export JDK_ARTIFACT=jdk-7u67-linux-x64.tar.gz
mkdir -p /usr/jdk64 && cd /usr/jdk64 && wget http://public-repo-1.hortonworks.com/ARTIFACTS/$JDK_ARTIFACT && \
    tar -xf $JDK_ARTIFACT && rm -f $JDK_ARTIFACT

if $IS_GATEWAY; then
  curl -Lo /sbin/cert-tool https://github.com/ehazlett/certm/releases/download/v0.0.1/cert-tool_linux_amd64 && chmod +x /sbin/cert-tool
  yum install -y nginx
  # make service redirect to systemctl
  rm -f /etc/init.d/ambari-server
  find /etc/rc.d/rc* -name "*ambari-server" | xargs rm -v
else
  curl -Lo /etc/ambari-agent/conf/public-hostname.sh https://raw.githubusercontent.com/sequenceiq/docker-ambari/master/ambari-agent/public-hostname.sh && chmod +x /etc/ambari-agent/conf/public-hostname.sh
  sed -i "/\[agent\]/ a public_hostname_script=\/etc\/ambari-agent\/conf\/public-hostname.sh" /etc/ambari-agent/conf/ambari-agent.ini
  # make service redirect to systemctl
  rm -f /etc/init.d/ambari-agent
  find /etc/rc.d/rc* -name "*ambari-agent" | xargs rm -v
fi



