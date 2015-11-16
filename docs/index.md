# Introduction

Cloudbreak simplifies the provisioning, management and monitoring of on-demand HDP clusters in virtual and cloud environments. Cloudbreak leverages the cloud infrastructure platforms to create host instances, uses Docker technology to deploy the requisite containers cloud-agnostically, and uses Apache Ambari (via Ambari Blueprints) to install and manage the HDP cluster.

Use the Cloudbreak UI or CLI to launch HDP clusters on public cloud infrastructure platforms such as Microsoft Azure, Amazon Web Services (AWS), and Google Cloud Platform (GCP) and the private cloud infrastructure platform OpenStack (available as Technical Preview).

Cloudbreak has two main components: the **Cloudbreak Application** and the **Cloudbreak Deployer**.

The **Cloudbreak Application** is made up from microservices (Cloudbreak, Uluwatu, Sultans, ...). The **Cloudbreak Deployer** helps you to deploy the Cloudbreak application automatically in environments with Docker support. Once the Cloudbreak Application is deployed you can use it to provision HDP clusters in different cloud environments.

> For an architectural overview of the Cloudbreak Deployer, the Cloudbreak Application, Apache Ambari, Docker and the rest of the Cloudbreak components, please follow this [link](architecture.md).

## Installation

The high-level process to be able to use Cloudbreak to install an HDP cluster includes the following steps:

1. **Install the Cloudbreak Deployer** by either: [installing the Cloudbreak Deployer](#install-deployer); or instantiating one of the [pre-built cloud images
that includes Cloudbreak Deployer](#pre-built-images) pre-installed.
2. **Configure the Cloudbreak Deployer and install the Cloudbreak Application**. Once you have installed Cloudbreak Deployer (cbd), it will start up several Docker containers: Cloudbreak API, Cloudbreak UI (called Uluwatu), Identity Server, and supporting databases. You have finished this step, if you are able to login in your browser to Cloudbreak UI (Uluwatu).
3. **Provision an HDP Cluster** using the Cloudbreak Application.

<div id="install-deployer"></div>
### Installing the Cloudbreak Deployer

You can install the Cloudbreak Deployer on your own VM manually. Once installed, you will use the deployer to setup
the Cloudbreak Application. We suggest you install the Cloudbreak Application as close to the
desired HDP clusters as possible. For example, if you plan to launch clusters on AWS, install the Cloudbreak Application in AWS.

Follow the instructions for [installing the Cloudbreak Deployer](onprem.md). Alternatively, you can consider using one of the [pre-built cloud images that includes Cloudbreak Deployer](#pre-built-images) pre-installed.

> **IMPORTANT:** If you plan to use Cloudbreak on Azure, you **must** use the [Azure Setup](azure.md) instructions to install and configure the Cloudbreak.


<div id="pre-built-images"></div>
### Using the Pre-Built Cloud Images

We have pre-built cloud images with Cloudbreak Deployer pre-installed. Following the steps will guide you through the provider specific configuration and launching clusters using that cloud provider.

| Cloud | Cloud Image |
|---|---|
| AWS | You can follow the AWS instructions using this [link](aws-image.md). |
| Azure | There are no pre-built cloud images available for Azure. See [Azure Setup](azure.md) to get the Cloudbreak Deployer installed and configured. |
| GCP | You can follow the GCP instructions using this [link](gcp-image.md) |
| OpenStack | You can follow the OpenStack instructions using this [link](openstack-image.md) |

## Learn More

For more information on Cloudbreak, Docker, Ambari and Ambari Blueprints, see:

| Resource | Description |
|---|---|
|[Cloudbreak Project](http://hortonworks.com/hadoop/cloudbreak/) | Cloudbreak is a tool to help simplify the provisioning of HDP clusters in virtual and cloud environments. |
|[Cloudbreak Forums](hortonworks.com/community/forums/forum/cloudbreak/) | Get connected with the community in the Cloudbreak Forums. |
|[Apache Ambari Project](http://hortonworks.com/hadoop/ambari/) | Apache Ambari is an operational platform for provisioning, managing, and monitoring Apache Hadoop clusters. Ambari exposes a robust set of REST APIs and a rich Web interface for cluster management. |
|[Ambari Blueprints](https://cwiki.apache.org/confluence/display/AMBARI/Blueprints)| Ambari Blueprints are a declarative definition of a Hadoop cluster that Ambari can use to create Hadoop clusters. |
|[Docker](https://www.docker.com/) | Docker is an open platform for developers and system administrators to build, ship, and run distributed applications. |