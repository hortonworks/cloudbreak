# Google Cloud Images

We have pre-built cloud images for GCP with the Cloudbreak Deployer pre-installed. Following the steps will guide you through the provider specific configuration then launch.

> Alternatively, instead of using the pre-built cloud images, you can install Cloudbreak Deployer on your own VM. See [install the Cloudbreak Deployer](onprem.md) for more information.

## Configured Image

You can create the latest Cloudbreak deployer image on the [Google Developers Console](https://console.developers.google.com/) with the help
 of the [Google Cloud Shell](https://cloud.google.com/cloud-shell/docs/).
 
![](/images/google-cloud-shell-launch.png)

Images are global resources, so they can be used across zones and projects.

### GCP Image Details


![](/images/google-cloud-shell.png)

Please make sure you opened the following ports on your virtual machine:
 
 * SSH (22)
 * Ambari (8080)
 * Identity server (8089)
 * Cloudbreak GUI (3000)
 * User authentication (3001)

## Setup Cloudbreak Deployer

Once you have the Cloudbreak Deployer installed, proceed to [Setup Cloudbreak Deployer](gcp.md).
