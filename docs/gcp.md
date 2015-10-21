#GCP deployment

GCP image is available in our storage with all the required tools and installed Cloudbreak Deployer. In order to launch this image on GCP please use the following [image](http://storage.googleapis.com/sequenceiqimage/cb-centos71-amb210-2015-08-24-docker18.tar.gz).

Cloudbreak will already be installed, thus you can follow these steps to launch the application.

## Installation

Once the Cloudbreak deployer is installed it will generate some config files and will download supporting binaries. It is
advised that you create a dedicated directory for it:

```
mkdir cloudbreak-deployment
cd cloudbreak-deployment
curl https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/master/install | sh && cbd --version
```

## Initialize Profile

First initialize your directory by creating a `Profile` file:

```
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - the only required
configuration is the `PUBLIC_IP`. This IP will be used to access the Cloudbreak UI
(called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

### Change default username/Password

The default credentials can be revealed by `cbd login` These values are used in the `uaa.yml` file's end section. To change these values, add 2 lines into your Profile:

```
export UAA_DEFAULT_USER_EMAIL=myself@example.com
export UAA_DEFAULT_USER_PW=demo123
```

### Regenerate your Profile

There is available a "cbd regenerate" command for this.

```
cbd regenerate
```

### Verify configurations

In order to verify that all configs are OK use the `doctor` command.

```
cbd doctor
```

### Pull Docker images

All Cloudbreak components and the backend database is running inside containers. The pull command is optional but you can run it prior to cbd start

```
cbd pull
```

## Use Cloudbreak

To start the Cloudbreak application use the following command.

```
cbd start
```

Launching the first time will take more time as it does some additional steps:

- download all the docker images, needed by Cloudbreak.
- create **docker-compose.yml**: Full configuration of containers needed for the Cloudbreak deployment.
- create **uaa.yml**: Identity Server configuration.

This will start all the Docker containers and initialize the application. Please give a few minutes until all services starts. While the services are starting you can check the logs.

```
cbd logs
```

## Provider specific configurations

Follow the [instructions](https://cloud.google.com/storage/docs/authentication#service_accounts) in Google Cloud's documentation to create a `Service account` and `Generate a new P12 key`.

Make sure that at API level (**APIs and auth** menu) you have enabled:

* Google Compute Engine
* Google Compute Engine Instance Group Manager API
* Google Compute Engine Instance Groups API
* BigQuery API
* Google Cloud Deployment Manager API
* Google Cloud DNS API
* Google Cloud SQL
* Google Cloud Storage
* Google Cloud Storage JSON API

When creating GCP credentials in Cloudbreak you will have to provide the email address of the Service Account and the project ID (from Google Developers Console - Projects) where the service account is created. You'll also have to upload the generated P12 file and provide an OpenSSH formatted public key that will be used as an SSH key.

Once Cloudbreak is up and running you can launch clusters in two different ways. You can use the [Cloudbreak UI](gcp_cb_ui.md) or use the [Cloudbreak shell](gcp_cb_shell.md).
