cloudbreak
================

*Cloudbreak is a powerful left surf that breaks over a coral reef, a mile off southwest the island of Tavarua, Fiji.*

*Cloudbreak is a cloud agnostic Hadoop as a Service API. Abstracts the provisioning and ease management and monitoring of on-demand clusters.*

http://docs.cloudbreak.apiary.io/

## Table of Contents
  - Overview [#overview]
  - Benefits [#benefits] 
   - Secure [#secure]
  - Running the Cloudbreak API

##Overview

Cloudbreak is a RESTful application development platform with the goal of helping developers to build solutions for deploying Hadoop YARN clusters in different environments. Once it is deployed in your favorite servlet container it exposes a REST API allowing to span up Hadoop clusters of arbitary sizes and cloud providers. Provisioning Hadoop has never been easier.
Cloudbreak is built on the foundation of cloud providers API (Amazon AWS, Microsoft Azure), Apache Ambari, Docker lightweight containers, Serf and dnsmasq. 


##Benefits

###Secure
It supports a token based and OAuth2 authentication model. The cluster is provisioned in a logically isolated network (Virtual Private Cloud) of your cloud favorite cloud provider.
If the cluster is launched in a VPC network, the framework gonfigure firewall settings that control the network access of your launched instances. For example a Hadoop Resource Manager can be accessed from the internet, whereas non of the other nodes are available.
Cloudbreak does not store or manages your cloud credentials - it is the end user's responsability to link Cloudbreak with her/his cloud account. We provide utilities to ease this process (IAM on Amazon, certificates on Azure).

###Elastic
Using Cloudbreak API you can provision an arbitrary number of Hadoop nodes - the API does the hard work for you, and span up the cluster, configure the networks and the selected Hadoop services without any interaction. 
POST once and use it anytime after.

###Scalable
As your workload changes, the API allows you to add or remove nodes on the fly. Cloudbreak does the hard work of reconfiguring the infrastructure, provision or decomission Hadoop nodes and let the cluster be continuosely operational. 
Once provisioned, new nodes will take up the load and increase the cluster throughput.

###Blueprints
Supports different Hadoop cluster blueprints. Hostgroups defined in blueprints can be associated to different VPC subnets and availability zones, thus you can span up a cluster for deploying highly available applications.

###Flexible
You have the option to choose your favorite cloud provider and different pricing models. The API translated the calls towards different cloud vendors - you develop and use one common API, no need to rewrite you code when changing between cloud providers.

##Running the Cloudbreak API
The only dependency that Cloudbreak needs is a postgresql database. The easiest way to spin up a postgresql is of course Docker. Run it first with this line:
```
docker run -d --name="postgresql" -p 5432:5432 -v /tmp/data:/data -e USER="seqadmin" -e DB="cloudbreak" -e PASS="seq123_" paintedfox/postgresql
```

After postgresql is running, Cloudbreak can be started locally in a Docker container with the following command. By linking the database container, the necessary environment variables for the connection are set. The postgresql address can be set explicitly through the environment variable: DB_PORT_5432_TCP_ADDR. 
```
VERSION=0.1-20140623140412

docker run -d --name cloudbreak -e "VERSION=$VERSION" -e "AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID" -e "AWS_SECRET_KEY=$AWS_SECRET_KEY" -e "HBM2DDL_STRATEGY=create" --link postgresql:db -p 8080:8080 dockerfile/java bash -c 'curl -o /tmp/cloudbreak-$VERSION.jar https://s3-eu-west-1.amazonaws.com/seq-repo/releases/com/sequenceiq/cloudbreak/$VERSION/cloudbreak-$VERSION.jar && java -jar /tmp/cloudbreak-$VERSION.jar'
```

To be able to use Cloudbreak, a keypair of an AWS IAM user must be specified. Because Cloudbreak creates AWS resources on third party accounts, the only permission this keypair needs is sts:assumeRole to be able to assume an IAM role to retrieve temporary credentials from AWS.

