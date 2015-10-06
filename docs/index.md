Cloudbreak is a cloud agnostic Hadoop as a Service API. Abstracts the provisioning and ease management and monitoring of on-demand HDP clusters in different virtual environments. For recent changes please check the [changelog](http://sequenceiq.com/cloudbreak-deployer/latest/changelog/).

Cloudbreak has two main components - the [Cloudbreak deployer](http://sequenceiq.com/cloudbreak-deployer) and the [Cloudbreak application](http://sequenceiq.com/cloudbreak). Cloudbreak deployer helps you to deploy the Cloudbreak application automatically in environments with Docker support. Once the Cloudbreak application is deployed you can use it to provision HDP clusters in different cloud environments.

## Requirements

Currently only **Linux** and **OSX** 64 bit binaries are released for Cloudbreak Deployer. For anything else we can create a special Docker container. The deployment itself needs only **Docker 1.7.0** or later.
Your firewall must be configured to let Docker containers talk to each other and have SELinux disabled. *This is especially important on CentOS 7 where default firewall configuration blocks connections by default.* Should you intend to deploy the Cloudbreak application alongside your cluster in your favorite cloud environment you can use our CentOS 7 based default cloud images - that comes with CBD (Cloudbreak deployer) and the required Docker containers pre-installed.

##On-prem installation

For on premise installations of the Cloudbreak application please open the [Onprem submenu](onprem.md)

##AWS based installation

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration and launching clusters using that provider.

You can follow the AWS provider specific documentation using this [link](aws.md)

##Azure based installation

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration and launching clusters using that provider.

You can follow the AWS provider specific documentation using this [link](azure.md)

##GCP based installation

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration and launching clusters using that provider.

You can follow the AWS provider specific documentation using this [link](gcp.md)

##OpenStack based installation

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration and launching clusters using that provider.

You can follow the AWS provider specific documentation using this [link](openstack.md)

## Debug

If you want to have more detailed output set the `DEBUG` env variable to non-zero:

```
DEBUG=1 cbd some_command
```

## Troubleshooting

You can also use the `doctor` command to diagnose your environment:

```
cbd doctor
```

## Update

The tool is capable to upgrade itself to a newer version.

```
cbd update
```

## Credits

This tool, and the PR driven release, is inspired from [glidergun](https://github.com/gliderlabs/glidergun). Actually it
could be a fork of it. The reason it’s not a fork, because we wanted to have our own binary with all modules
built in, so only a single binary is needed.
