#GCP deployment

You already have a cloudbreak-deployer on the machine now we have to start cloudbreak.

```
mkdir -p cloudbreak-deployer
cd cloudbreak-deployer
```

## Initialize Profile

First initialize your directory by creating a `Profile` file:

```
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - the only required
configuration is the `PUBLIC_IP`. This IP will be used to access the Cloudbreak UI
(called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

### Change default username/Password

The default credentials can be revealed by `cbd login` These values are used in the `uaa.yml` file's end section. To change these values, add 2 lines into your Profile:

```
export UAA_DEFAULT_USER_EMAIL=myself@example.com
export UAA_DEFAULT_USER_PW=demo123
```

### Regenerate your Profile

There is available a "cbd regenerate" command for this.

```
cbd regenerate
```

### Verify configurations

In order to verify that all configs are OK use the `doctor` command.

```
cbd doctor
```

### Pull Docker images

All Cloudbreak components and the backend database is running inside containers. The pull command is optional but you can run it prior to cbd start

```
cbd pull
```

## Use Cloudbreak

To start the Cloudbreak application use the following command.

```
cbd start
```

Launching the first time will take more time as it does some additional steps:

- download all the docker images, needed by Cloudbreak.
- create **docker-compose.yml**: Full configuration of containers needed for the Cloudbreak deployment.
- create **uaa.yml**: Identity Server configuration.

This will start all the Docker containers and initialize the application. Please give a few minutes until all services starts. While the services are starting you can check the logs.

```
cbd logs
```
>You can check the logs when the application is ready. It is about 30 minutes.

Once Cloudbreak is up and running you have to make some provider based configuration. You can use the [Cloudbreak UI]().
