Cloudbreak
================

*Cloudbreak is a powerful left surf that breaks over a coral reef, a mile off southwest the island of Tavarua, Fiji.*

*Cloudbreak is a cloud agnostic Hadoop as a Service API. Abstracts the provisioning and ease management and monitoring of on-demand clusters.*

http://docs.cloudbreak.apiary.io/


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

###Blueprints
Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different VPC subnets and availability zones, thus you can span up a highly available cluster running on different datacenters or availability zones.

###Flexible
You have the option to choose your favorite cloud provider and their different pricing models. The API translated the calls towards different vendors - you develop and use one common API, no need to rewrite your code when changing between cloud providers.
