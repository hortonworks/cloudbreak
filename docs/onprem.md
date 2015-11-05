# Install Cloudbreak deployer

To install Cloudbreak Deployer on your selected environment you have to follow the steps below.

## SELinux, firewalls

Make sure that SELinux is disabled and if there is a firewall installed than allows communication between Docker Containers

## Install Cloudbreak deployer

Install the Cloudbreak deployer and unzip the platform specific single binary to your PATH. The one-liner way is:

```
curl https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/install | sh && cbd --version
```

Once the Cloudbreak deployer is installed, you can start to setup the Cloudbreak application.

## Setup Cloudbreak deployer

Create a cloudbreak-deployment directory for the config files and the supporting binaries that will be downloaded by cloudbreak-deployer:

```
mkdir cloudbreak-deployer
```

## Next steps

Now that you all pre-requisites for Cloudbreak are in place you can follow with the **cloud provider specific** configuration. Based on the location where you plan to launch HDP clusters select one of the providers documentation and follow the steps from the **Deployment** section.

You can find the provider specific documentations here:

* [AWS](aws.md)
* [Azure](azure.md)
* [GCP](gcp.md)
* [OpenStack](openstack.md)