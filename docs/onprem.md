#On premise installation

To install Cloudbreak Deployer on your selected environment you just have to unzip the platform specific
single binary to your PATH. The one-liner way is:

```
curl https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/install | sh && cbd --version
```

Please note that this requires you to install **Docker 1.7.0** or later, configure firewalls and disable SELinux.

## Usage

Once the Cloudbreak deployer is installed it will generate some config files and will download supporting binaries. It is
advised that you create a dedicated directory for it:

```
mkdir cloudbreak-deployment
cd cloudbreak-deployment
```

### Initialize Profile

First initialize your directory by creating a `Profile` file:

```
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - the only required
configuration is the `PUBLIC_IP`. This IP will be used to access the Cloudbreak UI
(called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

### Pull Docker images

All Cloudbreak components and the backend database is running inside containers.
The **pull command is optional** but you can run it prior to `cbd start`

```
cbd pull
```

It will take some time - depending on your network connection - so you can grab a cup of coffee.

### Start Cloudbreak

To start all the containers run:

```
cbd start
```

Launching the first time will take more time as it does some additional steps:

- download all the docker images, needed by Cloudbreak.
- create **docker-compose.yml**: Full configuration of containers needed for the Cloudbreak deployment.
- create **uaa.yml**: Identity Server configuration.

### Watch the logs

```
cbd logs
```

## Default Credentials

The default credentials can be revealed by `cbd login`

These values are used in the `uaa.yml` end section. To change these values, add 2 lines into your Profile:

```
export UAA_DEFAULT_USER_EMAIL=myself@example.com
export UAA_DEFAULT_USER_PW=demo123
```
and than you need to recreate configs:
```
rm *.yml
cbd generate
```

### Provision clusters

Once your Cloudbreak deployer is up and running you can provision clusters on your favorite cloud environment. Please select the appropriate submenu based on the cloud provider and follow the additional **provider specific configurations** steps from there.

[AWS](aws.md)
[GCP](gcp.md)
[Azure](azure.md)
[OpenStack](openstack.md)
