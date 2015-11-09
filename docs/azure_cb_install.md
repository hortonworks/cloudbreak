If you want to use our cloudbreak-deployer on Azure then please follow the steps below.
Start an **OpenLogic 7.1** on Azure. Make sure you opened the **22** port for SSH and **3000** and **3001** for our UI.

Please log in to the machine with SSH or use username and password authentication (the following example shows how to ssh into the machine):

```
ssh -i <azure-ssh-pem-file> <username>@<machine-ip>
```

Assume **root** privileges with this command:

```
sudo su
```

Configure the correct yum repository on the machine:

```
cat > /etc/yum.repos.d/CentOS-Base.repo <<"EOF"
# CentOS-Base.repo
#
# The mirror system uses the connecting IP address of the client and the
# update status of each mirror to pick mirrors that are updated to and
# geographically close to the client.  You should use this for CentOS updates
# unless you are manually picking other mirrors.
#
# If the mirrorlist= does not work for you, as a fall back you can try the
# remarked out baseurl= line instead.
#
#

[base]
name=CentOS-$releasever - Base
mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=os&infra=$infra
#baseurl=http://mirror.centos.org/centos/$releasever/os/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7

#released updates
[updates]
name=CentOS-$releasever - Updates
mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=updates&infra=$infra
#baseurl=http://mirror.centos.org/centos/$releasever/updates/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7

#additional packages that may be useful
[extras]
name=CentOS-$releasever - Extras
mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=extras&infra=$infra
#baseurl=http://mirror.centos.org/centos/$releasever/extras/$basearch/
gpgcheck=1
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7

#additional packages that extend functionality of existing packages
[centosplus]
name=CentOS-$releasever - Plus
mirrorlist=http://mirrorlist.centos.org/?release=$releasever&arch=$basearch&repo=centosplus&infra=$infra
#baseurl=http://mirror.centos.org/centos/$releasever/centosplus/$basearch/
gpgcheck=1
enabled=0
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
EOF
```

Install the correct version of **kernel**, **kernel-tools** and **systemd**:

```
yum install -y kernel-3.10.0-229.14.1.el7 kernel-tools-3.10.0-229.14.1.el7 systemd-208-20.el7_1.6
```

To permanently disable **SELinux** set SELINUX=disabled in /etc/selinux/config This ensures that SELinux does not turn itself on after you reboot the machine:

```
setenforce 0 && sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/selinux/config
```

You need to install iptables-services, otherwise the 'iptables save' command will not be available:

```
yum -y install iptables-services net-tools unzip
```

Please configure you iptables on your machine:

```
iptables --flush INPUT && \
iptables --flush FORWARD && \
service iptables save && \
sed -i 's/net.ipv4.ip_forward = 0/net.ipv4.ip_forward = 1/g' /etc/sysctl.conf
```

Configure a custom Docker repository for installing the correct version of Docker:

```
cat > /etc/yum.repos.d/docker.repo <<"EOF"
[dockerrepo]
name=Docker Repository
baseurl=https://yum.dockerproject.org/repo/main/centos/7
enabled=1
gpgcheck=1
gpgkey=https://yum.dockerproject.org/gpg
EOF
```

Then you are able to install the Docker service:

```
yum install -y docker-engine-1.8.3
```

Configure your installed Docker service:

```
cat > /usr/lib/systemd/system/docker.service <<"EOF"
[Unit]
Description=Docker Application Container Engine
Documentation=https://docs.docker.com
After=network.target docker.socket cloud-final.service
Requires=docker.socket
Wants=cloud-final.service

[Service]
ExecStart=/usr/bin/docker -d -H fd:// -H tcp://0.0.0.0:2376 --selinux-enabled=false --storage-driver=devicemapper --storage-opt=dm.basesize=30G
MountFlags=slave
LimitNOFILE=200000
LimitNPROC=16384
LimitCORE=infinity

[Install]
WantedBy=multi-user.target
EOF
```

Remove docker folder and restart Docker service:

```
rm -rf /var/lib/docker && systemctl daemon-reload && service docker start && systemctl enable docker.service
```

Download **cloudbreak-deployer**:

```
mkdir -p cloudbreak-deployer && cd cloudbreak-deployer && curl https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/install | sh && cbd --version
```
