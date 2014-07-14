<!--main.md-->

Cloudbreak
================

*Cloudbreak is a powerful left surf that breaks over a coral reef, a mile off southwest the island of Tavarua, Fiji.*

*Cloudbreak is a cloud agnostic Hadoop as a Service API. Abstracts the provisioning and ease management and monitoring of on-demand clusters.*

Cloudbreak [API documentation](http://docs.cloudbreak.apiary.io/).

<!--main.md-->

## Table of Contents
  - [Overview] (#overview)
    - [Benefits] (#benefits)
     - [Secure] (#secure)
     - [Elastic] (#elastic)
     - [Scalable] (#scalable)
     - [Declarative Hadoop clusters] (#declarative-hadoop-clusters)
     - [Flexible] (#flexible)
  - [Quickstart and installation](#quickstart-and-installation)
    - [Running Cloudbreak API using Docker] (#running-cloudbreak-api-using-docker)
      - [Database] (#database)
      - [Cloudbreak REST API] (#cloudbreak-rest-api)
    - [Running Cloudbreak API on the host] (#running-cloudbreak-api-on-the-host)
    - [Configuration] (#configuration)
      - [Development] (#development)
      - [Production] (#production)
  - [How it works] (#how-it-works)
    - [Templates] (#templates)
    - [Stacks] (#stacks)
    - [Blueprints] (#blueprints)
    - [Clusters] (#clusters)
  - [Technology] (#technology)
    - [Apache Ambari](#apache-ambari)
    - [Docker](#docker)
    - [Serf](#serf)
  - [Supported components](#supported-components)
  - [Accounts](#accounts)
    - [Cloudbreak account](#cloudbreak-account)
    - [Configuring the AWS EC2 account](#configuring-the-aws-ec2-account)
    - [Configuring the Microsoft Azure account](#configuring-the-microsoft-azure-account)
  - [Cloudbreak UI](#cloudbreak-ui)
    - [Manage credentials](#manage-credentials)
    - [Manage templates](#manage-templates)
    - [Manage blueprints](#manage-blueprints)
    - [Create cluster](#create-cluster)
  - [Add new cloud providers](#add-new-cloud-providers)
    - [Handling API requests](#handling-api-requests)
    - [Event handling and notifications](#event-handling-and-notifications)
    - [Metadata service](#metadata-service)
    - [Account management](#account-management)
    - [Cluster creation](#cluster-creation)
  - [Releases](#releases)

<!--overview.md-->

##Overview

Cloudbreak is a RESTful Hadoop as a Service API. Once it is deployed in your favorite servlet container exposes a REST API allowing to span up Hadoop clusters of arbitary sizes on your selected cloud provider. Provisioning Hadoop has never been easier.
Cloudbreak is built on the foundation of cloud providers API (Amazon AWS, Microsoft Azure, Google Cloud Compute...), Apache Ambari, Docker containers, Serf and dnsmasq.

##Benefits

###Secure
Supports basic, token based and OAuth2 authentication model. The cluster is provisioned in a logically isolated network (Virtual Private Cloud) of your favorite cloud provider.
Cloudbreak does not store or manages your cloud credentials - it is the end user's responsability to link the Cloudbreak user with her/his cloud account. We provide utilities to ease this process (IAM on Amazon, certificates on Azure).

###Elastic
Using Cloudbreak API you can provision an arbitrary number of Hadoop nodes - the API does the hard work for you, and span up the infrastructure, configure the network and the selected Hadoop components and services without any user interaction.
POST once and use it anytime after.

###Scalable
As your workload changes, the API allows you to add or remove nodes on the fly. Cloudbreak does the hard work of reconfiguring the infrastructure, provision or decomission Hadoop nodes and let the cluster be continuosely operational.
Once provisioned, new nodes will take up the load and increase the cluster throughput.

###Declarative Hadoop clusters
We support declarative Hadoop cluster creation - using blueprints. Blueprints are a declarative definition of a Hadoop cluster. With a blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different VPC subnets and availability zones, thus you can span up a highly available cluster running on different datacenters or availability zones.

###Flexible
You have the option to choose your favorite cloud provider and their different pricing models. The API translated the calls towards different vendors - you develop and use one common API, no need to rewrite your code when changing between cloud providers.

<!--overview.md-->

<!--quickstart.md-->

##Quickstart and installation

###Running Cloudbreak API using Docker

####Database
The only dependency that Cloudbreak needs is a postgresql database. The easiest way to spin up a postgresql is of course Docker. Run it first with this line:
```
docker run -d --name="postgresql" -p 5432:5432 -v /tmp/data:/data -e USER="seqadmin" -e DB="cloudbreak" -e PASS="seq123_" paintedfox/postgresql
```
####Cloudbreak REST API
After postgresql is running, Cloudbreak can be started locally in a Docker container with the following command. By linking the database container, the necessary environment variables for the connection are set. The postgresql address can be set explicitly through the environment variable: DB_PORT_5432_TCP_ADDR.
```
VERSION=0.1-20140623140412

docker run -d --name cloudbreak \
-e "VERSION=$VERSION" \
-e "AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID" \
-e "AWS_SECRET_KEY=$AWS_SECRET_KEY" \
-e "HBM2DDL_STRATEGY=create" \
-e "MAIL_SENDER_USERNAME=$MAIL_SENDER_USERNAME" \
-e "MAIL_SENDER_PASSWORD=$MAIL_SENDER_PASSWORD" \
-e "MAIL_SENDER_HOST=$MAIL_SENDER_HOST" \
-e "MAIL_SENDER_PORT=$MAIL_SENDER_PORT" \
-e "MAIL_SENDER_FROM=$MAIL_SENDER_FROM" \
-e "HOST_ADDR=$HOST_ADDR" \
--link postgresql:db -p 8080:8080 \
dockerfile/java bash \
-c 'curl -o /tmp/cloudbreak-$VERSION.jar https://s3-eu-west-1.amazonaws.com/seq-repo/releases/com/sequenceiq/cloudbreak/$VERSION/cloudbreak-$VERSION.jar && java -jar /tmp/cloudbreak-$VERSION.jar'

```

Note: The system properties prefixed with MAIL_SENDER_ are the SNMP settings required to send emails.  

###Running Cloudbreak API on the host

If you'd like to run Cloudbreak outside of a Docker container - directly on the host - we provide you shell script that starts the app for you.

After building the application _(./gradlew clean build)_ please run the following script from the project root:

```
./run_cloudbreak.sh <host-addr> \
<db-user> \
<db-pass> \
<db-host> \
<db-port> \
<hbm2ddl-strategy> \
<smtp-username> \
<smpt-password \
<smtp-host> \
<smtp-port \
<mail-sender-from>

```
Where:

`host-address`     - the ngrok generated address to receive SNS notifications

`db-user`          - database user name

`db-pass`          - password for the database

`db-host`          - the address of the machine hosting the database

`db-port`          - the port where you can connect to the database

`hbm2ddl-strategy` - the desired value for the *hibernate.hbm2ddl.auto* configuration

`smtp-username`    - SMTP username

`smpt-password`    - SMTP password

`smtp-host`        - SMTP host address

`smtp-port`        - SMTP port

`mail-sender-from` - the email address to use when sending registration confirmation or password renewal emails


Please note, that configuration properties can be given both as arguments to the script and as system properties.

*Warning*: When providing configuration as arguments to the script, the arguments should follow the order above!  


##Configuration

###Development

In order to be able to receive Amazon push notifications on localhost, you will need to install a secure introspectable tunnel to localhost.

###Install and configure ngrok
Cloudbreak uses SNS to receive notifications. On OSX you can do the following:

```
brew update && brew install ngrok
ngrok 8080
```
_Note: In the terminal window you'll find displayed a value - this is the last argument `host-address` of the `run_cloudbreak.sh` script_

###Production

In production environments make sure the following system properties are set:

```
# The host running the cloudbreak app
HOST_ADDR

# SMTP related properties (required for account registration, password renewal)
MAIL_SENDER_USERNAME
MAIL_SENDER_PASSWORD
MAIL_SENDER_HOST
MAIL_SENDER_PORT
MAIL_SENDER_FROM

# Database related properties
DB_ENV_USER
DB_ENV_PASS
DB_PORT_5432_TCP_ADDR
DB_PORT_5432_TCP_PORT
HBM2DDL_STRATEGY
```

If you'd like to work with AWS you'll need to set two more system propwrties:

```
# AWS credentials
AWS_ACCESS_KEY_ID
AWS_SECRET_KEY
```

<!--quickstart.md-->

<!--howitworks.md-->

##How it works?

Cloudbreak launches on-demand Hadoop clusters on your favorite cloud provider in minutes. We have introduced 4 main notions - the core building block of the REST API.

###Templates

A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion.
Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, SSH setup and can capture and control region-specific infrastructure variations.

A template can be used repeatedly to create identical copies of the same stack (or to use as a foundation to start a new stack).

The infrastructure specific configuration is available under the Cloudbreak [resources](https://github.com/sequenceiq/cloudbreak/blob/master/src/main/resources/vpc-and-subnet.template).
As an example, for Amazon EC2, we use [AWS Cloudformation](http://aws.amazon.com/cloudformation/) to define the cloud infrastructure .

For further information please visit our [API documentation](http://docs.cloudbreak.apiary.io/#templates).

###Stacks

Stacks are template `instances` - a runnig cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks supports a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.

For further information please visit our [API documentation](http://docs.cloudbreak.apiary.io/#stacks).

###Blueprints

Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different VPC subnets and availability zones, thus you can span up a highly available cluster running on different datacenters or availability zones.
We have a few default blueprints available from single note to multi node blueprints and lamba architecture.

For further information please visit our [API documentation](http://docs.cloudbreak.apiary.io/#blueprints).

###Cluster

Clusters are materialized Hadoop clusters. They are built based on a Bluerint (running the components and services specified) and on a configured infrastructure Stack.
Once a cluster is created and launched it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.

For further information please visit our [API documentation](http://docs.cloudbreak.apiary.io/#clusters).

<!--howitworks.md-->

<!--technologies.md-->

##Technology

Cloudbreak is built on the foundation of cloud providers APIs, Apache Ambari, Docker containers, Serf and dnsmasq.

###Apache Ambari

The Apache Ambari project is aimed at making Hadoop management simpler by developing software for provisioning, managing, and monitoring Apache Hadoop clusters. Ambari provides an intuitive, easy-to-use Hadoop management web UI backed by its RESTful APIs.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/ambari-overview.png?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL21hc3Rlci9kb2NzL2ltYWdlcy9hbWJhcmktb3ZlcnZpZXcucG5nIiwiZXhwaXJlcyI6MTQwNTQyNTQ0NH0%3D--b5c7b0e5adc62ebd34c8a5b73806f6ac905faed2)

Ambari enables System Administrators to:

1. Provision a Hadoop Cluster
  * Ambari provides a step-by-step wizard for installing Hadoop services across any number of hosts.
  * Ambari handles configuration of Hadoop services for the cluster.

2. Manage a Hadoop Cluster
  * Ambari provides central management for starting, stopping, and reconfiguring Hadoop services across the entire cluster.

3. Monitor a Hadoop Cluster
  * Ambari provides a dashboard for monitoring health and status of the Hadoop cluster.
  * Ambari leverages Ganglia for metrics collection.
  * Ambari leverages Nagios for system alerting and will send emails when your attention is needed (e.g., a node goes down, remaining disk space is low, etc).

Ambari enables to integrate Hadoop provisioning, management, and monitoring capabilities into applications with the Ambari REST APIs.
Ambari Blueprints are a declarative definition of a cluster. With a Blueprint, you can specify a Stack, the Component layout and the Configurations to materialize a Hadoop cluster instance (via a REST API) without having to use the Ambari Cluster Install Wizard.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/ambari-create-cluster.png?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL21hc3Rlci9kb2NzL2ltYWdlcy9hbWJhcmktY3JlYXRlLWNsdXN0ZXIucG5nIiwiZXhwaXJlcyI6MTQwNTQyNTUxOH0%3D--4207ee222d43a67deeb6e5174029cef45ea4f271)

###Docker

Docker is an open platform for developers and sysadmins to build, ship, and run distributed applications. Consisting of Docker Engine, a portable, lightweight runtime and packaging tool, and Docker Hub, a cloud service for sharing applications and automating workflows, Docker enables apps to be quickly assembled from components and eliminates the friction between development, QA, and production environments. As a result, IT can ship faster and run the same app, unchanged, on laptops, data center VMs, and any cloud.

The main features of Docker are:

1. Lightweight, portable
2. Build once, run anywhere
3. VM - without the overhead of a VM
  * Each virtualized application includes not only the application and the necessary binaries and libraries, but also an entire guest operating system
  * The Docker Engine container comprises just the application and its dependencies. It runs as an isolated process in userspace on the host operating system, sharing the kernel with other containers.
    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/vm.png?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL21hc3Rlci9kb2NzL2ltYWdlcy92bS5wbmciLCJleHBpcmVzIjoxNDA1NDI1NTc1fQ%3D%3D--b07a90b876bd4e26f361f37eed901f874f71bf87)

4. Containers are isolated
5. It can be automated and scripted

###Serf

Serf is a tool for cluster membership, failure detection, and orchestration that is decentralized, fault-tolerant and highly available. Serf runs on every major platform: Linux, Mac OS X, and Windows. It is extremely lightweight.
Serf uses an efficient gossip protocol to solve three major problems:

  * Membership: Serf maintains cluster membership lists and is able to execute custom handler scripts when that membership changes. For example, Serf can maintain the list of Hadoop servers of a cluster and notify the members when nodes comes online or goes offline.

  * Failure detection and recovery: Serf automatically detects failed nodes within seconds, notifies the rest of the cluster, and executes handler scripts allowing you to handle these events. Serf will attempt to recover failed nodes by reconnecting to them periodically.
    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/serf-gossip.png?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL21hc3Rlci9kb2NzL2ltYWdlcy9zZXJmLWdvc3NpcC5wbmciLCJleHBpcmVzIjoxNDA1NDI1NjA4fQ%3D%3D--358fedd73c8fbd20a105dcc74bdf66524d680134)
  * Custom event propagation: Serf can broadcast custom events and queries to the cluster. These can be used to trigger deploys, propagate configuration, etc. Events are simply fire-and-forget broadcast, and Serf makes a best effort to deliver messages in the face of offline nodes or network partitions. Queries provide a simple realtime request/response mechanism.
    ![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/serf-event.png?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL21hc3Rlci9kb2NzL2ltYWdlcy9zZXJmLWV2ZW50LnBuZyIsImV4cGlyZXMiOjE0MDU0MjU2Mjh9--69ea14f7dc1db34dd1dd6585df42ad8f2dd2a9d1)

<!--technologies.md-->

<!--components.md-->

##Supported components

Ambari supports the concept of stacks and associated services in a stack definition. By leveraging the stack definition, Ambari has a consistent and defined interface to install, manage and monitor a set of services and provides extensibility model for new stacks and services to be introduced.

At high level the supported list of components can be grouped in to main categories: Master and Slave - and bundling them together form a Hadoop Service.

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
| NAGIOS		  | NAGIOS_SERVER                                                           |
| OOZIE		    | OOZIE_CLIENT, OOZIE_SERVER                                              |
| PIG		      | PIG                                                                     |
| SQOOP		    | SQOOP                                                                   |
| STORM		    | DRPC_SERVER, NIMBUS, STORM_REST_API, STORM_UI_SERVER, SUPERVISOR        |
| TEZ		      | TEZ_CLIENT                                                              |
| FALCON		  | FALCON_CLIENT, FALCON_SERVER                                            |
| ZOOKEEPER	  | ZOOKEEPER_CLIENT, ZOOKEEPER_SERVER                                      |


We provide a list of default Hadoop cluster Blueprints for your convenience, however you can always build and use your own Blueprint.

1. Simple single node - Apache Ambari blueprint

This is a simple [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/single-node-hdfs-yarn) which allows you to launch a single node, pseudo-distributed Hadoop Cluster in the cloud.

It's allows you to use the following services: HDFS, YARN, MAPREDUCE2.

2. Full stack single node - HDP 2.1 blueprint

This is a complex [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/hdp-singlenode-default) which allows you to launch a single node, pseudo-distributed Hadoop Cluster in the cloud.

It's allows you to use the following services: HDFS, YARN, MAPREDUCE2, GANGLIA, HBASE, HIVE, HCATALOG, WEBHCAT, NAGIOS, OOZIE, PIG, SQOOP, STORM, TEZ, FALCON, ZOOKEEPER.

3. Simple multi node - Apache Ambari blueprint

This is a simple [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/multi-node-hdfs-yarn) which allows you to launch a multi node, fully distributed Hadoop Cluster in the cloud.

It's allows you to use the following services: HDFS, YARN, MAPREDUCE2.

4. Full stack multi node - HDP 2.1 blueprint

This is a complex [Blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/hdp-multinode-default) which allows you to launch a multi node, fully distributed Hadoop Cluster in the cloud.

It's allows you to use the following services: HDFS, YARN, MAPREDUCE2, GANGLIA, HBASE, HIVE, HCATALOG, WEBHCAT, NAGIOS, OOZIE, PIG, SQOOP, STORM, TEZ, FALCON, ZOOKEEPER.

5. Custom blueprints

We allow you to build your own Blueprint - for further instractions please check the Apache Ambari [documentation](https://cwiki.apache.org/confluence/display/AMBARI/Blueprints).

When you are creating clustom Blueprints you can use the components above to build Hadoop services and use them in your on-demand Hadoop cluster.

We are trying to figure out the hosts to hostgroups assignements - and order to do so you will need to follow the conventions below:

_Note: Apache Ambari community and SequenceIQ is working on an auto-hostgroup assignement algorithm; in the meantime please follow our conventions and check the default blueprints as examples, or ask us to support you._

1. When you are creating a Single node blueprint the name of the default host group has to be `master`.
2. When yoy are creating a Multi node blueprint, all the worker node components (a.k.a. Slaves) will have to be grouped in host groups named `slave_*`. _Replace * with the number of Slave hostgroups_.

The default rules are that for multi node clusters are there must be at least as many hosts as the number of host groups. Each NOT slave host groups (master, gateway, etc) will be launched wiht a cardinality of 1 (1 node per master, gateway, etc hosts), and all the rest of the nodes are equally distributed among Slave nodes (if there are multiple slave host groups).

<!--components.md-->

<!--accounts.md-->

##Accounts

###Cloudbreak account

First and foremost in order to start launching Hadoop clusters you will need to create a Cloudbreak account. C
loudbreak supports registration, forgotten and reset password, and login features at API level.
All password are stored or sent are hashed - communication is always over a secure HTTPS channel. When you are deploying your own Cloudbreak instance we strongy suggest to configure an SSL certificate.
Users create and launch Hadoop clusters on their own namespace and security context.

Cloudbreak is launching Hadoop clusters on the user's behalf - on different cloud providers. One key point is that Cloudbreak **does not** store your Cloud provider account details (such as username, passord, keys, private SSL certificates, etc).
We work around the concept that Identity and Access Management is fully controlled by you - the end user. The Cloudbreak *deployer* is purely acting on behalf of the end user - without having access to the user's account.

**How does this work**?

###Configuring the AWS EC2 account

Once you have logged in Cloudbreak you will have to link your AWS account with the Cloudbreak one. In order to do that you will need to configure an IAM Role.

1. Log in to the AWS management console with the user account you'd like to use with Cloudbreak
2. Go to IAM and select Roles
  * Select Role for Cross-Account access
    *  Allows IAM users from a 3rd party AWS account to access this account.

      **Account ID:** In case you are using our hosted solution you will need to pass SequenceIQ's account id: 755047402263

      **External ID:** provision-ambari (association link)

    * Custom policy
      Use this policy [document](https://raw.githubusercontent.com/sequenceiq/cloudbreak/documentation/src/main/resources/iam-arn-custom.policy?token=6003104__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL2RvY3VtZW50YXRpb24vc3JjL21haW4vcmVzb3VyY2VzL2lhbS1hcm4tY3VzdG9tLnBvbGljeSIsImV4cGlyZXMiOjE0MDUzMzc2MjV9--cde4acae8c67317e6245598526be8cf680a08914) to configure the permission to start EC2 instances on the end user's behalf, and use SNS to receive notifications.


Once this is configured, Cloudbreak is ready to launch Hadoop clusters on your behalf. The only thing Cloudbreak requires is the `Role ARN` (Role for Cross-Account access).


###Configuring the Microsoft Azure account

Once you have logged in Cloudbreak you will have to link your Azure account with the Cloudbreak one. Cloudbreak asks for your Azure `Subscription Id`, and will generate a `JKS` file and a `certificate` for you with your configured `passphrase`.
The JKS file and certificate (uploaded) will be used to encrypt the communication between Cloudbreak and Azure in both directions.
Additionally when you are creating Templates you can specify a `password` or an `SSH public key` in order for you to be able to login in the launched instances. As you can see the communication is bith directions is secure, and we will not be able to login into your instances.

In order to create a Cloudbreak account associated with your Azure account you will need to do the following steps.

1. Log into Azure management console with the user account you'd like to use with Cloudbreak
2. On Azure go to Settings and Subscriptions tab - Cloudbreak will need your `Subscription Id` to associate accounts
3. On Cloudbreak API or UI you will have to add a password - this will be used as the `passphrase` for the JKS file and certificate we generate.
4. You should download the generated `certification`
5. On Azure go to Settings and `Management Certificates` and upload the `cert` file

You are done - from now on Cloudbreak can launch Azure instances on your behalf.

_Note: Clodbreak does not store any login details into these instances - when you are creating Templates you can specify a `password` or `SSH Public key` which you can used to login into the launched instances._

<!--accounts.md-->

<!--ui.md-->

##Cloudbreak UI

When we have started to work on Cloudbreak our main goal was to create an easy to use, cloud and Hadoop distribution agnostis Hadoop as a Service API. Though we always like to automate everything and aproach things with a very DevOps mindset, as a side project we have created a UI for Cloudbreak as well.
The goal of the UI is to ease to process and allow you to create a Hadoop cluster on your favorite cloud provider in `one-click`.

The UI is built on the foundation of the Cloudbreak REST API.

###Manage credentials
Using manage credentials you can link your cloud account with the Cloudbreak account.

**Amazon AWS**

`Name:` name of your credential

`Description:` short description of your linked credential

`Role ARN:` the role string - see Accounts

**Azure**

`Name:` name of your credential

`Description:` short description of your linked credential

`Subscription Id:` your Azure subscription id - see Accounts

`File password:` your generated JKS file password - see Accounts

###Manage templates

Using manage templates you can create infrastructure templates.

**Amazon AWS**

`Name:` name of your template

`Description:` short description of your template

`AMI:` the AMI which contains the Docker containers

`SSH location:` allowed inbound SSH access. Use 0.0.0.0/0 as default

`SSH key name:` your SSH key name used on AWS

`Region:` AWS region where you'd like to launch your cluster

`Instance type:` the Amazon instance type to be used - we suggest to use at least small or medium instances

**Azure**

`Name:` name of your template

`Description:` short description of your template

`Location:` Azure datacenter location where you'd like to launch your cluster

`Image name:` The Azure base image used

`Instance type:` the Azure instance type to be used - we suggest to use at least small or medium instances

`Password:` if you specify a password you can use it later to log into you launched instances

`SSH public key` if you specify an SSH public key you can use your private key later to log into you launched instances

###Manage blueprints
Blueprints are your declarative definition of a Hadoop cluster.

`Name:` name of your blueprint

`Description:` short description of your blueprint

`Source URL:` you can add a blueprint by pointing to a URL. As an example you can use this [blueprint](https://raw.githubusercontent.com/sequenceiq/ambari-rest-client/master/src/main/resources/blueprints/multi-node-hdfs-yarn).

`Manual copy:` you can copy paste your blueprint in this text area

_Note: Apache Ambari community and SequenceIQ is working on an auto-hostgroup assignement algorithm; in the meantime please follow our conventions and check the default blueprints as examples, or ask us to support you._

_1. When you are creating a Single node blueprint the name of the default host group has to be `master`._
_2. When yoy are creating a Multi node blueprint, all the worker node components (a.k.a. Slaves) will have to be grouped in host groups named `slave_*`. Replace * with the number of Slave hostgroups._

_The default rules are that for multi node clusters are there must be at least as many hosts as the number of host groups. Each NOT slave host groups (master, gateway, etc) will be launched wiht a cardinality of 1 (1 node per master, gateway, etc hosts), and all the rest of the nodes are equally distributed among Slave nodes (if there are multiple slave host groups)._

###Create cluster
Using the create cluster functionality you will create a cloud Stack and a Hadoop Cluster. In order to create a cluster you will have to select a credential first.
_Note: Cloudbreak can maintain multiple cloud credentials (even for the same provider)._

`Cluster name:` your cluster name

`Cluster size:` the number of nodes in your Hadoop cluster

`Template:` your cloud infrastructure template to be used

`Blueprint:` your Hadoop cluster blueprint

Once you have launched the cluster creation you can track the progress either on Cloudbreak UI or your cloud provider management UI.

<!--ui.md-->

<!--addnewcloud.md-->

## Add new cloud providers

Cloudbreak is built from ground up on the idea of being cloud provider agnostic. All the external API's are cloud agnostic, and we have
internally abstracted working with individual cloud providers API's. Nevertheless adding new cloud providers is extremely important for us, thus
in order to speed up the process and linking the new provider API with Cloudbreak we came up with an SDK and list of responsibilities.
Once these interfaces are implemented, and the different provider's API calls are `translated` you are ready to go.

Though we are working on a few popular providers to add to Cloudbreak we'd like to hear your voice as well - your ideas, provider requests or `contribution` is highly appreciated.

### Handling API requests

Connecting a new cloud provider means that the Cloudbreak rest API should handle GET, POST and DELETE requests to a stack resource of the new cloud provider correctly.

- *POST*: Creates the cloud resources (VPC, instances, etc) and properly starts Sequenceiq's Ambari Docker container on the instances.

- *DELETE*: Terminates all instances in the cloud and deletes every other resources

- *GET*: Describes the cloud resources by communicating with the cloud provider.

When connecting a new cloud provider, the `CloudPlatform` enum that holds the providers should be extended first. It is used to find the implementations when a request arrives.
The main idea is the same behind every method: deal with the database calls in the controller then call the correct implementation that communicates with the cloud platform. This enables the connectors to be detached from the repository calls, they should only deal with the communication with the providers.

After an interface is implemented there is only one task to make it available: put the `@Service` annotation on the class. Spring will take care of the rest by discovering the bean when building the application context and putting it in the list that holds the different implementations (see `AppConfig`).

#### POST
Cloudbreak uses an event-driven flow to provision stacks in the cloud. It gives more freedom to the cloud provider implementations and allows developers to build async flows easily, yet unifies the process and takes off the responsiblity from the implementations to manage the lifecycle of a stack entity.
It means that connecting a new provider involves implementing the necessary interfaces, and sending different 'completed' events after a specific step is done.

The flow is presented with the sequence diagrams below.
The first diagram shows how the process is started when a `POST` request is sent to the API, the second one shows the actual provisioning flow's first part which contains the cloud platform specific services. The final diagram contains the last part of the provision flow that's common for every provider.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/seq_diagram_stack_post.png?token=1568469__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL21hc3Rlci9kb2NzL2ltYWdlcy9zZXFfZGlhZ3JhbV9zdGFja19wb3N0LnBuZyIsImV4cGlyZXMiOjE0MDU2OTU0MzJ9--29d50c7dd14b7a0d2b68d82dd2a92a661d3e421d)

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

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/seq_diagram_provision_flow_2.png?token=1568469__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL21hc3Rlci9kb2NzL2ltYWdlcy9zZXFfZGlhZ3JhbV9wcm92aXNpb25fZmxvd18yLnBuZyIsImV4cGlyZXMiOjE0MDU2OTU0NDF9--37c78d072a5c16fb0dba4128351c63d17b33cb83)

*Notes:*

- Ambari server is not shown on the diagram, but health check is basically a call to the Ambari REST API's health endpoint

- `MetadataSetupCH` = `MetadataSetupCompleteHandler`

- `AmbariRoleAllocationCH` = `AmbariRoleAllocationCompleteHandler`

- `StackCreationSH` = `StackCreationSuccessHandler`

- `ClusterRequestH` = `ClusterRequestHandler`


When adding a new provider, the following **3+1 steps** should be implemented to handle the provisioning successfully. The first three steps are similar: they involve the implementation of different interfaces and they should send `complete` events after their task is done. Sending only events when a task is done enables the implementations themselves to use async processes. For example AWS provisioning uses such an implementation (See `Provisioner` for the details).
The last step is a bit different because it requires the implementation of the user-data bash script that runs on every cloud machine instance after they are started.

- `ProvisionSetup`: This step should create every cloud provider resource that will be used during the provisioning. For example the EC2 implementation uses SNS topics to notify Cloudbreak when an EC2 resource creation is completed. These topics are created (or retrieved) in this step, and the identifiers are sent in the `PROVISION_SETUP_COMPLETE` event as a key-value pair. (see `AwsProvisionSetup`)

- `Provisioner`: The actual cloud resource creation is done here. The `PROVISION_COMPLETE` must be sent after every resource creation is initialized. It must contain the type and id of the created resources. This process can be async itself, e.g.: AWS CloudFormation is able to notify clients through an SNS topic when specific resources are created - the `PROVISION_COMPLETE` event is only sent after this notification arrives. `AwsProvisioner` handles the resource requests and `SnsMessageHandler` handles the notifications coming from Amazon SNS.
There are a few restriction for the resources to be created: the service must start as many instances as it is specified in the stack object and these instances must be started in a different subnet for every stack. They must also be able to reach the Internet, and a few ports should be open in the subnet.
We recommend to create and use **pre-installed images** from a vanilla Ubuntu that have Docker and some other required tools installed and the sequenceiq/ambari image downloaded, so these things don't have to be done every time an instance is started. This prevents network issues and shortens the time of the provisioning. We have also created an [Ansible playbook]() that can be used to create the image.

- `MetadataSetup`: The Cloudbreak instance metadata service is detailed [here](#metadata-service). For this to work, the cloud platform connectors must provide details to Cloudbreak about the instances that were started in a stack. The `CoreInstanceMetaData` (private IP in VPC, public IP and instance identifier) of every instance must be retrieved from the cloud platform and must be sent in a `METADATA_SETUP_COMPLETE` event. (see `AwsMetadataSetup`)

- `user-data script`: Most cloud platforms provide user-data provisioning when starting an instance. It means that a specific script can be handed to the instance and it is run when the instance is initializing. User-data is usually handled by [CloudInit](https://help.ubuntu.com/community/CloudInit) on Ubuntu. This script is responsible for setting up the network configuration on an instance and start the Serf and Dnsmasq based Ambari Docker container with the proper variables.
We have created Cloudbreak's metadata service to be able to use the same script everywhere, but the different cloud platforms can have different characteristics so we left the possibility to write a custom script too. The user data should be saved in `src/main/resources` with the name `[prefix]-init.sh` and it's prefix must be specified in the `CloudPlatform` enum.

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

To learn more about the `sequenceiq/ambari` container, how it works with Serf and Dnsmasq check the [repo](https://github.com/sequenceiq/docker-ambari) on Github and read the blog posts about the [single-node](http://blog.sequenceiq.com/blog/2014/06/17/ambari-cluster-on-docker/) and [multi-node](http://blog.sequenceiq.com/blog/2014/06/19/multinode-hadoop-cluster-on-docker/) Hadoop clusters on Docker.


### Account management

Every cloud platform requires some kind of credentials when requesting, deleting or describing resources. Cloudbreak supports the management of credentials through it's REST API. First a credential should be created and later when a stack is requested, the credential has to be attached. This credential will be used by Cloudbreak for authentication and authorization to the cloud provider. This is a sample `POST: /stacks` request body with a credential:
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
It is async like the stack creation: the response is sent back immedately, and the flow starts asynchronously.
The sequence diagram below shows the flow.

![](https://raw.githubusercontent.com/sequenceiq/cloudbreak/master/docs/images/seq_diagram_cluster_flow.png?token=1568469__eyJzY29wZSI6IlJhd0Jsb2I6c2VxdWVuY2VpcS9jbG91ZGJyZWFrL21hc3Rlci9kb2NzL2ltYWdlcy9zZXFfZGlhZ3JhbV9jbHVzdGVyX2Zsb3cucG5nIiwiZXhwaXJlcyI6MTQwNTY5NTQzNn0%3D--7966403bc08a94e8fda5ce17aae800523856219a)

<!--addnewcloud.md-->

<!--releases.md-->

##Releases

When we have started to work on Cloudbreak the idea was to `democratize` the usage of Hadoop in the cloud and VMs. For us this was a necesity as we often had to deal with different Hadoop versions, distributions and cloud providers.

Also we needed to find a way to speed up the process of adding new cloud providers, and be able to `ship` Hadoop between clouds without re-writing and re-engineering our code base each and every time - welcome **Docker**.

All the Hadoop ecosystem related code, configuration and serivces are inside Docker containers - and these containers are launched on VMs of cloud providers or physical hardware - the end result is the same: a **resilient and dynamic** Hadoop cluster.

We needed to find a unified way to provision, manage and configurure Hadoon clusters - welcome **Apache Ambari**.

###Current release - 0.1
The first public beta version of Cloudbreak supports Hadoop on Amazon's EC2 and Microsoft's Azure cloud providers. The currently supported Hadoop is the Hortonworks Data Platform - the 100% open source Hadoop distribution.

Versions:

CentOS - 6.5
Hortonworks Data Platform - 2.1
Apache Hadoop - 2.4.0
Apache Tez - 0.4
Apache Pig - 0.12.1
Apache Hive & HCatalog - 0.13.0
Apache HBase - 0.98.0
Apache Phoenix - 4.0.0
Apache Accumulo - 1.5.1
Apache Storm - 0.9.1
Apache Mahout - 0.9.0
Apache Solr - 4.7.2
Apache Falcon - 0.5.0
Apache Sqoop - 1.4.4
Apache Flume - 1.4.0
Apache Ambari - 1.6.1
Apache Oozie - 4.0.0
Apache Zookeeper - 3.4.5
Apache Knox - 0.4.0
Docker - 1.1
Serf - 0.5.0
dnsmasq - 2.7

###Future releases

**Hadoop distributions**

There is a great effort by the community and SequenceIQ to bring [Apache Bigtop](http://bigtop.apache.org/) - the Apache Hadoop distribution - under the umbrella of Apache Ambari. Once this effort is finished, Cloudbreak will support Apache Bigtop as a Hadoop distribution as well.

In the meantime we have started an internal R&D project to bring Cloudera's CDH distribution under Apache Ambari - in case you would like to collaborate in this task with us or sounds interesting to you don't hesitate to let us know.

**Cloud providers**

While we have just released the first public beta version of Cloudbreak, we have already started working on other cloud providers - namely Google Cloud Compute and Digital Ocean.
We have received many requests from people to integrate Cloudbreak wih 3d party hypervisors and cloud providers - as IBM's Softlayer. In case you'd like to have your favorite cloud provider listed don't hesitate to contact us or use our SDK and process to add yours.

Enjoy Cloudbreak - the Hadoop as a Service API which brings you a Hadoop ecosystem in matters on minutes. You are literarily one click or two REST calls away from a fully functional, distributed Hadoop cluster.

<!--releases.md-->
