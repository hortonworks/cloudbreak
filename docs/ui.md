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
