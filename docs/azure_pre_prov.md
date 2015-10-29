# Provision prerequisites

Note that we use the new [Azure ARM](https://azure.microsoft.com/en-us/documentation/articles/resource-group-overview/) in order to launch clusters. In order to work we need to create an Active Directory application with the configured name and password and adds the permissions that are needed to call the Azure Resource Manager API. Cloudbreak deployer automates all this for you.

## Generate a new SSH key

All the instances created by Cloudbreak are configured to allow key-based SSH,
so you'll need to provide an SSH public key that can be used later to SSH onto the instances in the clusters you'll create with Cloudbreak.
You can use one of your existing keys or you can generate a new one.

To generate a new SSH keypair:

```
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"
# Creates a new ssh key, using the provided email as a label
# Generating public/private rsa key pair.
```

```
# Enter file in which to save the key (/Users/you/.ssh/id_rsa): [Press enter]
You'll be asked to enter a passphrase, but you can leave it empty.

# Enter passphrase (empty for no passphrase): [Type a passphrase]
# Enter same passphrase again: [Type passphrase again]
```

After you enter a passphrase the keypair is generated. The output should look something like below.
```
# Your identification has been saved in /Users/you/.ssh/id_rsa.
# Your public key has been saved in /Users/you/.ssh/id_rsa.pub.
# The key fingerprint is:
# 01:0f:f4:3b:ca:85:sd:17:sd:7d:sd:68:9d:sd:a2:sd your_email@example.com
```

Later you'll need to pass the `.pub` file's contents to Cloudbreak and use the private part to SSH to the instances.

## Azure access setup

If you do not have an Active directory user then you have to configure it before deploying a cluster with Cloudbreak.

1. Go to `manage.windowsazure.com` > `Active Directory`
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/azure1.png)

2. You can configure your AD users on `Your active directory` > `Users` menu
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/azure2.png)

3. Here you can add the new user to AD. Simply click on `Add User` on the bottom of the page
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/azure3.png)

4. Type the new user name into the box
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/azure4.png)

5. You will see the new user in the list. You have got a temporary password so you have to change it before you start using the new user.
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/azure5.png)

6. After you add the user to the AD you need to add your AD user to the `manage.windowsazure.com` > `Settings` > `Administrators`
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/azure6.png)

7. Here you can add the new user to Administrators. Simply click on `Add` on the bottom of the page
![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/azure7.png)

##Azure application setup with Cloudbreak Deployer

In order for Cloudbreak to be able to launch clusters on Azure on your behalf you need to set up your **Azure ARM application**. We have automated the Azure configurations in the Cloudbreak Deployer (CBD). After the CBD has installed, simply run the following command:

```
cbd azure configure-arm --app_name myapp --app_password password123 --subscription_id 1234-abcd-efgh-1234
```
*Options:*

**--app_name**: Your application name. Default is *app*.

**--app_password**: Your application password. Default is *password*.

**--subscription_id**: Your Azure subscription ID.

**--username**: Your Azure username.

**--password**: Your Azure password.

The command first creates an Active Directory application with the configured name and password and adds the permissions that are needed to call the Azure Resource Manager API.
Please use the output of the command when you creating your Azure credential in Cloudbreak.
The output of the command something like this:

```
Subscription ID: sdf324-26b3-sdf234-ad10-234dfsdfsd
App ID: 234sdf-c469-sdf234-9062-dsf324
Password: password123
App Owner Tenant ID: sdwerwe1-d98e-dsf12-dsf123-df123232
```

## Filesystem configuration

When starting a cluster with Cloudbreak on Azure, the default filesystem is “Windows Azure Blob Storage with DASH”. Hadoop has built-in support for the [WASB filesystem](https://hadoop.apache.org/docs/current/hadoop-azure/index.html) so it can be used easily as HDFS instead of disks.

### Disks and blob storage

In Azure every data disk attached to a virtual machine [is stored](https://azure.microsoft.com/en-us/documentation/articles/virtual-machines-disks-vhds/) as a virtual hard disk (VHD) in a page blob inside an Azure storage account. Because these are not local disks and the operations must be done on the VHD files it causes degraded performance when used as HDFS.
When WASB is used as a Hadoop filesystem the files are full-value blobs in a storage account. It means better performance compared to the data disks and the WASB filesystem can be configured very easily but Azure storage accounts have their own [limitations](https://azure.microsoft.com/en-us/documentation/articles/azure-subscription-service-limits/#storage-limits) as well. There is a space limitation for TB per storage account (500 TB) as well but the real bottleneck is the total request rate that is only 20000 IOPS where Azure will start to throw errors when trying to do an I/O operation.
To bypass those limits Microsoft created a small service called [DASH](https://github.com/MicrosoftDX/Dash). DASH itself is a service that imitates the API of the Azure Blob Storage API and it can be deployed as a Microsoft Azure Cloud Service. Because its API is the same as the standard blob storage API it can be used *almost* in the same way as the default WASB filesystem from a Hadoop deployment.
DASH works by sharding the storage access across multiple storage accounts. It can be configured to distribute storage account load to at most 15 **scaleout** storage accounts. It needs one more **namespace** storage account where it keeps track of where the data is stored.
When configuring a WASB filesystem with Hadoop, the only required config entries are the ones where the access details are described. To access a storage account Azure generates an access key that is displayed on the Azure portal or can be queried through the API while the account name is the name of the storage account itself. A DASH service has a similar account name and key, those can be configured in the configuration file while deploying the cloud service.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/dash.png)

### Deploying a DASH service with Cloudbreak deployer

We have automated the deployment of a DASH service in cloudbreak-deployer. After cbd is installed, simply run the following command to deploy a DASH cloud service with 5 scale out storage accounts:
```
cbd azure deploy-dash --accounts 5 --prefix dash --location "West Europe" --instances 3
```

The command first creates the namespace account and the scaleout storage accounts, builds the *.cscfg* configuration file based on the created storage account names and keys, generates an Account Name and an Account Key for the DASH service and finally deploys the cloud service package file to a new cloud service.

The WASB filesystem configured with DASH can be used as a data lake - when multiple clusters are deployed with the same DASH filesystem configuration the same data can be accessed from all the clusters, but every cluster can have a different service configured as well. In that case deploy as many DASH services with cbd as clusters with Cloudbreak and configure them accordingly.

## Next steps

After these prerequisites are done you can move on to create clusters on the [UI](azure_cb_ui.md) or with the [Shell](azure_cb_shell.md).
