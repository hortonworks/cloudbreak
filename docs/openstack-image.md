# OpenStack Cloud Images

We have pre-built cloud images for OpenStack with the Cloudbreak Deployer pre-installed. Following the steps will guide you through the provider specific configuration then launch.

> Alternatively, instead of using the pre-built cloud images, you can install Cloudbreak Deployer on your own VM. See [install the Cloudbreak Deployer](onprem.md) for more information.

##System Requirements

Cloudbreak currently only supports the `OpenStack Juno` release.

##Download the Cloudbreak image

You can download the latest pre-configured Cloudbreak deployer image for OpenStack with the following script in the 
following section.

Please make sure you opened the following ports on your virtual machine:
 
 * SSH (22)
 * Ambari (8080)
 * Identity server (8089)
 * Cloudbreak GUI (3000)
 * User authentication (3001)

###OpenStack image details


##Import the image into OpenStack

```
export OS_IMAGE_NAME="name_in_openstack"
export OS_USERNAME=...
export OS_AUTH_URL="http://.../v2.0"
export OS_TENANT_NAME=...
glance image-create --name "$OS_IMAGE_NAME" --file "$LATEST_IMAGE" --disk-format qcow2 --container-format bare --progress
```

## Setup Cloudbreak Deployer

Once you have the Cloudbreak Deployer installed, proceed to [Setup Cloudbreak Deployer](openstack.md).
