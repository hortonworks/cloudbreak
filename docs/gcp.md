#GCP deployment

## Preprovision configurations

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

>If you enabled every API then you have to wait about **10 minutes** for the provider.

When creating GCP credentials in Cloudbreak you will have to provide the email address of the Service Account and the project ID (from Google Developers Console - Projects) where the service account is created. You'll also have to upload the generated P12 file and provide an OpenSSH formatted public key that will be used as an SSH key.

## Start Cloudbreak

You already have a cloudbreak-deployer on the machine now we have to start Cloudbreak.

```
mkdir -p cloudbreak-deployer
cd cloudbreak-deployer
```

## Initialize Profile

First initialize your directory by creating a `Profile` file:

```
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - the only required
configuration is the `PUBLIC_IP`. This IP will be used to access the Cloudbreak UI
(called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

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
>You can check the logs when the application is ready. It is about 30 seconds.

Once Cloudbreak is up and running you can launch clusters in two different ways. You can use the [Cloudbreak UI](gcp_cb_ui.md) or use the [Cloudbreak shell](gcp_cb_shell.md).
