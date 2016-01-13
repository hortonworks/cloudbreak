# Release Notes

The Release Notes summarize and describe changes released in Cloudbreak.


## New Features

This release includes the following new features and improvements:

| Feature | Description |
|----|----|
| Recipes | Ability to script extensions that run before/after cluster installation. See [Recipes](recipes.md) for more information. |
| Cloudbreak Shell | A Command Line Interface (CLI) for interactively managing Cloudbreak. See [Shell](shell.md) for more information. |
| Pre-built Cloud Images | **Technical Preview** Pre-built Cloud images for AWS, GCP and OpenStack that include Cloudbreak Deployer pre-installed and configured.|
| Kerberos | **Technical Preview** Support for enabling Kerberos on the HDP clusters deployed by Cloudbreak. See [Kerberos](kerberos.md) for more information. |
| OpenStack Cloud Provider |  **Technical Preview** Support for OpenStack Juno cloud provider. See [OpenStack](openstack.md) for more information. |
| Cloud Provider SPI | **Technical Preview** Cloudbreak Service Provider Interface (SPI) for pluging-in new providers. See [SPI](spi.md) for more information. |


## Behavioral Changes

This release introduces the following changes in behavior as compared to previous Cloudbreak versions:

| Title | Description |
|----|----|
|UI changes|Cluster creation is based on a step-by-step wizard. |
| Custom Security Groups | Ability to define and create custom security groups and rules.|
| Azure ARM support | With this release we have switched to the new [Azure ARM API](https://azure.microsoft.com/en-us/documentation/articles/resource-group-overview/) aka **Azure API v2**. Using the old API is not supported anymore - users have the option to **terminate only** clusters lunched with the old API. All new clusters are lunched with the new API.|
|WASB support|For clusters launched on Microsoft Azure the default file system in use will be [WASB](http://blogs.msdn.com/b/cindygross/archive/2015/02/04/understanding-wasb-and-hadoop-storage-in-azure.aspx). Users will still have to option to use local HDFS with attached disk but the recommended file system will be WASB. See [Filesystem configuration](azure_pre_prov.md) for more information.|
|DASH support for WASB|When WASB is used as a Hadoop filesystem the files are full-value blobs in a storage account. It means better performance compared to the data disks and the WASB filesystem can be configured very easily but Azure storage accounts have their own [limitations](https://azure.microsoft.com/en-us/documentation/articles/azure-subscription-service-limits/#storage-limits) as well. There is a space limitation for TB per storage account (500 TB) as well but the real bottleneck is the total request rate that is only 20000 IOPS where Azure will start to throw errors when trying to do an I/O operation. To bypass those limits Microsoft created a small service called [DASH](https://github.com/MicrosoftDX/Dash). See [Filesystem configuration](azure_pre_prov.md) for more information.|
|Support for new regions|On AWS we added support for **Frankfurt**. On GCP we added support for **us-east-1**.|
|UAA zones| Updated to UAA 2.7.1 version, which introduced the concept of **zones**. See [Access from custom domains](configuration.md) for more information.|



## Patch Information

TBD

## Known Issues

Cloudbreak has the following known issues, scheduled for resolution in a future release. Please work around the following issues:

| JIRA | Problem | Solution |
|----|----|---|
| X | X | X |


## Fixed Issues

The following sections list selected issues resolved in Cloudbreak 1.1:

| JIRA | Category | Summary |
|----|----|---|
| X |Potential Data Loss | X |
| X |Stability | X |
| X |Security | X |
| X |Upgrade | X |
| X |Usability | X |
| X |Performance | X |
| X |Other | X |
