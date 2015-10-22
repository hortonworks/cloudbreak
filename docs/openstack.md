#OpenStack based installation

##Deployment prerequisites

###Download the Cloudbreak image

You can download the latest Cloudbreak OpenStack image with the following commands
```
LATEST_IMAGE="cb-openstack-images/cb-centos71-amb212-2015-10-09.img"
LOCAL_IMAGE_NAME="..."
aws s3 cp s3://$LATEST_IMAGE $LOCAL_IMAGE_NAME
```

###Import the image into OpenStack
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

### Initialize Profile

First initialize your directory by creating a `Profile` file:

```
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - the only required
configuration is the `PUBLIC_IP`. This IP will be used to access the Cloudbreak UI
(called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

###Configure the name of the Cloudbreak image
You need to set the Cloudbreak image name you uploaded in your OpenStack cloud in the `Profile` file.
```
export CB_OPENSTACK_IMAGE="$OS_IMAGE_NAME"
```

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
