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

###Blueprints

Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different VPC subnets and availability zones, thus you can span up a highly available cluster running on different datacenters or availability zones.
We have a few default blueprints available from single note to multi node blueprints and lamba architecture.

###Cluster

Clusters are materialized Hadoop clusters. They are built based on a Bluerint (running the components and services specified) and on a configured infrastructure Stack.
Once a cluster is created and launched it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.
