# Launch/configure your instance

To install Cloudbreak Deployer on your selected environment you have to follow the steps below.

#### SELinux, firewalls

Make sure that SELinux is disabled and if there is a firewall installed than allows communication between Docker Containers

#### Install Cloudbreak deployer

Install the Cloudbreak deployer and unzip the platform specific single binary to your PATH. The one-liner way is:

```
curl https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/install | sh && cbd --version
```

Once the Cloudbreak deployer is installed it will generate some config files and will download supporting binaries. It is
advised that you create a dedicated directory for it:

```
mkdir cloudbreak-deployment
cd cloudbreak-deployment
```

All Cloudbreak components and the backend database is running inside containers.
The **pull command is optional** but you can run it prior to `cbd start`

```
cbd pull
```
#### Initialize Profile

First initialize your directory by creating a `Profile` file:

```
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - the only required
configuration is the `PUBLIC_IP`. This IP will be used to access the Cloudbreak UI
(called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

#### Pull Docker images

All Cloudbreak components and the backend database is running inside containers.
The **pull command is optional** but you can run it prior to `cbd start`

```
cbd pull
```

It will take some time - depending on your network connection - so you can grab a cup of coffee.

# Configure Cloudbreak deployer

Now that you all pre-requisites for Cloudbreak are in place you can follow with the **cloud provider specific** configuration. Based on the location where you plan to launch HDP clusters select one of the providers documentation and follow the steps from the  **Configure Cloudbreak deployer** section.

# Use Cloudbreak

Now that you are ready to provision clusters you can follow with the **cloud provider specific** documentation. Based on the location where you plan to launch HDP clusters select one of the providers documentation and follow the steps from the  **Use Cloudbreak** section.
