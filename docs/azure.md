# Azure deployment

If you already have cloudbreak-deployer installed you can start to setup the Cloudbreak application.

Create a cloudbreak-deployment directory for the config files and the supporting binaries that will be downloaded by cloudbreak-deployer:

```
mkdir -p cloudbreak-deployer
cd cloudbreak-deployer
```

## Initialize your Profile

First initialize cbd by creating a `Profile` file:

```
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

## Start Cloudbreak

To start the Cloudbreak application use the following command.
This will start all the Docker containers and initialize the application. It will take a few minutes until all the services start.

```
cbd start
```

Launching it the first time will take more time as it does some additional steps:

- downloads all the docker images needed by Cloudbreak.
- creates the **docker-compose.yml** file that describes the configuration of all the Docker containers needed for the Cloudbreak deployment.
- creates the **uaa.yml** file that holds the configuration of the identity server used to authenticate users to Cloudbreak.

After the `cbd generate` command finishes you can check the logs of the Cloudbreak server with this command:

```
cbd logs cloudbreak
```
>Cloudbreak server should start within a minute - you should see a line like this: `Started CloudbreakApplication in 36.823 seconds`

Once Cloudbreak is up and running you should check out the [prerequisites](azure_pre_prov.md) needed to create Azure clusters with Cloudbreak.

