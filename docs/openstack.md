#OpenStack based installation

We have pre-built a custom OpenStack image available on VM Depot with all the required tooling and Cloudbreak deployer installed. In order to launch this image on OpenStack please use the following [image]().

Cloudbreak will already be installed, thus you can follow these steps to launch the application.

## Usage

Once the Cloudbreak deployer is installed it will generate some config files and will download supporting binaries. It is
advised that you create a dedicated directory for it:

```
mkdir -p cloudbreak-deployment
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

### Start Cloudbreak

To start all the containers run:

```
cbd start
```

Launching the first time will take more time as it does some additional steps:

- download all the docker images, needed by Cloudbreak.
- create **docker-compose.yml**: Full configuration of containers needed for the Cloudbreak deployment.
- create **uaa.yml**: Identity Server configuration.

This will start all the Docker containers and initialize the application. Please give a few minutes until all services starts. While the services are starting you can check the logs.

### Watch the logs

```
cbd logs
```
