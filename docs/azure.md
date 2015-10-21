# Launch/configure your instance

We have pre-built a custom Azure image available on VM Depot with all the required tooling and Cloudbreak deployer installed. In order to launch this image on Azure please use the following [image]().

Note that we use the new [Azure ARM](https://azure.microsoft.com/en-us/documentation/articles/resource-group-overview/) in order to launch clusters. In order to work we need to create an Active Directory application with the configured name and password and adds the permissions that are needed to call the Azure Resource Manager API. Cloudbreak deployer automates all this for you.

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

#### Azure access setup

If you want to use your Azure subscription then you need an Azure Active directory user.

1. You can configure your AD users on `manage.windowsazure.com` > `Active Directory` > `Your active directory` > `Users` menu
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/docsupdate/docs/images/azure2.png)

2. Now you add the new user to AD. Simply click on `Add User` on the bottom of the page
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/docsupdate/docs/images/azure3.png)

3. Type the new user name in to the box
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/docsupdate/docs/images/azure4.png)

4. You will see the added user in the list
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/docsupdate/docs/images/azure5.png)

5. After you add the user to the AD you need to add to the `manage.windowsazure.com` > `Settings` > `Administrators`
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/docsupdate/docs/images/azure6.png)

6. Now you add the new user to Administrators. Simply click on `Add` on the bottom of the page
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/docsupdate/docs/images/azure7.png)

In order for Cloudbreak to be able to launch clusters on Azure on your behalf you need to set up your Azure ARM application.

```
cbd azure configure-arm --app_name myapp --app_password password123 --subscription_id 1234-abcd-efgh-1234 --username example@company.onmicrosoft.com --password password123
```

Where `--app_password` is your application password (default is password), `--subscription_id` is your Azure subscription ID, `--username` is your Azure username and  `--password` is your Azure password.

#### Filesystem configuration

When starting a cluster with Cloudbreak on Azure, the default filesystem is “Windows Azure blob storage with DASH”. Hadoop has built-in support for the [WASB filesystem](https://hadoop.apache.org/docs/current/hadoop-azure/index.html) so it can be used easily as HDFS instead of disks.

##### Disks and blob storage

In Azure every data disk attached to a virtual machine [is stored](https://azure.microsoft.com/en-us/documentation/articles/virtual-machines-disks-vhds/) as a virtual hard disk (VHD) in a page blob inside an Azure storage account. Because these are not local disks and the operations must be done on the VHD files it causes degraded performance when used as HDFS.
When WASB is used as a Hadoop filesystem the files are full-value blobs in a storage account. It means better performance compared to the data disks and the WASB filesystem can be configured very easily but Azure storage accounts have their own [limitations](https://azure.microsoft.com/en-us/documentation/articles/azure-subscription-service-limits/#storage-limits) as well. There is a space limitation for TB per storage account (500 TB) as well but the real bottleneck is the total request rate that is only 20000 IOPS where Azure will start to throw errors when trying to do an I/O operation.
To bypass those limits Microsoft created a small service called [DASH](https://github.com/MicrosoftDX/Dash). DASH itself is a service that imitates the API of the Azure Blob Storage API and it can be deployed as a Microsoft Azure Cloud Service. Because its API is the same as the standard blob storage API it can be used *almost* in the same way as the default WASB filesystem from a Hadoop deployment.
DASH works by sharding the storage access across multiple storage accounts. It can be configured to distribute storage account load to at most 15 **scaleout** storage accounts. It needs one more **namespace** storage account where it keeps track of where the data is stored.
When configuring a WASB filesystem with Hadoop, the only required config entries are the ones where the access details are described. To access a storage account Azure generates an access key that is displayed on the Azure portal or can be queried through the API while the account name is the name of the storage account itself. A DASH service has a similar account name and key, those can be configured in the configuration file while deploying the cloud service.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/dash.png)

##### Deploying a DASH service with Cloudbreak deployer

We have automated the deployment of a DASH service in cloudbreak-deployer. After cbd is installed, simply run the following command to deploy a DASH cloud service with 5 scale out storage accounts:
```
cbd azure deploy-dash --accounts 5 --prefix dash --location "West Europe" --instances 3
```

The command first creates the namespace account and the scaleout storage accounts, builds the *.cscfg* configuration file based on the created storage account names and keys, generates an Account Name and an Account Key for the DASH service and finally deploys the cloud service package file to a new cloud service.

The WASB filesystem configured with DASH can be used as a data lake - when multiple clusters are deployed with the same DASH filesystem configuration the same data can be accessed from all the clusters, but every cluster can have a different service configured as well. In that case deploy as many DASH services with cbd as clusters with Cloudbreak and configure them accordingly.

#### SMTP configurations

During registration or cluster provisioning Cloudbreak sends emails to the user. In order for email sending to work put these lines into your `Profile` file.

```
export CLOUDBREAK_SMTP_SENDER_USERNAME=
export CLOUDBREAK_SMTP_SENDER_PASSWORD=
export CLOUDBREAK_SMTP_SENDER_HOST=
export CLOUDBREAK_SMTP_SENDER_PORT=
export CLOUDBREAK_SMTP_SENDER_FROM=
```

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

Once Cloudbreak is up and running you can launch clusters in two different ways. You can use the [Cloudbreak UI](azure_cb_ui.md) or use the [Cloudbreak shell](azure_cb_shell.md).
