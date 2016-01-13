#Benefits

##Secure
Supports basic, token based and OAuth2 authentication model. The cluster is provisioned in a logically isolated network (Virtual Private Cloud) of your favorite cloud provider. Cloudbreak does not store or manage your cloud credentials - it is the end user's responsibility to link the Cloudbreak user with her/his cloud account.

##Elastic
Using Cloudbreak API you can provision an arbitrary number of Hadoop nodes - the API does the hard work for you, and span up the infrastructure, configure the network and the selected Hadoop components and services without any user interaction. POST once and use it anytime after.

##Scalable
As your workload changes, the API allows you to add or remove nodes on the fly. Cloudbreak does the hard work of reconfiguring the infrastructure, provision or decommission Hadoop nodes and let the cluster be continuously operational. Once provisioned, new nodes will take up the load and increase the cluster throughput.

##Declarative Hadoop clusters
We support declarative Hadoop cluster creation - using blueprints. Blueprints are a declarative definition of your stack, the component/services layout and the configurations to materialize a Hadoop cluster instance.

##Flexible
You have the option to choose your favorite cloud provider and their different pricing models. The API translates the calls towards different vendors. Nevertheless you integrate and use one common API, there is no need to rewrite your code when changing between cloud providers.

<!--overview.md-->

<!--howitworks.md-->


##How it works?

Cloudbreak launches on-demand Hadoop clusters on your favorite cloud provider in minutes. We have introduced 4 main notions - the core building blocks of the REST API.

###Templates

A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related `resources`, maintaining and updating them in an orderly and predictable fashion.
Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types, and can capture and control region-specific infrastructure variations. We support heterogenous clusters - this means that one Hadoop cluster can be built by combining different templates.

A template can be used repeatedly to create identical copies of the same stack (or to use as a foundation to start a new stack).

The infrastructure specific configuration is available under the Cloudbreak resources.
As an example for Amazon EC2, we use [AWS Cloudformation](http://aws.amazon.com/cloudformation/) to define the cloud infrastructure.

For further information please visit our [API documentation](https://cloudbreak-api.sequenceiq.com/api/index.html#/templates).

###Stacks

Stacks are template `instances` - a running cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account. Stacks support a wide range of resources, allowing you to build a highly available, reliable, and scalable infrastructure for your application needs.

For further information please visit our [API documentation](https://cloudbreak-api.sequenceiq.com/api/index.html#/stack).

###Blueprints

Ambari Blueprints are a declarative definition of a Hadoop cluster. With a Blueprint, you specify a stack, the component layout and the configurations to materialize a Hadoop cluster instance. Hostgroups defined in blueprints can be associated to different templates, thus you can spin up a highly available cluster running on different instance types. This will give you the option to group your Hadoop services based on resource needs (e.g. high I/O, CPU or memory) and create an infrastructure which fits your workload best.

We have a few default blueprints available from multinode, streaming to analytic ones.

For further information please visit our [API documentation](https://cloudbreak-api.sequenceiq.com/api/index.html#/blueprints).

###Cluster

Clusters are materialized Hadoop services on a given infrastructure. They are built based on a Blueprint (running the components and services specified) and on a configured infrastructure Stack.
Once a cluster is created and launched, it can be used the usual way as any Hadoop cluster. We suggest to start with the Cluster's Ambari UI for an overview of your cluster.

For further information please visit our [API documentation](https://cloudbreak-api.sequenceiq.com/api/index.html#/cluster).
