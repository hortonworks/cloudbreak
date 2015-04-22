<!--main.md-->

Cloudbreak
================

*Cloudbreak is a powerful left surf that breaks over a coral reef, a mile off southwest the island of Tavarua, Fiji.*

*Cloudbreak is a cloud agnostic Hadoop as a Service API. Abstracts the provisioning and ease management and monitoring of on-demand clusters.*

Cloudbreak [API documentation](https://cloudbreak-api.sequenceiq.com/api/index.html).

<!--main.md-->

<!--overview.md-->

##Overview

Cloudbreak is a RESTful Hadoop as a Service API. Once it is deployed in your favorite servlet container exposes a REST API allowing to span up Hadoop clusters of arbitrary sizes on your selected cloud provider. Provisioning Hadoop has never been easier.
Cloudbreak is built on the foundation of cloud providers API (Microsoft Azure, Amazon AWS, Google Cloud Platform, OpenStack), Apache Ambari, Docker containers, Swarm and Consul.

##Benefits

###Secure
Supports basic, token based and OAuth2 authentication model. The cluster is provisioned in a logically isolated network (Virtual Private Cloud) of your favorite cloud provider.
Cloudbreak does not store or manage your cloud credentials - it is the end user's responsibility to link the Cloudbreak user with her/his cloud account.

###Elastic
Using Cloudbreak API you can provision an arbitrary number of Hadoop nodes - the API does the hard work for you, and span up the infrastructure, configure the network and the selected Hadoop components and services without any user interaction.
POST once and use it anytime after.

###Scalable
As your workload changes, the API allows you to add or remove nodes on the fly. Cloudbreak does the hard work of reconfiguring the infrastructure, provision or decommission Hadoop nodes and let the cluster be continuously operational.
Once provisioned, new nodes will take up the load and increase the cluster throughput.

###Declarative Hadoop clusters
We support declarative Hadoop cluster creation - using blueprints. Blueprints are a declarative definition of your stack, the component/services layout and the configurations to materialize a Hadoop cluster instance.

###Flexible
You have the option to choose your favorite cloud provider and their different pricing models. The API translates the calls towards different vendors. Nevertheless you integrate and use one common API, there is no need to rewrite your code when changing between cloud providers.

<!--overview.md-->

<!--howitworks.md-->

##How it works?

Cloudbreak launches on-demand Hadoop clusters on your favorite cloud provider in minutes. We have introduced 4 main notions - the core building blocks of the REST API.

###Templates

A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion.
Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this one Hadoop cluster can be built by combining different templates.

A template can be used repeatedly to create identical copies of the same stack (or to use as a foundation to start a new stack).

The infrastructure specific configuration is available under the Cloudbreak [resources](https://github.com/sequenceiq/cloudbreak/blob/master/src/main/resources/templates).
As an example for Amazon EC2, we use [AWS Cloudformation](http://aws.amazon.com/cloudformation/) to define the cloud infrastructure .

For further information please visit our [API documentation](https://cloudbreak-api.sequenceiq.com/api/index.html#/templates).

###Stacks

Stacks are template `instances` - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.

For further information please visit our [API documentation](https://cloudbreak-api.sequenceiq.com/api/index.html#/stack).

###Blueprints

Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.

We have a few default blueprints available from multi node blueprints to lambda architectures.

For further information please visit our [API documentation](https://cloudbreak-api.sequenceiq.com/api/index.html#/blueprints).

###Cluster

Clusters are materialised Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack.
Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.

For further information please visit our [API documentation](https://cloudbreak-api.sequenceiq.com/api/index.html#/cluster).

<!--howitworks.md-->

<!--technologies.md-->

##Technology

Cloudbreak is built on the foundation of cloud providers APIs, Apache Ambari, Docker containers, Swarm and Consul.

###Apache Ambari

The Apache Ambari project is aimed at making Hadoop management simpler by developing software for provisioning, managing, and monitoring Apache Hadoop clusters. Ambari provides an intuitive, easy-to-use Hadoop management web UI backed by its RESTful APIs.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/ambari-overview.png)

Ambari enables System Administrators to:

1. Provision a Hadoop Cluster
  * Ambari provides a step-by-step wizard for installing Hadoop services across any number of hosts.
  * Ambari handles configuration of Hadoop services for the cluster.

2. Manage a Hadoop Cluster
  * Ambari provides central management for starting, stopping, and reconfiguring Hadoop services across the entire cluster.

3. Monitor a Hadoop Cluster
  * Ambari provides a dashboard for monitoring health and status of the Hadoop cluster.
  * Ambari leverages Ganglia for metrics collection.
  * Ambari leverages Nagios for system alerting and will send emails when your attention is needed (e.g. a node goes down, remaining disk space is low, etc).

Ambari enables to integrate Hadoop provisioning, management and monitoring capabilities into applications with the Ambari REST APIs.
Ambari Blueprints are a declarative definition of a cluster. With a Blueprint, you can specify a Stack, the Component layout and the Configurations to materialise a Hadoop cluster instance (via a REST API) without having to use the Ambari Cluster Install Wizard.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/ambari-create-cluster.png)

###Docker

Docker is an open platform for developers and sysadmins to build, ship, and run distributed applications. Consisting of Docker Engine, a portable, lightweight runtime and packaging tool, and Docker Hub, a cloud service for sharing applications and automating workflows, Docker enables apps to be quickly assembled from components and eliminates the friction between development, QA, and production environments. As a result, IT can ship faster and run the same app, unchanged, on laptops, data center VMs, and any cloud.

The main features of Docker are:

1. Lightweight, portable
2. Build once, run anywhere
3. VM - without the overhead of a VM
  * Each virtualised application includes not only the application and the necessary binaries and libraries, but also an entire guest operating system
  * The Docker Engine container comprises just the application and its dependencies. It runs as an isolated process in userspace on the host operating system, sharing the kernel with other containers.
    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/vm.png)

4. Containers are isolated
5. It can be automated and scripted

###Swarm

Docker Swarm is native clustering for Docker. It turns a pool of Docker hosts into a single, virtual host. Swarm serves the standard Docker API.

  * Distributed container orchestration: Allows to remotely orchestrate Docker containers on different hosts
    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/swarm.png)
  * Discovery services: Supports different discovery backends to provide service discovery, as such: token (hosted) and file based, etcd, Consul, Zookeeper.
  * Advanced scheduling: Swarm will schedule containers on hosts based on different filters and strategies

###Consul

Consul it is a tool for discovering and configuring services in your infrastructure. It provides several key features

  * Service Discovery: Clients of Consul can provide a service, such as api or mysql, and other clients can use Consul to discover providers of a given service. Using either DNS or HTTP, applications can easily find the services they depend upon.

  * Health Checking: Consul clients can provide any number of health checks, either associated with a given service ("is the webserver returning 200 OK"), or with the local node ("is memory utilization below 90%"). This information can be used by an operator to monitor cluster health, and it is used by the service discovery components to route traffic away from unhealthy hosts.

  * Key/Value Store: Applications can make use of Consul's hierarchical key/value store for any number of purposes, including dynamic configuration, feature flagging, coordination, leader election, and more. The simple HTTP API makes it easy to use.

  * Multi Datacenter: Consul supports multiple datacenters out of the box. This means users of Consul do not have to worry about building additional layers of abstraction to grow to multiple regions.

    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/consul.png)

<!--technologies.md-->

<!--components.md-->

##Supported components

Ambari supports the concept of stacks and associated services in a stack definition. By leveraging the stack definition, Ambari has a consistent and defined interface to install, manage and monitor a set of services and provides extensibility model for new stacks and services to be introduced.

At high level the supported list of components can be grouped into main categories: Master and Slave - and bundling them together form a Hadoop Service.

| Services    | Components                                                              |
|:----------- |:------------------------------------------------------------------------|
| HDFS		    | DATANODE, HDFS_CLIENT, JOURNALNODE, NAMENODE, SECONDARY_NAMENODE, ZKFC  |
| YARN		    | APP_TIMELINE_SERVER, NODEMANAGER, RESOURCEMANAGER, YARN_CLIENT          |
| MAPREDUCE2	| HISTORYSERVER, MAPREDUCE2_CLIENT                                        |
| GANGLIA		  | GANGLIA_MONITOR, GANGLIA_SERVER                                         |
| HBASE		    | HBASE_CLIENT, HBASE_MASTER, HBASE_REGIONSERVER                          |
| HIVE		    | HIVE_CLIENT, HIVE_METASTORE, HIVE_SERVER, MYSQL_SERVER                  |
| HCATALOG	  | HCAT                                                                    |
| WEBHCAT		  | WEBHCAT_SERVER                                                          |
| OOZIE		    | OOZIE_CLIENT, OOZIE_SERVER                                              |
| PIG		      | PIG                                                                     |
| SQOOP		    | SQOOP                                                                   |
| STORM		    | DRPC_SERVER, NIMBUS, STORM_REST_API, STORM_UI_SERVER, SUPERVISOR        |
| TEZ		      | TEZ_CLIENT                                                              |
| FALCON		  | FALCON_CLIENT, FALCON_SERVER                                            |
| ZOOKEEPER	  | ZOOKEEPER_CLIENT, ZOOKEEPER_SERVER                                      |
| SPARK	  | SPARK_JOBHISTORYSERVER, SPARK_CLIENT                                      |
| RANGER	  | RANGER_USERSYNC, RANGER_ADMIN                                      |
| AMBARI_METRICS	  | AMBARI_METRICS, METRICS_COLLECTOR, METRICS_MONITOR                                      |
| KERBEROS	  | KERBEROS_CLIENT                                      |
| FLUME	  | FLUME_HANDLER                                      |
| KAFKA	  | KAFKA_BROKER                                      |
| KNOX	  | KNOX_GATEWAY                                      |
| NAGIOS	  | NAGIOS_SERVER                                      |


We provide a list of default Hadoop cluster Blueprints for your convenience, however you can always build and use your own Blueprint.

1. Full stack multi node - HDP 2.2 blueprint

This is a complex [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/hdp-multinode-default) which allows you to launch a multi node, fully distributed HDP 2.2 Cluster in the cloud.

It allows you to use the following services: HDFS, YARN, MAPREDUCE2, GANGLIA, HBASE, HIVE, HCATALOG, WEBHCAT, NAGIOS, OOZIE, PIG, SQOOP, STORM, TEZ, FALCON, ZOOKEEPER.

2. Simple multi node blueprint

This is a simple [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/multi-node-hdfs-yarn) which allows you to launch a multi node, fully distributed Hadoop 2.6 Cluster in the cloud.

It allows you to use the following services: HDFS, YARN, MAPREDUCE2, ZOOKEEPER.

3. Custom blueprints

We allow you to build and use your own Blueprint - for further instructions please check the Apache Ambari [documentation](https://cwiki.apache.org/confluence/display/AMBARI/Blueprints).

When you are creating custom Blueprints you can use the components above to build Hadoop services and use them in your on-demand Hadoop cluster.

The default rule for multi node clusters is that there must be at least as many hosts as the number of host groups. Each NOT slave host groups (master, gateway, etc) will be launched with a cardinality of 1 (1 node per master, gateway, hosts, etc.), and all the rest of the nodes are equally distributed among Slave nodes (if there are multiple slave host groups).

<!--components.md-->

<!--accounts.md-->

##Accounts

###Cloudbreak account

First and foremost in order to start launching Hadoop clusters you will need to create a Cloudbreak account.
Cloudbreak supports registration, forgotten and reset password, and login features at API level.
All passwords that are stored or sent are hashed - communication is always over a secure HTTPS channel. When you are deploying your own Cloudbreak instance we strongly suggest to configure an SSL certificate. Users create and launch Hadoop clusters on their own namespace and security context.

Users can be invited under an account by the administrator, and all resources (e.g. templates, blueprints, credentials, clusters) can be shared across account users.

  * Usage explorer: Cloudbreak gives you an up to date overview of cluster usage based on different filtering criteria (start/end date, users, providers, region, etc).

  * Account details: The account details of the user.

  * Manage users: You can invite, active and deactivate users under the account.

###Cloudbreak credentials

Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak **does not** store your Cloud provider account details (such as username, password, keys, private SSL certificates, etc).
We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak *deployer* is purely acting on behalf of the end user - without having access to the user's account.

**How does this work**?

###Configuring the AWS EC2 account

Once you have logged in Cloudbreak you will have to link your AWS account with the Cloudbreak one. In order to do that you will need to configure an IAM Role.
You can do this on the management console, or - if you have aws-cli configured - by running a small script we're providing in the `docs/aws` folder.

####Create IAM role on the console

1. Log in to the AWS management console with the user account you'd like to use with Cloudbreak
2. Go to IAM, select "Roles", click Create New Role
3. Give your role a name.
4. Setup your role access:
  * Select Role for Cross-Account access
    *  Allows IAM users from a 3rd party AWS account to access this account.

      **Account ID:** In case you are using our hosted solution you will need to pass SequenceIQ's account id: 755047402263

      **External ID:** provision-ambari (association link)

    * Custom policy

      Use this policy **[document](https://s3-eu-west-1.amazonaws.com/sequenceiq/iam_role.policy)** to configure the permission to start EC2 instances on the end user's behalf, and use SNS to receive notifications.

####Create IAM role with the create script

1. Download [this script](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/aws/create-iam-role.sh), e.g: `curl -O https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/aws/create-iam-role.sh`
2. Make sure you have the [AWS CLI](http://aws.amazon.com/cli/) installed and on your path.
3. Run `./create-iam-role`
4. Copy the resulting role ARN

Once this is configured, Cloudbreak is ready to launch Hadoop clusters on your behalf. The only thing Cloudbreak requires is the `Role ARN` (Role for Cross-Account access).


###Configuring the Microsoft Azure account

In order to launch Hadoop clusters on the  Microsoft Azure cloud platform you'll need to link your Azure account with Cloudbreak. This can be achieved by creating a new `Azure Credential` in Cloudbreak.

You have to create network manually first in Azure before you start provisoning with Cloudbreak that is a known issue on Azure side.

You'll need an X509 certificate with a 2048-bit RSA keypair.

Generate these artifacts with `openssl` by running the following command, and answering the questions in the command prompt:

```
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout my_azure_private.key -out my_azure_cert.pem
```

The command generates the following files into the directory you ran the command from:

`my_azure_private.key` and `my_azure_cert.pem`

(obviously artifacts can be named after your wish)

Fill the form by providing your Azure `Subscription Id`, and the **content** of the previously generated certificate (my_azure_cert.pem).

_Note:_ Cloudbreak will generate a `JKS` file (stored by the backend) and a `certificate` with the `passphrase`. You will need to upload the generated certificate (that is automatically downloaded to you after the form submission, alternatively you can download it any time from Cloudbreak) to your Azure account:

On your `Azure Management Console`, `Settings` menu, click the `Management Certificates` tab and upload the downloaded `cert` file

The JKS file and certificate will be used to encrypt the communication between Cloudbreak and Azure in both directions.

You are done - from now on Cloudbreak can launch Azure instances on your behalf.

_Note:_ Cloudbreak does not store any login details for these instances - when you create **Templates** you can specify a `password` or `SSH Public key` that can be used to login into the launched instances.

_Note:_ Because Azure does not directly support third party public images Cloudbreak will copy the used image from VM Depot into your storage account. The steps below need to be finished ONCE and only ONCE before any stack is created for every affinity group - **this is an automated step**  - it's only highlighted here as an explanation of why it takes longer for the first time:

_1. Get the VM image - http://vmdepot.msopentech.com/Vhd/Show?vhdId=42480&version=43564_

_2. Copy the VHD blob from above (community images) into your storage account_

_3. Create a VM image from the copied VHD blob._

_This process will take 20 minutes so be patient - but this step will have do be done once and only once._


###Configuring the Google Cloud account

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

<!--accounts.md-->

##Roles

Cloudbreak defines three distinct roles:

1. DEPLOYER
2. ACCOUNT_ADMIN
3. ACCOUNT_USER

###Cloudbreak deployer
This is the `master` role - the user which is created during the deployment process will have this role.

###Account admin
We have introduced the notion of accounts - and with that comes an administrator role. Upon registration a user will become an account administrator.

The extra rights associated with the account admin role are:

* Invite users to join the account
* Share account wide resources (credential, blueprints, templates)
* See resources created by account users
* Monitor clusters started by account users
* Management and reporting tool available

###Account user
An account user is a user who has been invited to join Cloudbreak by an account administrator. Account users activity will show up in the management and reporting tool for account wide statistics - accessible by the account administrator. Apart from common account wide resources, the account users can manage their own private resources.

<!--ui.md-->

##Cloudbreak UI

When we have started to work on Cloudbreak, our main goal was to create an easy to use, cloud and Hadoop distribution agnostic Hadoop as a Service API. Though we always like to automate everything and approach things with a very DevOps mindset, as a side project we have created a UI for Cloudbreak as well.
The goal of the UI is to ease to process and allow you to create a Hadoop cluster on your favorite cloud provider in `one-click`.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/UI-screenshot.png)

The UI is built on the foundation of the Cloudbreak REST API. You can access the UI [here](https://cloudbreak.sequenceiq.com/).

###User registration
While we consider the registration process quite simple, we'd like to explain the notion of **companies**. When a user registers as a `company admin` it means that he will be the administrator of that company - further colleagues will have the opportunity to join the **company** upon being invited by the admin user.

###Manage credentials
Using manage credentials you can link your cloud account with the Cloudbreak account.

**Amazon AWS**

`Name:` name of your credential

`Description:` short description of your linked credential

`Role ARN:` the role string - you can find it at the summary tab of the IAM role

`SSH public key:` an SSH public key in OpenSSH format that's private keypair can be used to log into the launched instances later

`Public in account:` share it with others in the account


**Azure**

`Name:` name of your credential

`Description:` short description of your linked credential

`Subscription Id:` your Azure subscription id - see Accounts

`File password:` your generated JKS file password - see Accounts

`SSH certificate:` the SSH public certificate in OpenSSH format that's private keypair can be used to log into the launched instances later (The key generation process is described in the Configuring the Microsoft Azure account section)


**Google Cloud Platform**

`Name:` name of your credential

`Description:` short description of your linked credential

`Project Id:` your GCP Project id - see Accounts

`Service Account Email Address:` your GCP service account mail address - see Accounts

`Service Account private (p12) key:` your GCP service account generated private key - see Accounts

`SSH public key:` the SSH public key in OpenSSH format that's private keypair can be used to log into the launched instances later

`Public in account:` share it with others in the account


**OpenStack**

`Name:` name of your credential

`Description:` short description of your linked credential

`User:` OpenStack user name

`Password:` OpenStack user's password name

`Tenant Name:` OpenStack tenant's (project) name

`Endpoint:` OpenStack API address endpoint's

`SSH public key:` the SSH public key to be used to log into the launched instances later

`Public in account:` share it with others in the account

###Manage templates
Using manage templates you can create infrastructure templates.

**Amazon AWS**

`Name:` name of your template

`Description:` short description of your template

`Instance type:` the Amazon instance type to be used - we suggest to use at least small or medium instances

`Volume type:` option to choose are SSD, regular HDD (both EBS) or Ephemeral

`Attached volumes per instance:` the number of disks to be attached

`Volume size (GB):` the size of the attached disks (in GB)

`Spot price:` option to set a spot price - not mandatory, if specified we will request spot price instances (which might take a while or never be fulfilled by Amazon)

`EBS encryption:` this feature is supported with all EBS volume types (General Purpose (SSD), Provisioned IOPS (SSD), and Magnetic

`Public in account:` share it with others in the account

**Azure**

`Name:` name of your template

`Description:` short description of your template

`Instance type:` the Azure instance type to be used - we suggest to use at least D2 or D4 instances

`Attached volumes per instance:` the number of disks to be attached

`Volume size (GB):` the size of the attached disks (in GB)

`Public in account:` share it with others in the account

**Google Cloud Platform**

`Name:` name of your template

`Description:` short description of your template

`Instance type:` the Google instance type to be used - we suggest to use at least small or medium instances

`Volume type:` option to choose SSD or regular HDD

`Attached volumes per instance:` the number of disks to be attached

`Volume size (GB):` the size of the attached disks (in GB)

`Public in account:` share it with others in the account

**OpenStack**

`Name:` name of your template

`Description:` short description of your template

`Public Net Id:` the public net id

`Attached volumes per instance:` the number of disks to be attached

`Volume size (GB):` the size of the attached disks (in GB)

`Public in account:` share it with others in the account

###Manage blueprints
Blueprints are your declarative definition of a Hadoop cluster.

`Name:` name of your blueprint

`Description:` short description of your blueprint

`Source URL:` you can add a blueprint by pointing to a URL. As an example you can use this [blueprint](https://github.com/sequenceiq/ambari-rest-client/raw/1.6.0/src/main/resources/blueprints/multi-node-hdfs-yarn).

`Manual copy:` you can copy paste your blueprint in this text area

`Public in account:` share it with others in the account


###Create cluster
Using the create cluster functionality you will create a cloud Stack and a Hadoop Cluster. In order to create a cluster you will have to select a credential first.
_Note: Cloudbreak can maintain multiple cloud credentials (even for the same provider)._

`Cluster name:` your cluster name

`Region`: the region where the cluster is started

`Blueprint:` your Hadoop cluster blueprint

Once the blueprint is selected we parse it and give you the option to select the followings for each **hostgroup**.

`Group size:` the nummber of instances to be started

`Template:` the stack template associated to the hostgroup

`Public in account:` share it with others in the account

Once you have launched the cluster creation you can track the progress either on Cloudbreak UI or your cloud provider management UI.


_Note: Because Azure does not directly support third party public images we will have to copy the used image from VM Depot into your storage account. The steps below need to be finished once and only once before any stack is created for every affinity group:_

_1. Get the VM image - http://vmdepot.msopentech.com/Vhd/Show?vhdId=42480&version=43564_

_2. Copy the VHD blob from above (community images) into your storage account_

_3. Create a VM image from the copied VHD blob._

_This process will take 20 minutes so be patient - but this step will have do be done once and only once._


<!--ui.md-->

<!--addnewcloud.md-->

## Add new cloud providers

Cloudbreak is built from ground up on the idea of being cloud provider agnostic. All the external API's are cloud agnostic, and we have
internally abstracted working with individual cloud providers API's. Nevertheless adding new cloud providers is extremely important for us, thus
in order to speed up the process and linking the new provider API with Cloudbreak we came up with an SDK and a list of responsibilities.
Once these interfaces are implemented, and the different provider's API calls are `translated`, you are ready to go.

Though we are working on a few popular providers to add to Cloudbreak, we'd like to hear your voice as well - your ideas, provider requests or `contribution` is highly appreciated.

### Handling API requests

Connecting a new cloud provider means that the Cloudbreak rest API should handle GET, POST and DELETE requests to a stack resource of the new cloud provider correctly.

- *POST*: Creates the cloud resources (VPC, instances, etc) and properly starts SequenceIQ's Ambari Docker container on the instances.

- *DELETE*: Terminates all instances in the cloud and deletes every other resources

- *GET*: Describes the cloud resources by communicating with the cloud provider.

- *PUT*: Updates the cloud resources by communicating with the cloud provider.

When connecting a new cloud provider, the `CloudPlatform` enum that holds the providers should be extended first. It is used to find the implementations when a request arrives.
The main idea is the same behind every method: deal with the database calls in the controller, then call the correct implementation that communicates with the cloud platform. This enables the connectors to be detached from the repository calls, so they should only deal with the communication with the providers.

After an interface is implemented there is only one task to make it available: put the `@Service` annotation on the class. Spring will take care of the rest by discovering the bean when building the application context and putting it in the list that holds the different implementations (see `AppConfig`).

#### POST
Cloudbreak uses an event-driven flow to provision stacks in the cloud. It gives more freedom to the cloud provider implementations and allows developers to build async flows easily, yet unifies the process and takes off the responsibility from the implementations to manage the lifecycle of a stack entity.
It means that connecting a new provider involves implementing the necessary interfaces, and sending different 'completed' events after a specific step is done.

The flow is presented with the sequence diagrams below.
The first diagram shows how the process is started when a `POST` request is sent to the API, the second one shows the actual provisioning flow's first part, which contains the cloud platform specific services. The final diagram contains the last part of the provision flow that's common for every provider.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/seq_diagram_stack_post.png)

The process starts with the controller layer (`StackController` and `SimpleStackService`) that creates and persists the new stack domain object, then sends a `PROVISION_REQUEST_EVENT`. The whole provision flow runs async from this step, the controller returns the response with the newly created stack's id to the client.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/seq_diagram_provision_flow_1.png?token=1568469__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL21hc3Rlci9kb2NzL2ltYWdlcy9zZXFfZGlhZ3JhbV9wcm92aXNpb25fZmxvd18xLnBuZyIsImV4cGlyZXMiOjE0MDU2OTU0Mzd9--733998eb6ce47cdfa0f788bd9fdbe4b95e870da3)

The diagram's goal is to provide a high level design of the flow, it doesn't contain every little detail, there are some smaller steps and method calls left out, the method parameters are not shown and the class names are often abbreviated.

*Notes:*

- every notification event contains the stack id and the cloud platform

- the stack object is retrieved from the database in every complete handler and passed to the invoked method

- the ProvisionSetup event contains a `Map` that hold keys and values and passed to the Provision step

- the MetadataSetup event contains a `Set` of `CoreMetadataInstance` that's processed by the complete handler

- `ProvisionRH` = `ProvisionRequestHandler`

- `ProvisionSetupCH` = `ProvisionSetupCompleteHandler`

- `ProvisionRH` = `ProvisionRequestHandler`

- `MetadataSetupCH` = `MetadataSetupCompleteHandler`

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/seq_diagram_provision_flow_2.png)

*Notes:*

- Ambari server is not shown on the diagram, but health check is basically a call to the Ambari REST API's health endpoint

- `MetadataSetupCH` = `MetadataSetupCompleteHandler`

- `AmbariRoleAllocationCH` = `AmbariRoleAllocationCompleteHandler`

- `StackCreationSH` = `StackCreationSuccessHandler`

- `ClusterRequestH` = `ClusterRequestHandler`


When adding a new provider, the following **3+1 steps** should be implemented to handle the provisioning successfully. The first three steps are similar: they involve the implementation of different interfaces and they should send `complete` events after their task is done. Sending only events when a task is done enables the implementations themselves to use async processes. For example AWS provisioning uses such an implementation (See `Provisioner` for the details).
The last step is a bit different because it requires the implementation of the user-data bash script that runs on every cloud machine instance after they are started.

- `ProvisionSetup`: This step should create every cloud provider resource that will be used during the provisioning. For example the EC2 implementation uses SNS topics to notify Cloudbreak when an EC2 resource creation is completed. These topics are created (or retrieved) in this step, and the identifiers are sent in the `PROVISION_SETUP_COMPLETE` event as a key-value pair. (see `AwsProvisionSetup`)

- `Provisioner`: The actual cloud resource creation is done here. The `PROVISION_COMPLETE` must be sent after every resource creation is initialised. It must contain the type and id of the created resources. This process can be async itself, e.g.: AWS CloudFormation is able to notify clients through an SNS topic when specific resources are created - the `PROVISION_COMPLETE` event is only sent after this notification arrives. `AwsProvisioner` handles the resource requests and `SnsMessageHandler` handles the notifications coming from Amazon SNS.
There are a few restrictions for the resources to be created: the service must start as many instances as it is specified in the stack object and these instances must be started in a different subnet for every stack. They must also be able to reach the Internet, and a few ports should be open in the subnet. We have also created an [Ansible playbook]() that can be used to create the image.

- `MetadataSetup`: The Cloudbreak instance metadata service is detailed [here](#metadata-service). For this to work, the cloud platform connectors must provide details to Cloudbreak about the instances that were started in a stack. The `CoreInstanceMetaData` (private IP in VPC, public IP and instance identifier) of every instance must be retrieved from the cloud platform and must be sent in a `METADATA_SETUP_COMPLETE` event. (see `AwsMetadataSetup`)

- `user-data script`: Most cloud platforms provide user-data provisioning when starting an instance. It means that a specific script can be handed to the instance and it is run when the instance is initialising. User-data is usually handled by [CloudInit](https://help.ubuntu.com/community/CloudInit) on Ubuntu. This script is responsible for setting up the network configuration on an instance and start the Serf and Dnsmasq based Ambari Docker container with the proper variables.
We have created Cloudbreak's metadata service to be able to use the same script everywhere, but the different cloud platforms can have different characteristics so we left the possibility to write a custom script too. The user data should be saved in `src/main/resources` with the name `[prefix]-init.sh` and its prefix must be specified in the `CloudPlatform` enum.

The rest of the flow in the sequence diagram is common for every cloud provider. Once the instances are started, the docker containers are running and the network configuration is done there is **no other cloud platform specific task** in Cloudbreak.
Waiting for the Ambari server to start requires only the IP address of the server, the rest of the cluster installation is handled by Ambari through our [Ambari rest client](https://github.com/sequenceiq/ambari-rest-client) that is written in Groovy.

#### GET and DELETE
GET and DELETE is much more straightforward to implement than POST. There is an interface called `CloudPlatformConnector` in the `com.sequenceiq.cloudbreak.service.stack.connector` package that must be implemented.
Deleting or describing a resource on a cloud provider is in most cases easy. It usually involves an API call through an SDK where the name or id of the resource must be specified. Cloudbreak stores the ids of the resources in its database to determine which resources belong to a given stack.
The implementation should iterate over these resources and should call the proper API request that deletes it or returns its details.

When a DELETE request arrives, the controller layer finds the requested stack in the database and passes it to the correct cloud platform implementation. There is no return type in this case. In case of a GET request, the implementation should return a `StackDescription` that contains the details of the cloud resources. Later in the controller the basic information from the Cloudbreak database is returned besides the detailed description coming from the `CloudPlatformConnector`.

### Event handling and notifications
Cloudbreak uses events and notifications extensively. Event publishing and subscribing uses the Reactor framework. This is an example to send a `METADATA_SETUP_COMPLETE` event after wiring the `Reactor` bean in the component:
```
@Autowired
private Reactor reactor;
...
reactor.notify(ReactorConfig.METADATA_SETUP_COMPLETE_EVENT, Event.wrap(new MetadataSetupComplete(CloudPlatform.AWS, stack.getId(), coreInstanceMetadata)));
```
If you'd like to learn more about the Reactor framework, Spring has a [good guide](http://spring.io/guides/gs/messaging-reactor/) to start with. You should also check the `ReactorConfig` and `ReactorInitializer` classes in Cloudbreak.

### Metadata service
To be able to use the `sequenceiq/ambari` docker container, instances that are started in the same stack should know about each other. They have to know on which addresses can the other docker containers be reached to be able to join the Serf cluster.
They also have to know if they have an Ambari server *role* or not. This kind of information is provided by the metadata service that is available for every stack on a different unique hash. The metadata address looks like this:
```
http://<METADATA_ADDRESS>/stacks/metadata/<METADATA_HASH>
```
`METADATA_ADDRESS` and `METADATA_HASH` is available in every init-script as variables.

Different cloud platform implementations should send only the `CoreInstanceMetadata` in the `MetadataSetup` step. It is later extended by `MetadataSetupContext` that selects the Ambari server and generates an index and a Docker subnet for every instance. It means that the instances can be completely equal when started, only their metadata differentiates them from each other.

Our reference user-data implementation is `ec2-init.sh`. That script contains the network configurations, the Docker subnet setup and the launch of the Docker container. The script has only one EC2-specific part: it retrieves the current instance's id from the EC2 metadata and uses that to parse the metadata coming from Cloudbreak. This means that this script is highly reusable for other cloud platforms too, the only difference should be the retrieval of the instance id (and other cloud platform specific characteristics, like in case of Azure).

To learn more about the `sequenceiq/ambari` container, how it works with Serf and Dnsmasq you can check the [repo](https://github.com/sequenceiq/docker-ambari) on Github and read the blog posts about the [single-node](http://blog.sequenceiq.com/blog/2014/06/17/ambari-cluster-on-docker/) and [multi-node](http://blog.sequenceiq.com/blog/2014/06/19/multinode-hadoop-cluster-on-docker/) Hadoop clusters on Docker.


### Account management

Every cloud platform requires some kind of credentials when requesting, deleting or describing resources. Cloudbreak supports the management of credentials through its REST API. First a credential should be created and later when a stack is requested, the credential has to be attached. This credential will be used by Cloudbreak for authentication and authorisation to the cloud provider. This is a sample `POST: /stacks` request body with a credential:
```
{
  "nodeCount":12,
  "templateId":"123",
  "name":"sample-stack",
  "credentialId":"123"
}
```

Connecting a new cloud provider requires that the right credentials are passed when communicating with its API. In order to be able to create new type of credentials with Cloudbreak, a new domain object must be created that extends `Credential`, and `SimpleCredentialService` must be extended so it can handle the new `CloudPlatform`.
Cloudbreak offers the clients to create clusters **on their own accounts**. It means that some kind of cross-account access must be implemented, which is provided nicely by Amazon through IAM roles but quite hard to solve in Azure for example, and couldn't be done without some kind of unique solutions.
Different providers use very different kind of credentials, so the Cloudbreak API expects these credentials as an unstructured parameters set. The implementation has the responsibility to parse, validate, convert and use these credentials (see `AwsCredential`, `AwsCredentialConverter` and `CrossAccountCredentialsProvider` for the AWS part).

A sample AWS credentials request:

```
{
  "cloudPlatform":"AWS",
  "name":"sample-aws-credential",
  "parameters": {
    "roleArn":"arn:aws:iam::123456789000:role/my-iam-role",
  }
}
```

### Cluster creation

Cluster creation and installation is common for every cloud provider, it is based on Ambari and uses SequenceIQ's Ambari REST client.
It is async like the stack creation: the response is sent back immediately, and the flow starts asynchronously.
The sequence diagram below shows the flow.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/seq_diagram_cluster_flow.png)

<!--addnewcloud.md-->

<!--states.md-->

##Stack lifecycle

In order to understand the state of your Hadoop as a Servie stack and the potential outcomes we have put together a UML state diagram.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/stack_state_diag.png)

<!--states.md-->

<!--quickstart.md-->

##QuickStart and installation

We provide you two different ways to start using Cloudbreak. The simplest and easiest solution is hosted by SequenceIQ, however we encourage you to get Cloudbreak and deployed it on-premise or on your favorite cloud provider.

###Hosted - Cloudbreak UI and API  
The easiest way to start your own Hadoop cluster in your favorite cloud provider is to use our hosted solution. We host, maintain and support [Cloudbreak](https://cloudbreak.sequenceiq.com/) for you.

Please note that Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. We do not store your cloud provider account details (such as username, password, keys, private SSL certificates, etc), but work around the concept that Identity and Access Management is fully controlled by you - the end user.

Though Cloudbreak controls your Hadoop cluster lifecycle (start, stop, pause), we **do not** have access to the launched instances. The Hadoop clusters created by Cloudbreak are private to you.


###DIY - Deploying Cloudbreak UI and API

To deploy a running Cloudbreak instance the only thing you will need to do is to get the code first.

```
git clone https://github.com/sequenceiq/cloudbreak-deployment.git
cloudbreak-deployment
```

Follow the instructions.

<!--quickstart.md-->


<!--releases.md-->

##Releases, future plans

When we have started to work on Cloudbreak the idea was to `democratise` the usage of Hadoop in the cloud and VMs. For us this was a necessity as we often had to deal with different Hadoop versions, distributions and cloud providers.

Also we needed to find a way to speed up the process of adding new cloud providers, and be able to `ship` Hadoop between clouds without re-writing and re-engineering our code base each and every time - welcome **Docker**.

All the Hadoop ecosystem related code, configuration and services are inside Docker containers - and these containers are launched on VMs of cloud providers or physical hardware - the end result is the same: a **resilient and dynamic** Hadoop cluster.

We needed to find a unified way to provision, manage and configure Hadoop clusters - welcome **Apache Ambari**.

###Public Beta
The first public beta version of Cloudbreak supports Hadoop on Amazon's EC2, Microsoft's Azure, Google Cloud Platform and OpenStack cloud providers. The supported Hadoop platform is the Hortonworks Data Platform - the 100% open source Hadoop distribution.

Versions:

CentOS - 6.5
Hortonworks Data Platform - 2.2
Apache Hadoop - 2.6.0
Apache Tez - 0.6
Apache Pig - 0.14
Apache Hive & HCatalog - 0.14.0
Apache HBase - 0.98.4
Apache Phoenix - 4.2
Apache Accumulo - 1.6.1
Apache Storm - 0.9.3
Apache Spark - 1.2.0
Apache Slider - 0.5.1
Apache Solr -  4.10.0
Apache Kafka - 0.8.1
Apache Falcon - 0.6.0
Apache Sqoop - 1.4.5
Apache Flume - 1.5.0
Apache Ambari - 1.7.0
Apache Oozie - 4.1.0
Apache Zookeeper - 3.4.5
Apache Knox - 0.5.0
Docker - 1.3
Consul - 0.4.1

###Future releases

####Hadoop distributions

There is an effort by the community and SequenceIQ to bring [Apache Bigtop](http://bigtop.apache.org/) - the Apache Hadoop distribution - under the umbrella of Ambari. Once this effort is finished, Cloudbreak will support Apache Bigtop as a Hadoop distribution as well.

Apache Ambari allows you to create your own [custom Hadoop stack](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=38571133) - and you can use Cloudbreak to provision a cluster based on that.

####Cloud providers

Supported cloud providers:

  * Amazon AWS
  * Microsoft Azure
  * Google Cloud Platform
  * OpenStack

While we have just released the first public beta version of Cloudbreak, we have already started working on other cloud providers - namely *Rackspace* and *HP Helion Public Cloud*.
We have received many requests from people to integrate Cloudbreak with 3d party hypervisors and cloud providers - as IBM's SoftLayer. In case you'd like to have your favorite cloud provider listed don't hesitate to contact us or use our SDK and process to add yours. You can fill the following [questionnaire](https://docs.google.com/forms/d/129RVh6VfjRsuuHOcS3VPbFYTdM2SEjANDsGCR5Pul0I/viewform) and request your favorite cloud provider. In case you'd like to integrate your favorite provider we are happy to support you and merge your contribution.

Enjoy Cloudbreak - the Hadoop as a Service API which brings you a Hadoop ecosystem in minutes. You are literaly one click or REST call away from a fully functional, distributed Hadoop cluster.

<!--releases.md-->

##Contribution

So you are about to contribute to Cloudbreak? Awesome! There are many different ways in which you can contribute. We strongly value your feedback, questions, bug reports, and feature requests.
Cloudbreak consist of the following main projects:

###Cloudbreak UI

Available: <a href=https://cloudbreak.sequenceiq.com>https://cloudbreak.sequenceiq.com</a>

GitHub: <a href=https://github.com/sequenceiq/uluwatu>https://github.com/sequenceiq/uluwatu</a>

###Cloudbreak API

Available: <a href=https://cloudbreak-api.sequenceiq.com>https://cloudbreak-api.sequenceiq.com</a>

GitHub: <a href=https://github.com/sequenceiq/cloudbreak>https://github.com/sequenceiq/cloudbreak</a>

###Cloudbreak REST client

GitHub: <a href=https://github.com/sequenceiq/cloudbreak-rest-client>https://github.com/sequenceiq/cloudbreak-rest-client</a>

###Cloudbreak CLI

GitHub: <a href=https://github.com/sequenceiq/cloudbreak-shell>https://github.com/sequenceiq/cloudbreak-shell</a>

###Cloudbreak documentation

Product documentation: <a href=http://sequenceiq.com/cloudbreak>http://sequenceiq.com/cloudbreak</a>

GitHub: <a href=https://github.com/sequenceiq/cloudbreak/blob/master/docs/index.md>https://github.com/sequenceiq/cloudbreak/blob/master/docs/index.md</a>


API documentation: <a href=https://cloudbreak-api.sequenceiq.com/api/index.html>https://cloudbreak.sequenceiq.com/api</a>

###Ways to contribute

* Use Cloudbreak
* Submit a GitHub issue to the appropriate GitHub repository.
* Submit a new feature request (as a GitHub issue).
* Submit a code fix for a bug.
* Submit a unit test.
* Code review pending pull requests and bug fixes.
* Tell others about these projects.

###Contributing code

We are always thrilled to receive pull requests, and do our best to process them as fast as possible. Not sure if that typo is worth a pull request? Do it! We will appreciate it.
The Cloudbreak projects are open source and developed/distributed under the [Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
If you wish to contribute to Cloudbreak (which you're very welcome and encouraged to do so) then you must agree to release the rights of your source under this license.

####Creating issues

Any significant improvement should be documented as a GitHub issue before starting to work on it. Please use the appropriate labels - bug, enhancement, etc - this helps while creating the release notes for a version release.
Before submitting issues please check for duplicate or similar issues. If you are unclear about an issue please feel free to [contact us](https://groups.google.com/forum/#!forum/cloudbreak).

####Discuss your design

We recommend discussing your plans on the [mailing list](https://groups.google.com/forum/#!forum/cloudbreak) before starting to code - especially for more ambitious contributions. This gives other contributors a chance to point you in the right direction, give feedback on your design, and maybe point out if someone else is working on the same thing.

####Conventions

Please write clean code. Universally formatted code promotes ease of writing, reading, and maintenance.
* Do not use @author tags.

* New classes must match our dependency mechanism.

* Code must be formatted according to our [formatter](https://github.com/sequenceiq/cloudbreak/blob/master/config/eclipse_formatter.xml).

* Code must be checked with our [checkstyle](https://github.com/sequenceiq/cloudbreak/tree/master/config/checkstyle).

* Contributions must pass existing unit tests.

* The code changes must be accompanied by unit tests. In cases where unit tests are not possible or don’t make sense an explanation should be provided.

* New unit tests should be provided to demonstrate bugs and fixes (use Mockito whenever possible).

* The tests should be named *Test.java.

* Use slf4j instead of commons logging as the logging facade.


###Thank you
Huge thanks go to the contributors from the community who have been actively working with the SequenceIQ team. Kudos for that.

###Jobs
Do you like what we are doing? We are looking for exceptional software engineers to join our development team. Would you like to work with others in the open source community?
Please consider submitting your resume and applying for open positions at jobs@sequenceiq.com.


### Legal

*Brought to you courtesy of our legal counsel.*

Use and transfer of Cloudbreak may be subject to certain restrictions by the
United States and other governments.  
It is your responsibility to ensure that your use and/or transfer does not
violate applicable laws.

### Licensing
Cloudbreak is licensed under the Apache License, Version 2.0. See [LICENSE](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/LICENSE) for the full license text.
