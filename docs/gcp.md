#GCP based installation

We have pre-built a custom GCP image available on VM Depot with all the required tooling and Cloudbreak deployer installed. In order to launch this image on Azure please use the following [image]().

Cloudbreak will already be installed, thus you can follow these steps to launch the application.

## Usage

Once the Cloudbreak deployer is installed it will generate some config files and will download supporting binaries. It is
advised that you create a dedicated directory for it:

```
mkdir cloudbreak-deployment
cd cloudbreak-deployment
```

### Initialize Profile

First initialize your directory by creating a `Profile` file:

```
cbd init
```

It will create a `Profile` file in the current directory. Please edit the file - the only required
configuration is the `PUBLIC_IP`. This IP will be used to access the Cloudbreak UI
(called Uluwatu). In some cases the `cbd` tool tries to guess it, if can't than will give a hint.

### Start Cloudbreak

To start all the containers run:

```
cbd start
```

Launching the first time will take more time as it does some additional steps:

- download all the docker images, needed by Cloudbreak.
- create **docker-compose.yml**: Full configuration of containers needed for the Cloudbreak deployment.
- create **uaa.yml**: Identity Server configuration.

### Watch the logs

```
cbd logs
```

## Default Credentials

The default credentials can be revealed by `cbd login`

These values are used in the `uaa.yml` end section. To change these values, add 2 lines into your Profile:

```
export UAA_DEFAULT_USER_EMAIL=myself@example.com
export UAA_DEFAULT_USER_PW=demo123
```
and than you need to recreate configs:
```
rm *.yml
cbd generate
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


## Manage cloud credentials

You can now log into the Cloudbreak application at http://PUBLIC_IP:3000. Once logged in go to **Manage credentials**. Using manage credentials will  link your cloud account with the Cloudbreak account.

`Name:` name of your credential

`Description:` short description of your linked credential

`Project Id:` your GCP Project id - see Accounts

`Service Account Email Address:` your GCP service account mail address - see Accounts

`Service Account private (p12) key:` your GCP service account generated private key - see Accounts

`SSH public key:` the SSH public key in OpenSSH format that's private keypair can be used to log into the launched instances later

`Public in account:` share it with others in the account

The ssh username is cloudbreak.

##Manage resources

Using manage resources you can create infrastructure templates. Templates describes the infrastructure where the HDP cluster will be provisioned. We support heterogenous clusters - this means that one cluster can be built by combining different templates.

`Name:` name of your template

`Description:` short description of your template

`Instance type:` the Amazon instance type to be used - we suggest to use at least small or medium instances

`Volume type:` option to choose are SSD, regular HDD (both EBS) or Ephemeral

`Attached volumes per instance:` the number of disks to be attached

`Volume size (GB):` the size of the attached disks (in GB)

`Spot price:` option to set a spot price - not mandatory, if specified we will request spot price instances (which might take a while or never be fulfilled by Amazon)

`EBS encryption:` this feature is supported with all EBS volume types (General Purpose (SSD), Provisioned IOPS (SSD), and Magnetic

`Public in account:` share it with others in the account

## Manage blueprints
Blueprints are your declarative definition of a Hadoop cluster.

`Name:` name of your blueprint

`Description:` short description of your blueprint

`Source URL:` you can add a blueprint by pointing to a URL. As an example you can use this [blueprint](https://github.com/sequenceiq/ambari-rest-client/raw/1.6.0/src/main/resources/blueprints/multi-node-hdfs-yarn).

`Manual copy:` you can copy paste your blueprint in this text area

`Public in account:` share it with others in the account

## Manage networks

Manage networks allows you to create or reuse existing networks and configure them.

`Name:` name of the network

`Description:` short description of your network

`Subnet (CIDR):` a subnet in the VPC with CIDR block

`Address prefix (CIDR):` the address space that is used for subnets

`Public in account:` share it with others in the account

## Manage security groups

Security groups allows configuration of traffic/access to the cluster. Currently there are two default groups, and later versions will allow setup of new groups.

`only-ssh-and-ssl:` all ports are locked down (you can't access Hadoop services outside of the VPN) but SSH (22) and HTTPS (443)

`all-services-port:` all Hadoop services + SSH/HTTP are accessible by default: SSH (22) HTTPS (443) 8080 (Ambari) 8500 (Consul) 50070 (NN) 8088 (RM Web) 8030 (RM Scheduler) 8050 (RM IPC) 19888 (Job history server) 60010 (HBase master) 15000 (Falcon) 8744 (Storm) 11000 (Oozie) 18080 (Spark HS) 8042 (NM Web) 9996 (Zeppelin WebSocket) 9995 (Zeppelin UI) 3080 (Kibana) 9200 (Elasticsearch)

## Create a cluster

Using the create cluster functionality Cloudbreak will create a cloud Stack and a Hadoop Cluster. In order to create a cluster you will have to select a credential first.

`Cluster name:` your cluster name

`Region:` the region where the cluster is started

`Network:` the network template

`Security Group:" the security group

`Blueprint:` your Hadoop cluster blueprint. Once the blueprint is selected we parse it and give you the option to select the followings for each **hostgroup**.

`Hostgroup configuration`

  `Group size:` the number of instances to be started

  `Template:` the stack template associated to the hostgroup

`Enable security:` Install KDC and Kerberize the cluster

`Public in account:` share it with others in the account

**Advanced features**:

`Consul server count:` the number of Consul servers (odd number), by default is 3. It varies with the cluster size.

`Minimum cluster size:` the provisioning strategy in case of the cloud provider can't allocate all the requested nodes

`Validate blueprint:` feature to validate or not the Ambari blueprint. By default is switched on.

`Ambari Repository config:` you can take the stack RPM's from a custom stack repository

Once you have launched the cluster creation you can track the progress either on Cloudbreak UI or your cloud provider management UI.
