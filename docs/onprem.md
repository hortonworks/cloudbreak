# Install Cloudbreak Deployer

To install Cloudbreak Deployer on your selected environment you have to follow the steps below. The instruction describe a CentOS-based installation.

> **IMPORTANT:** If you plan to use Cloudbreak on Azure, you **must** use the [Azure Setup](azure.md) instructions to install and configure the Cloudbreak.

## System Requirements

To run the Cloudbreak Deployer and install the Cloudbreak Application, you must meet the following system requirements:

 * RHEL / CentOS / Oracle Linux 7 (64-bit)
 * Docker 1.9.1

> You can install Cloudbreak on Mac OS X "Darwin" for **evaluation purposes only**. This operating system is not supported for a production deployment of Cloudbreak.

Make sure you opened the following ports:

 * SSH (22)
 * Cloudbreak (8080)
 * Identity server (8089)
 * Cloudbreak GUI (3000)
 * User authentication (3001)

Every command shall be executed as root **root**. In order to get root privileges execute:

```
sudo -i
```

Ensure that your system is up-to date and reboot if necessary (e.g. there was a kernel update)  :

```
yum -y update
```

You need to install iptables-services, otherwise the 'iptables save' command will not be available:

```
yum -y install iptables-services net-tools
```

Please configure permissive iptables on your machine:

```
iptables --flush INPUT && \
iptables --flush FORWARD && \
service iptables save
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
yum install -y docker-engine-1.9.1 docker-engine-selinux-1.9.1
systemctl start docker
systemctl enable docker
```

## Install Cloudbreak deployer

Install the Cloudbreak deployer and unzip the platform specific single binary to your PATH. The one-liner way is:

```
yum -y install unzip tar
curl https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/install-latest | sh && cbd --version
```

Once the Cloudbreak deployer is installed, you can start to setup the Cloudbreak application.

## Initialize your Profile

First initialize cbd by creating a `Profile` file:

```
mkdir cloudbreak-deployment
cd cloudbreak-deployment
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - the only required
configuration is the `PUBLIC_IP`. This IP will be used to access the Cloudbreak UI
(called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

## Generate your Profile

You are done with the configuration of Cloudbreak deployer. The last thing you have to do is to regenerate the configurations in order to take effect.

```
rm *.yml
cbd generate
```

This command applies the following steps:

- creates the **docker-compose.yml** file that describes the configuration of all the Docker containers needed for the Cloudbreak deployment.
- creates the **uaa.yml** file that holds the configuration of the identity server used to authenticate users to Cloudbreak.

## Start Cloudbreak

To start the Cloudbreak application use the following command.
This will start all the Docker containers and initialize the application. It will take a few minutes until all the services start.

```
cbd pull
cbd start
```

>Launching it first will take more time as it downloads all the docker images needed by Cloudbreak.

After the `cbd start` command finishes you can check the logs of the Cloudbreak server with this command:

```
cbd logs cloudbreak
```
>Cloudbreak server should start within a minute - you should see a line like this: `Started CloudbreakApplication in 36.823 seconds`


## Troubleshooting

If you are experiencing with permission or connection issues, then try to permanently disable **SELinux**. Setting the SELINUX=disabled in /etc/selinux/config  ensures that SELinux is not turned on after reboot of the machine:

```
setenforce 0 && sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/selinux/config
```


## Next steps

Now that you all pre-requisites for Cloudbreak are in place you can follow with the **cloud provider specific** configuration. Based on the location where you plan to launch HDP clusters select one of the providers documentation and follow the steps from the **Deployment** section.

You can find the provider specific documentations here:

* [AWS](aws.md)
* [Azure](azure.md)
* [GCP](gcp.md)
* [OpenStack](openstack.md)
