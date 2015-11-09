#OpenStack deployment

##Supported OpenStack version

Cloudbreak currently only supports the `OpenStack Juno` release.

##Download the Cloudbreak image

You can download the latest pre-configured Cloudbreak Deployer OpenStack image with the following script.

###OpenStack image details

```
curl -L  https://atlas.hashicorp.com/api/v1/artifacts/sequenceiq/cloudbreak/openstack.image/$VERSION/file | tar -xz
```

##Import the image into OpenStack

```
export OS_IMAGE_NAME="name_in_openstack"
export OS_USERNAME=...
export OS_AUTH_URL="http://.../v2.0"
export OS_TENANT_NAME=...
glance image-create --name "$OS_IMAGE_NAME" --file "$LATEST_IMAGE" --disk-format qcow2 --container-format bare --progress
```

##Usage

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the configuration then launch. If you do not have installed Cloudbreak deployer, you can [install this on premise](onprem.md).

##Setup Cloudbreak deployer

If you already have Cloudbreak deployer installed you can start to setup the Cloudbreak application.

Open the `cloudbreak-deployment` directory:

```
cd cloudbreak-deployment
```

This is the directory of the config files and the supporting binaries that will be downloaded by Cloudbreak deployer.

###Initialize your Profile

First initialize your directory by creating a `Profile` file:

```
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - one of the required configurations is the `PUBLIC_IP`.
This IP will be used to access the Cloudbreak UI (called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

The other required configuration in the `Profile` is the name of the Cloudbreak image you uploaded to your OpenStack cloud.

```
export CB_OPENSTACK_IMAGE="$OS_IMAGE_NAME"
```

###Generate your Profile

You are done with the configuration of Cloudbreak deployer. The last thing you have to do is to regenerate the configurations in order to take effect.

```
rm *.yml
cbd generate
```

###Start Cloudbreak

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

###Next steps

Once Cloudbreak is up and running you should check out the [prerequisites](openstack_pre_prov.md) needed to create OpenStack clusters with Cloudbreak.
