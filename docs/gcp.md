# Google Cloud deployment

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration then launch. If you do not have installed Cloudbreak deployer, you can [install this on premise](onprem.md).

## Configured image

You can create the latest Cloudbreak deployer image on the [Google Developers Console](https://console.developers.google.com/) with the help
 of the [Google Cloud Shell](https://cloud.google.com/cloud-shell/docs/).
 
![](/images/google-cloud-shell-launch.png)

Images are global resources, so they can be used across zones and projects.

### GCP image details


![](/images/google-cloud-shell.png)

Please make sure you opened the following ports on your virtual machine:
 
 * SSH (22)
 * Ambari (8080)
 * Identity server (8089)
 * Cloudbreak GUI (3000)
 * User authentication (3001)

## Setup Cloudbreak deployer

If you already have cloudbreak-deployer installed you can start to setup the Cloudbreak application.

Open the `cloudbreak-deployment` directory:

```
cd cloudbreak-deployment
```

This is the directory of the config files and the supporting binaries that will be downloaded by Cloudbreak deployer.

### Initialize your Profile

First initialize cbd by creating a `Profile` file:

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

This command applies the following steps: 

- creates the **docker-compose.yml** file that describes the configuration of all the Docker containers needed for the Cloudbreak deployment.
- creates the **uaa.yml** file that holds the configuration of the identity server used to authenticate users to Cloudbreak.

### Start Cloudbreak

To start the Cloudbreak application use the following command.
This will start all the Docker containers and initialize the application. It will take a few minutes until all the services start.

```
cbd start
```

>Launching it first will take more time as it downloads all the docker images needed by Cloudbreak.

After the `cbd start` command finishes you can check the logs of the Cloudbreak server with this command:

```
cbd logs cloudbreak
```
>Cloudbreak server should start within a minute - you should see a line like this: `Started CloudbreakApplication in 36.823 seconds`

### Next steps

Once Cloudbreak is up and running you should check out the [prerequisites](gcp_pre_prov.md) needed to create Google Cloud clusters with Cloudbreak. Besides these you can check out some useful [configurations for Cloudbreak deployer](configuration.md).
