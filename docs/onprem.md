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

Create a cloudbreak-deployment directory for the config files and the supporting binaries that will be downloaded by cloudbreak-deployer:

```
mkdir cloudbreak-deployer
cd cloudbreak-deployment
```

## Initialize Profile

First initialize cbd by creating a `Profile` file:

```
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - one of the required configurations is the `PUBLIC_IP`.
This IP will be used to access the Cloudbreak UI (called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

## Generate your Profile

You are done with the initialization of Cloudbreak deployer. The last thing you have to do is to regenerate the configurations in order to take effect.

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

Now that you all pre-requisites for Cloudbreak are in place you can follow with the **cloud provider specific** configuration. Based on the location where you plan to launch HDP clusters select one of the providers documentation and follow the steps from the  **Deployment** section.
