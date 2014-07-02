##How it works?

Cloudbreak launches on-demand Hadoop clusters on your favorite cloud provider in minutes. We have introduced 4 main notions - the core building block of the REST API. 

###Templates

A template gives developers and systems administrators an easy way to create and manage a collection of cloud infrastructure related resources, maintaining and updating them in an orderly and predictable fashion.
Templates are cloud specific - and on top of the infrastructural setup they collect the information such as the used machine images, the datacenter location, instance types and SSH setup.

The infrastructure specific configuration is available under the Cloudbreak [resources](https://github.com/sequenceiq/cloudbreak/blob/master/src/main/resources/vpc-and-subnet.template).
As an example, for Amazon EC2, we use [AWS Cloudformation](http://aws.amazon.com/cloudformation/) to define the cloud infrastructure .

For further information please visit our [API documentation](http://docs.cloudbreak.apiary.io/#templates).

###Stacks

Stacks are template instances - a runnig cloud infrastructure created based on a template. Stacks are always launched on behalf of a cloud user account.

###Blueprint

###Cluster
