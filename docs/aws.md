# AWS deployment

We have pre-built custom cloud images with Cloudbreak deployer pre-configured. Following the steps will guide you through the provider specific configuration then launch. If you do not have installed Cloudbreak deployer, you can [install this on premise](onprem.md).

## Configured images

You can find the pre-configured AWS images as:

#### AWS Images


## Setup Cloudbreak deployer

If you already have cloudbreak-deployer installed you can start to setup the Cloudbreak application.

Open the `cloudbreak-deployment` directory:

```
cd cloudbreak-deployment
```

This is the directory of the config files and the supporting binaries that will be downloaded by Cloudbreak deployer.

### Initialize your Profile

First initialize cbd by creating a `Profile` file:

```
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - one of the required configurations is the `PUBLIC_IP`.
This IP will be used to access the Cloudbreak UI (called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

The other required configuration in the `Profile` are the AWS keys belonging to the AWS account used by the Cloudbreak application.
In order for Cloudbreak to be able to launch clusters on AWS on your behalf you need to set your AWS keys in the `Profile` file.
We suggest to use the keys of an *IAM User* here. The IAM User's policies must be configured to have permission to assume roles (`sts:AssumeRole`) on all (`*`) resources.

```
export AWS_ACCESS_KEY_ID=AKIA**************W7SA
export AWS_SECRET_ACCESS_KEY=RWCT4Cs8******************/*skiOkWD
```

You can learn more about the concepts used by Cloudbreak with AWS accounts in the [prerequisites chapter](aws_pre_prov.md) 

### Generate your Profile

You are done with the initialization of Cloudbreak deployer. The last thing you have to do is to regenerate the configurations in order to take effect.

```
rm *.yml
cbd generate
```

### Start Cloudbreak

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

### Next steps

Once Cloudbreak is up and running you should check out the [prerequisites](aws_pre_prov.md) needed to create AWS clusters with Cloudbreak.

