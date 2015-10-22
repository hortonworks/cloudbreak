# Launch/configure your instance

We have pre-built a custom AWS AMI image with all the required tooling and Cloudbreak deployer installed. In order to launch this image on AWS please use the following [Cloudformation] link().

Cloudbreak will already be installed, thus you can follow these steps to launch the application.

# Configure Cloudbreak deployer

Enter into the `cloudbreak-deployment folder`.

```
cd ~/cloudbreak-deployment
```

In this folder you will find a `Profile` file.

#### Configure Cloudbreak UI access

Please edit the Profile file - the only mandatory configuration is the `PUBLIC_IP`. This IP will be used to access the Cloudbreak UI
(called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

#### AWS access setup

In order for Cloudbreak to be able to launch clusters on AWS on your behalf you need to set up your AWS keys in the Profile file:

```
export AWS_ACCESS_KEY_ID=AKIA**************W7SA
export AWS_SECRET_ACCESS_KEY=RWCT4Cs8******************/*skiOkWD
```

#### SMTP configurations

During registration or cluster provisioning Cloudbreak sends emails to the user. In order for email sending to work put these lines into your `Profile` file.

```
export CLOUDBREAK_SMTP_SENDER_USERNAME=
export CLOUDBREAK_SMTP_SENDER_PASSWORD=
export CLOUDBREAK_SMTP_SENDER_HOST=
export CLOUDBREAK_SMTP_SENDER_PORT=
export CLOUDBREAK_SMTP_SENDER_FROM=
```

#### Generate an AWS role

One key point is that Cloudbreak **does not** store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc). We work around the concept that Identity and Access Management is fully controlled by the end user. Cloudbreak is purely acting on behalf of the end user - without having access to the user's account. In order to launch clusters on your behalf we need an AWS IAM role - that can be used in the Cloudbreak application as a credential.

```
cbd aws generate-role  - Generates an AWS IAM role for Cloudbreak provisioning on AWS
cbd aws show-role      - Show assumers and policies for an AWS role
cbd aws delete-role    - Deletes an AWS IAM role, removes all inline policies
```

You can check the generated role on your AWS console, under IAM roles.

#### Change default username/Password

The default credentials can be revealed by `cbd login` These values are used in the `uaa.yml` file's end section. To change these values, add 2 lines into your Profile:

```
export UAA_DEFAULT_USER_EMAIL=myself@example.com
export UAA_DEFAULT_USER_PW=demo123
```

#### Regenerate your Profile

You are done with the configuration of Cloudbreak deployer. The last thing you have to do is to regenerate the configurations in order to take effect.

```
rm *.yml
cbd generate
```

#### Verify configs

In order to verify that all configs are OK use the `doctor` command.

```
cbd doctor
```

# Use Cloudbreak 

To start the Cloudbreak application use the following command.

```
cbd start
```

This will start all the Docker containers and initialize the application. Please give a few minutes until all services starts. While the services are starting you can check the logs.

```
cbd logs
```

Once Cloudbreak is up and running you should check out the [prerequisites](aws_pre_prov.md) needed to create AWS clusters with Cloudbreak.
