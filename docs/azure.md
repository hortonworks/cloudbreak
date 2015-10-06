#Azure based installation

We have pre-built a custom Azure image available on VM Depot with all the required tooling and Cloudbreak deployer installed. In order to launch this image on Azure please use the following [image]().

Note that we use the new [Azure ARM](https://azure.microsoft.com/en-us/documentation/articles/resource-group-overview/) in order to launch clusters. In order to work we need to create an Active Directory application with the configured name and password and adds the permissions that are needed to call the Azure Resource Manager API. Cloudbreak deployer automates all this for you.

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

In order for Cloudbreak to be able to launch clusters on Azure on your behalf you need to set up your Azure ARM application.

```
cbd azure configure-arm --app_name myapp --app_password password123 --subscription_id 1234-abcd-efgh-1234 --username example@company.onmicrosoft.com --password password123
```

Where `--app_password` is your application password (default is password), `--subscription_id` is your Azure subscription ID, `--username` is your Azure username and  `--password` is your Azure password.

##Filesystem configuration

When starting a cluster with Cloudbreak on Azure, the default filesystem is “Windows Azure blob storage with DASH”. Hadoop has built-in support for the [wasb filesystem](https://hadoop.apache.org/docs/current/hadoop-azure/index.html) so it can be used easily as HDFS instead of disks.

### Disks and blob storage

In Azure every data disk attached to a virtual machine [is stored](https://azure.microsoft.com/en-us/documentation/articles/virtual-machines-disks-vhds/) as a virtual hard disk (VHD) in a page blob inside an Azure storage account. Because these are not local disks and the operations must be done on the VHD files it causes degraded performance when used as HDFS.
When WASB is used as a Hadoop filesystem the files are full-value blobs in a storage account. It means better performance compared to the data disks and the WASB filesystem can be configured very easily but Azure storage accounts have their own [limitations](https://azure.microsoft.com/en-us/documentation/articles/azure-subscription-service-limits/#storage-limits) as well. There is a space limitation for TB per storage account (500 TB) as well but the real bottleneck is the total request rate that is only 20000 IOPS where Azure will start to throw errors when trying to do an I/O operation.
To bypass those limits Microsoft created a small service called [DASH](https://github.com/MicrosoftDX/Dash). DASH itself is a service that imitates the API of the Azure Blob Storage API and it can be deployed as a Microsoft Azure Cloud Service. Because its API is the same as the standard blob storage API it can be used *almost* in the same way as the default WASB filesystem from a Hadoop deployment.
DASH works by sharding the storage access across multiple storage accounts. It can be configured to distribute storage account load to at most 15 **scaleout** storage accounts. It needs one more **namespace** storage account where it keeps track of where the data is stored.
When configuring a WASB filesystem with Hadoop, the only required config entries are the ones where the access details are described. To access a storage account Azure generates an access key that is displayed on the Azure portal or can be queried through the API while the account name is the name of the storage account itself. A DASH service has a similar account name and key, those can be configured in the configuration file while deploying the cloud service.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/dash.png)

### Deploying a DASH service with Cloudbreak deployer

We have automated the deployment of a DASH service in cloudbreak-deployer. After cbd is installed, simply run the following command to deploy a DASH cloud service with 5 scale out storage accounts:
```
cbd azure deploy-dash --accounts 5 --prefix dash --location "West Europe" --instances 3
```

The command first creates the namespace account and the scaleout storage accounts, builds the *.cscfg* configuration file based on the created storage account names and keys, generates an Account Name and an Account Key for the DASH service and finally deploys the cloud service package file to a new cloud service.

### Configuring DASH from Cloudbreak

When an Azure credential is selected in Cloudbreak the default filesystem is set to "WASB with DASH”. The *Account Name* and *Account Key* displayed by `cbd` must be copied to the corresponding fields.

**Important:** For better performance DASH service and the Cloudbreak cluster must be in the same Azure region.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/dashui.png)

The WASB filesystem configured with DASH can be used as a data lake - when multiple clusters are deployed with the same DASH filesystem configuration the same data can be accessed from all the clusters, but every cluster can have a different service configured as well. In that case deploy as many DASH services with cbd as clusters with Cloudbreak and configure them accordingly.


## Manage cloud credentials

You can now log into the Cloudbreak application at http://PUBLIC_IP:3000. Once logged in go to **Manage credentials**. Using manage credentials will  link your cloud account with the Cloudbreak account.

`Name:` name of your credential

`Description:` short description of your linked credential

`Subscription Id:` your Azure subscription id - see Accounts (Browse all> Subscription)

`Password:` your password which was setted up when you create the AD app

`App ID:` You app Id (Browse all> Subscription> Subscription detail> Users> You application> Properties)

`App Owner Tenant ID:` You Tenant Id (Browse all> Subscription> Subscription detail> Users> You application> Properties)

`SSH certificate:` the SSH public certificate in OpenSSH format that's private keypair can be used to log into the launched instances later (The key generation process is described in the Configuring the Microsoft Azure account section)

The ssh username is **cloudbreak**

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
