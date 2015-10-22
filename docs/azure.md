#AZURE deployment

You already have a cloudbreak-deployer on the machine now we have to start Cloudbreak.

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

### Generate your Profile

You are done with the configuration of Cloudbreak deployer. The last thing you have to do is to regenerate the configurations in order to take effect.

```
rm *.yml
cbd generate
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
cbd logs cloudbreak
```
>You can check the logs when the application is ready. It is about 30 seconds.

Once Cloudbreak is up and running you can launch clusters in two different ways. You can use the [Cloudbreak UI](azure_cb_ui.md) or use the [Cloudbreak shell](azure_cb_shell.md) but before the provisioning please create the [Provisioning prerequisites](azure_pre_prov.md).
