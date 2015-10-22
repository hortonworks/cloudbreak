You can log into the Cloudbreak application at http://PUBLIC_IP:3000.

The main goal of the Cloudbreak UI is to easily create clusters on your own cloud provider account.
This description details the GCP setup - if you'd like to use a different cloud provider check out its manual.

This document explains the four steps that need to be followed to create Cloudbreak clusters from the UI:

- connect your GCP account with Cloudbreak
- create some template resources on the UI that describe the infrastructure of your clusters
- create a blueprint that describes the HDP services in your clusters and add some recipes for customization
- launch the cluster itself based on these template resources


## Manage cloud credentials

You can now log into the Cloudbreak application at http://PUBLIC_IP:3000. Once logged in go to **Manage credentials**. Using manage credentials will  link your cloud account with the Cloudbreak account.

`Name:` name of your credential

`Description:` short description of your linked credential

`Project Id:` your GCP Project id - see Accounts

`Service Account Email Address:` your GCP service account mail address - see Accounts

`Service Account private (p12) key:` your GCP service account generated private key - see Accounts

`SSH public key:` the SSH public key in OpenSSH format that's private keypair can be used to log into the launched instances later

`Public in account:` share it with others in the account

The ssh username is **cloudbreak**.

## Infrastructure templates

After your GCP account is linked to Cloudbreak you can start creating templates that describe your clusters' infrastructure:

- resources
- networks
- security groups

When you create a template, Cloudbreak *doesn't make any requests* to GCP.
Resources are only created on GCP after the `Create cluster` button is pushed.
These templates are saved to Cloudbreak's database and can be reused with multiple clusters to describe the infrastructure.

**Manage resources**

Using manage resources you can create infrastructure templates. Templates describes the infrastructure where the HDP cluster will be provisioned. We support heterogenous clusters - this means that one cluster can be built by combining different templates.

`Name:` name of your template

`Description:` short description of your template

`Instance type:` the Amazon instance type to be used - we suggest to use at least small or medium instances

`Volume type:` option to choose are SSD, regular HDD (both EBS) or Ephemeral

`Attached volumes per instance:` the number of disks to be attached

`Volume size (GB):` the size of the attached disks (in GB)

`Public in account:` share it with others in the account

**Manage blueprints**

Blueprints are your declarative definition of a Hadoop cluster.

`Name:` name of your blueprint

`Description:` short description of your blueprint

`Source URL:` you can add a blueprint by pointing to a URL. As an example you can use this [blueprint](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/core/src/main/resources/defaults/blueprints/multi-node-hdfs-yarn.bp).

`Manual copy:` you can copy paste your blueprint in this text area

`Public in account:` share it with others in the account

**Manage networks**

Manage networks allows you to create or reuse existing networks and configure them.

`Name:` name of the network

`Description:` short description of your network

`Subnet (CIDR):` a subnet in the VPC with CIDR block

`Public in account:` share it with others in the account

**Security groups**

They describe the allowed inbound traffic to the instances in the cluster.
Currently only one security group template can be selected for a Cloudbreak cluster and all the instances have a public IP address so all the instances in the cluster will belong to the same security group.
This may change in a later release.

You can define your own security group by adding all the ports, protocols and CIDR range you'd like to use. 443 needs to be there in every security group otherwise Cloudbreak won't be able to
The rules defined here doesn't need to contain the internal rules, those are automatically added by Cloudbreak to the security group on GCP.

You can also use the two pre-defined security groups in Cloudbreak:

