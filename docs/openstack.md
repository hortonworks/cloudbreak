# OpenStack deployment


## Supported OpenStack version

Cloudbreak currently only supports the `OpenStack Juno` release.

## Download the Cloudbreak image

You can download the latest Cloudbreak OpenStack image with the following commands
```
LATEST_IMAGE="cb-openstack-images/cb-centos71-amb212-2015-10-09.img"
LOCAL_IMAGE_NAME="..."
aws s3 cp "s3://$LATEST_IMAGE" "$LOCAL_IMAGE_NAME" --region eu-west-1
```

## Import the image into OpenStack

```
OS_IMAGE_NAME="name_in_openstack"
glance image-create --name "$OS_IMAGE_NAME" --file "$LOCAL_IMAGE_NAME"  --disk-format qcow2 --container-format bare --is-public True --progress
```

## Usage

Once the Cloudbreak deployer is installed it will generate some config files and will download supporting binaries. It is
advised that you create a dedicated directory for it:

```
mkdir -p cloudbreak-deployment
cd cloudbreak-deployment
```

## Initialize your Profile

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

## Next steps

Once Cloudbreak is up and running you should check out the [prerequisites](openstack_pre_prov.md) needed to create OpenStack clusters with Cloudbreak.
