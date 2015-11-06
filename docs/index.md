<<<<<<< HEAD
##Introduction
=======
# Introduction
>>>>>>> Add Content additions, minor cleanup

Cloudbreak simplifies the provisioning, management and monitoring of on-demand HDP clusters in virtual and cloud environments. Cloudbreak leverages the cloud infrastructure platforms to create host instances, uses Docker technology to deploy the requisite containers cloud-agnostically, and uses Apache Ambari (via Ambari Blueprints) to install and manage the HDP cluster.

Use the Cloudbreak UI or CLI to launch HDP clusters on public cloud infrastructure platforms such as Microsoft Azure, Amazon Web Services (AWS), and Google Cloud Platform (GCP) and the private cloud infrastructure platform OpenStack (available as Technical Preview).

## Technology

Cloudbreak has two main components: the **Cloudbreak Application** and the **Cloudbreak Deployer**.

<<<<<<< HEAD
The **Cloudbreak Application** is made up from microservices (Cloudbreak, Uluwatu, Sultans, ...). The **Cloudbreak Deployer** helps you to deploy the Cloudbreak application automatically in environments with Docker support. Once the Cloudbreak Application is deployed you can use it to provision HDP clusters in different cloud environments.

> For an architectural overview of the [Cloudbreak Deployer](http://sequenceiq.com/cloudbreak-deployer), the Cloudbreak Application, Apache Ambari, Docker and the rest of the Cloudbreak components, please follow this [link](technology.md).

##System Requirements

To run the Cloudbreak Deployer and install the Cloudbreak Application, you must meet the following system requirements:

- RHEL / CentOS / Oracle Linux 6 (64-bit)
- Docker 1.6.0 (or later)
    - [Docker for RHEL](http://docs.docker.com/engine/installation/rhel/)
    - [Docker for CentOS](https://docs.docker.com/installation/centos/)

> You can install Cloudbreak on Mac OS X "Darwin" for **evaluation purposes only**. This operating system is not supported for a production deployment of Cloudbreak.
=======
The **Cloudbreak Application** is made up from microservices (Cloudbreak, Uluwatu, Sultans, ...). The **Cloudbreak 
Deployer** helps you to deploy the Cloudbreak application automatically in environments with Docker support. Once the Cloudbreak Application is deployed you can use it to provision HDP clusters in different cloud environments.

> For an architectural overview of the [Cloudbreak Deployer](http://sequenceiq.com/cloudbreak-deployer), the Cloudbreak Application, Apache Ambari, Docker and the rest of the Cloudbreak components, please follow this [link](technology.md).

For an architectural overview of the Cloudbreak deployer and the Cloudbreak application please follow this [link](technology.md).
>>>>>>> Add Content additions, minor cleanup

##System Requirements

To run the Cloudbreak Deployer and install the Cloudbreak Application, you must meet the following system requirements:

* RHEL / CentOS / Oracle Linux 6 (64-bit)
* Docker 1.6.0 (or later)
  * [Docker for RHEL](http://docs.docker.com/engine/installation/rhel/)
  * [Docker for CentOS](https://docs.docker.com/installation/centos/)

> You can install Cloudbreak on Mac OS X "Darwin" for **evaluation purposes only**. This operating system is not supported for a production deployment of Cloudbreak.

## Process Overview

The full proceess to be able to use an HDP cluster includes the following steps:

 * **Cloudbreak Deployer Installation**: You need to install Cloudbreak Deployer which is a small cli tool called `cbd`. It will help you to deploy the CloudBreak Application consisting several Docker containers. You have finished this step if you can issue `cbd --version`.
 * **CloudBreak Deployment**: Once you have installed Cloudbreak Deployer (cbd), it will start up several Docker containers: CloudBreak API, CloudBreak UI (called Uluwatu), Identity Server, and supporting databases. You have finished this step, if you are able to login in your browser to Cloudbreak UI (Uluwatu).
 * **HDP Cluster Provisioning**: To be able to provision a HDP cluster, you will use the browser, to:
    * Create Credentials: You give access to Cloudbreak, to act on behalf of you, and start resources on the cloud provider.
    * Create Resources: Optionally you can define infrastructure parameters, such as, instance type, memory size, disk type/size, network...
    * Blueprint configuration: You can choose which Ambari Blueprint you want to use (or upload a custom one) and assign hostgroups to resource types (created in the previous step) 
    * Create Cluster: You define the region, where you want to create the HDP cluster. Once Cloudbreak recognize that Ambari Server is up and running, it posts the configured blueprint to it, which triggers a cluster wide HDP component installation process.

## Installation

Currently only **Linux** and **OSX** 64 bit binaries are released for Cloudbreak Deployer. For anything else we can create a special Docker container - please contact us. The deployment itself needs only **Docker 1.7.0** or later. You can install the Cloudbreak installation anywhere (on-prem or cloud VMs), however we suggest to installed it as close to the desired HDP clusters as possible. For further information check the **Provider** section of the documentation.

### On-prem installation

For on premise installations of the Cloudbreak application please follow the [link](onprem.md)

## Setup

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration and launching clusters using that provider.

### AWS

You can follow the AWS provider specific documentation using this [link](aws.md)

### Azure

You can follow the Azure provider specific documentation using this [link](azure.md)

### GCP

You can follow the GCP provider specific documentation using this [link](gcp.md)

### OpenStack

You can follow the OpenStack provider specific documentation using this [link](openstack.md)


## Release notes - 1.1.0

| Components    | GA            | Tech preview  |
| ------------- |:-------------:| -----:|
| AWS   | yes |
| Azure ARM   | yes      |    |
| Azure ARM   | yes      |    |
| GCP  | yes      |    |
| OpenStack Juno   |       | yes   |
| SPI interface   |       | yes   |
| CLI/shell  |   yes    |    |
| Recipes  |       | yes   |
| Kerberos   |       | yes   |