`only-ssh-and-ssl:` all ports are locked down except for SSH and gateway HTTPS (you can't access Hadoop services outside of the VPC):

* SSH (22)
* HTTPS (443)

`all-services-port:` all Hadoop services and SSH/gateway HTTPS are accessible by default:

* SSH (22)
* HTTPS (443)
* Ambari (8080)
* Consul (8500)
* NN (50070)
* RM Web (8088)
* Scheduler (8030RM)
* IPC (8050RM)
* Job history server (19888)
* HBase master (60000)
* HBase master web (60010)
* HBase RS (16020)
* HBase RS info (60030)
* Falcon (15000)
* Storm (8744)
* Hive metastore (9083)
* Hive server (10000)
* Hive server HTTP (10001)
* Accumulo master (9999)
* Accumulo Tserver (9997)
* Atlas (21000)
* KNOX (8443)
* Oozie (11000)
* Spark HS (18080)
* NM Web (8042)
* Zeppelin WebSocket (9996)
* Zeppelin UI (9995)
* Kibana (3080)
* Elasticsearch (9200)

If `Public in account` is checked all the users belonging to your account will be able to use this security group template to create clusters, but cannot delete or modify it.

Note that the security groups are *not created* on GCP after the `Create Security Group` button is pushed, only after the cluster provisioning starts with the selected security group template.

## Cluster installation

This section describes

**Blueprints**

Blueprints are your declarative definition of a Hadoop cluster. These are the same blueprints that are [used by Ambari](https://cwiki.apache.org/confluence/display/AMBARI/Blueprints).

You can use the 3 default blueprints pre-defined in Cloudbreak or you can create your own.
Blueprints can be added from an URL or the whole JSON can be copied to the `Manual copy` field.

The hostgroups added in the JSON will be mapped to a set of instances when starting the cluster and the services and components defined in the hostgroup will be installed on the corresponding nodes.
It is not necessary to define all the configuration fields in the blueprints - if a configuration is missing, Ambari will fill that with a default value.
The configurations defined in the blueprint can also be modified later from the Ambari UI.

If `Public in account` is checked all the users belonging to your account will be able to use this blueprint to create clusters, but cannot delete or modify it.

A blueprint can be exported from a running Ambari cluster that can be reused in Cloudbreak with slight modifications.
There is no automatic way to modify an exported blueprint and make it instantly usable in Cloudbreak, the modifications have to be done manually.
When the blueprint is exported some configurations will have for example hardcoded domain names, or memory configurations that won't be applicable to the Cloudbreak cluster.

**Cluster customization**

Sometimes it can be useful to define some custom scripts that run during cluster creation and add some additional functionality.
For example it can be a service you'd like to install but it's not supported by Ambari or some script that automatically downloads some data to the necessary nodes.
The most notable example is Ranger setup: it has a prerequisite of a running database when Ranger Admin is installing.
A PostgreSQL database can be easily started and configured with a recipe before the blueprint installation starts.

To learn more about these so called *Recipes*, and to check out the Ranger database recipe, take a look at the [Cluster customization](recipes.md) part of the documentation.


## Cluster deployment

After all the templates are configured you can deploy a new HDP cluster. Start by selecting a previously created credential in the header.
Click on `create cluster`, give it a `Name`, select a `Region` where the cluster infrastructure will be provisioned and select one of the `Networks` and `Security Groups` created earlier.
After you've selected a `Blueprint` as well you should be able to configure the `Template resources` and the number of nodes for all of the hostgroups in the blueprint.

If `Public in account` is checked all the users belonging to your account will be able to see the newly created cluster on the UI, but cannot delete or modify it.

If `Enable security` is checked as well, Cloudbreak will install KDC and the cluster will be Kerberized. See more about it in the [Kerberos](kerberos.md) section of this documentation.

After the `create and start cluster` button is pushed Cloudbreak will start to create resources on your GCP account.
Cloudbreak uses *ARM template* to create the resources - you can check out the resources created by Cloudbreak on the ARM Console under the Resourcegroups page.

**Important!** Always use Cloudbreak to delete the cluster, or if that fails for some reason always try to delete the ARM first.

**Advanced features**

There are some advanced features when deploying a new cluster, these are the following:

`Minimum cluster size:` the provisioning strategy in case of the cloud provider can't allocate all the requested nodes

`Validate blueprint:` feature to validate or not the Ambari blueprint. By default is switched on.

`Consul server count:` the number of Consul servers, by default is 3.

`Ambari Repository config:` you can take the stack RPM's from a custom stack repository

## Next steps

Congrats! Your cluster should now be up and running. To learn more about it we have some [interesting insights](insights.md) about Cloudbreak clusters.

