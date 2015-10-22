You can log into the Cloudbreak application at http://PUBLIC_IP:3000.

The main goal of the Cloudbreak UI is to easily create clusters on your own cloud provider account.
This description details the AWS setup - if you'd like to use a different cloud provider check out its manual.

This document explains the four steps that need to be followed to create Cloudbreak clusters from the UI:

- connect your AWS account with Cloudbreak
- create some template resources on the UI that describe the infrastructure of your clusters
- create a blueprint that describes the HDP services in your clusters and add some recipes for customization
- launch the cluster itself based on these template resources

## Setting up AWS credentials

Cloudbreak works by connecting your AWS account through so called *Credentials*, and then uses these credentials to create resources on your behalf.
The credentials can be configured on the "manage credentials" tab.

Add a `name` and a `description` for the credential, copy your IAM role's Amazon Resource Name (ARN) to the corresponding field (`IAM Role ARN`) and copy your SSH public key to the `SSH public key` field.
To learn more about how to setup the IAM Role on your AWS account check out the [prerequisites](aws_pre_prov.md).

The SSH public key must be in OpenSSH format and it's private keypair can be used later to SSH onto every instance of every cluster you'll create with this credential.
The SSH username for the EC2 instances is **ec2-user**.

There is a last option called `Public in account` - it means that all the users belonging to your account will be able to use this credential to create clusters, but cannot delete or modify it.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/aws-credential.png)

## Infrastructure templates

After your AWS account is linked to Cloudbreak you can start creating templates that describe your clusters' infrastructure:

- resources
- networks
- security groups

When you create a template, Cloudbreak *doesn't make any requests* to AWS.
Resources are only created on AWS after the `Create cluster` button is pushed.
These templates are saved to Cloudbreak's database and can be reused with multiple clusters to describe the infrastructure.

**Resources**

Resources describe the instances of your cluster - the instance type and the attached volumes.
A typical setup is to combine multiple resources in a cluster for the different types of nodes.
For example you may want to attach multiple large disks to the datanodes or have memory optimized instances for Spark nodes.

There are some additional configuration options here:

- Spot price is not mandatory, if specified Cloudbreak will request spot price instances (which might take a while or never be fulfilled by Amazon). This option is *not supported* by the default RedHat images.
- EBS encryption is supported for all volume types. If this option is checked then all the attached disks [will be encrypted](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSEncryption.html) by Amazon using the AWS KMS master keys.
- If `Public in account` is checked all the users belonging to your account will be able to use this resource to create clusters, but cannot delete or modify it.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/aws-resources.png)

**Networks**

Your clusters can be created in their own Virtual Private Cloud (VPC) or in one of your already existing VPCs.
Currently Cloudbreak creates a new subnet in both cases, in a later release it may change.
The subnet's IP range must be defined in the `Subnet (CIDR)` field using the general CIDR notation.

If you don't want to use your already existing VPC, you can use the default network (`default-aws-network`) for all your clusters.
It will create a new VPC with a `10.0.0.0/16` subnet every time a cluster is created.

If you'd like to deploy a cluster to an already existing VPC you'll have to create a new network template where you configure the identifier of your VPC and the internet gateway (IGW) that's attached to the VPC.
In this case you'll have to create a different network template for every one of your clusters because the Subnet CIDR cannot overlap an already existing subnet in the VPC.
For example you can create 3 different clusters with 3 different network templates for the subnets `10.0.0.0/24`, `10.0.1.0/24`, `10.0.2.0/24` but with the same VPC and IGW identifiers.
Please make sure that the subnet you define here doesn't overlap with any of your already deployed subnets in the VPC because the validation only happens after the cluster creation starts.

If `Public in account` is checked all the users belonging to your account will be able to use this network template to create clusters, but cannot delete or modify it.

Note that the VPCs, IGWs and/or subnets are *not created* on AWS after the `Create Network` button is pushed, only after the cluster provisioning starts with the selected network template.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/aws-network.png)

**Security groups**

Security group templates are very similar to the security groups on the AWS Console.
They describe the allowed inbound traffic to the instances in the cluster.
Currently only one security group template can be selected for a Cloudbreak cluster and all the instances have a public IP address so all the instances in the cluster will belong to the same security group.
This may change in a later release.

You can define your own security group by adding all the ports, protocols and CIDR range you'd like to use. 443 needs to be there in every security group otherwise Cloudbreak won't be able to
The rules defined here doesn't need to contain the internal rules, those are automatically added by Cloudbreak to the security group on AWS.

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

Note that the security groups are *not created* on AWS after the `Create Security Group` button is pushed, only after the cluster provisioning starts with the selected security group template.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/ui-secgroup.png)

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

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/ui-blueprints.png)

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

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak-deployer/docsupdate/docs/images/aws-create-cluster.png)

After the `create and start cluster` button is pushed Cloudbreak will start to create resources on your AWS account.
Cloudbreak uses *CloudFormation* to create the resources - you can check out the resources created by Cloudbreak on the AWS Console under the CloudFormation page.

**Important!** Always use Cloudbreak to delete the cluster, or if that fails for some reason always try to delete the CloudFormation stack first.
Instances are started in an Auto Scaling Group so they may be restarted if you terminate an instance manually!

**Advanced features**

There are some advanced features when deploying a new cluster, these are the following:

`Availability Zone`: You can restrict the instances to a specific availability zone. It may be useful if you're using reserved instances.

`Dedicated instances:` Use [dedicated instances](https://aws.amazon.com/ec2/purchasing-options/dedicated-instances/) on EC2

`Minimum cluster size:` the provisioning strategy in case of the cloud provider can't allocate all the requested nodes

`Validate blueprint:` feature to validate or not the Ambari blueprint. By default is switched on.

`Consul server count:` the number of Consul servers, by default is 3.

`Ambari Repository config:` you can take the stack RPM's from a custom stack repository
