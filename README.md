Cloudbreak Deployer helps to deploy a Cloudbreak environment into Docker containers.
For recent changes please check the [CHANGELOG.md](https://github.com/sequenceiq/cloudbreak-deployer/blob/master/CHANGELOG.md).

## Requirements

Currently only **Linux** and **OSX** 64 bit binaries are released for Cloudbreak Deployer. For anything else we will create a special Docker container.
The deployment itself needs only **Docker 1.5.0** or later.

## Installation

To install Cloudbreak Deployer, you just have to unzip the platform specific
single binary to your PATH. The one-liner way is:

```
curl https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/install | sh && cbd --version
```

## Usage

**cbd** will generate some config files, and will download supporting binaries. It is
advised that you create a dedicated directory for it:

```
mkdir cloudbreak-deployment
cd cloudbreak-deployment
```

### Initialize Profile
First lets initialize your directory by creating a `Profile`

```
cbd init
```
It will create a `Profile` in the current directory. The only required
configuration is the `PUBLIC_IP`. This ip will be used for Cloudbreak UI
(called Uluwatu). In some case cbd can guess it, if not it will give you a hint.


### Deploy Cloudbreak

To start all the containers run:

```
cbd start
```

For the first it will take more time, as it does additional steps:
- download all the docker images, neded by cloudbreak.
- create **docker-compose.yml**: Full confirguration of containers needed for Cloudbreak deployment.
- create **uaa.yml**: Identity Server configuration. (For example default user/password in the last line)

### Watch the logs

```
cbd logs
```

### Pull Docker images

All Cloudbreak components and the backend database is running inside containers.
The **pull command is optional** but you can run it prior to `cbd start`

```
cbd pull
```

It will take some time, depending on your network connection, so you can grab a cup of coffee.


## Default Credentials

If you check the output of `cbd env` you can see the default principal/credential combination:
- user: **admin@example.com**
- password: **cloudbreak**

These values are generated in the `uaa.yml` end section. To change these values, add 2 lines into your Profile:

```
export UAA_DEFAULT_USER_EMAIL=myself@example.com
export UAA_DEFAULT_USER_PW=demo123
```
and than you need to recreate configs:
```
rm *.yml
cbd generate
```

## Cloud Provider configuration
In order to be able to assume roles on AWS you need to set up your AWS keys in the Profile file:
```
export AWS_ACCESS_KEY_ID=AKIA**************W7SA
export AWS_SECRET_ACCESS_KEY=RWCT4Cs8******************/*skiOkWD
```
If you do not have plan to launch clusters in AWS, then you can safely skip these settings.

For more details regarding accounts please check [Cloudbreak documentation](http://sequenceiq.com/cloudbreak/#accounts).


## Debug

If you want to have more detailed output set the `DEBUG` env variable to non-zero:

```
DEBUG=1 cbd some_command
```

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
