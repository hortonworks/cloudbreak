##Overview

Cloudbreak is a cloud agnostic Hadoop as a Service API. Abstracts the provisioning and ease management and monitoring of on-demand HDP clusters in different virtual environments. Once it is deployed in your favorite servlet container exposes a REST API allowing to span up Hadoop clusters of arbitrary sizes on your selected cloud provider. Provisioning Hadoop has never been easier.
Cloudbreak is built on the foundation of cloud providers API (Microsoft Azure, Amazon AWS, Google Cloud Platform, OpenStack), Apache Ambari, Docker containers, Swarm and Consul.

For a detailed overview please follow this [link](overview.md)

Cloudbreak has two main components - the [Cloudbreak deployer](http://sequenceiq.com/cloudbreak-deployer) and the [Cloudbreak application](http://sequenceiq.com/cloudbreak). Cloudbreak deployer helps you to deploy the Cloudbreak application automatically in environments with Docker support. Once the Cloudbreak application is deployed you can use it to provision HDP clusters in different cloud environments.

##Technology 

For an architectural overview of the [Cloudbreak deployer](http://sequenceiq.com/cloudbreak-deployer) and the [Cloudbreak application](http://sequenceiq.com/cloudbreak) please follow this [link](technology.md).

##Installation

Currently only **Linux** and **OSX** 64 bit binaries are released for Cloudbreak Deployer. For anything else we can create a special Docker container - please contact us. The deployment itself needs only **Docker 1.7.0** or later. You can install the Cloudbreak installation anywhere (on-prem or cloud VMs), however we suggest to installed it as close to the desired HDP clusters as possible. For further information check the **Provider** section of the documentation.

**On-prem installation**

For on premise installations of the Cloudbreak application please follow the [link](onprem.md)

**AWS based installation**

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration and launching clusters using that provider.

You can follow the AWS provider specific documentation using this [link](aws.md)

**Azure based installation**

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration and launching clusters using that provider.

You can follow the AWS provider specific documentation using this [link](azure.md)

**GCP based installation**

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration and launching clusters using that provider.

You can follow the AWS provider specific documentation using this [link](gcp.md)

**OpenStack based installation**

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration and launching clusters using that provider.

You can follow the AWS provider specific documentation using this [link](openstack.md)

##Misc

**Debug**

If you want to have more detailed output set the `DEBUG` env variable to non-zero:

```
DEBUG=1 cbd some_command
```

**Troubleshoot**

You can also use the `doctor` command to diagnose your environment:

```
cbd doctor
```

**Logs**

For logs use the :

```
cbd logs
```

**Update**

The tool is capable to upgrade itself to a newer version.

```
cbd update
```

**Credits**

This tool, and the PR driven release, is inspired from [glidergun](https://github.com/gliderlabs/glidergun). Actually it
could be a fork of it. The reason it’s not a fork, because we wanted to have our own binary with all modules
built in, so only a single binary is needed.
