# Service Provider Interface (SPI)

Cloudbreak already supports multiple cloud platforms and provides an easy way to integrate a new provider trough [Cloudbreak's Service Provider Interface (SPI)](https://github.com/hortonworks/cloudbreak/tree/master/cloud-api), a plugin mechanism that enables seamless integration of any cloud provider. The SPI plugin mechanism has been used to integrate all currently supported providers with Cloudbreak. Consequently, if you use SPI to integrate a new provider, the integration will be seamless.
 
 * The [cloud-aws-cloudformation](https://github.com/hortonworks/cloudbreak/tree/master/cloud-aws-cloudformation) module integrates Amazon Web Services
 * The [cloud-gcp](https://github.com/hortonworks/cloudbreak/tree/master/cloud-gcp) module integrates Google Cloud Platform
 * The [cloud-azure](https://github.com/hortonworks/cloudbreak/tree/master/cloud-azure) module integrates Microsoft Azure
 * The [cloud-openstack](https://github.com/hortonworks/cloudbreak/tree/master/cloud-openstack) module integrates OpenStack

The SPI interface is event-based, it scales well, and is decoupled from Cloudbreak. The core of Cloudbreak uses [EventBus](http://projectreactor.io/) to communicate with the providers, but the complexity of event handling is hidden from the provider implementation.

## Resource Management

Cloud providers support two kinds of deployment and resource management methods:

* Template-based deployments
* Individual resource-based deployments

Cloudbreak's SPI supports both of these methods. It provides a well-defined interfaces, abstract classes, and helper classes, scheduling and polling of resources to aid the integration and to avoid any boilerplate code in the module of cloud provider.

### Template Based Deployments

Providers with template-based deployments like [AWS CloudFormation](https://aws.amazon.com/cloudformation/), [Azure ARM](https://azure.microsoft.com/en-us/documentation/articles/resource-group-overview/#) or [OpenStack Heat](https://wiki.openstack.org/wiki/Heat) have the ability to create and manage a collection of related cloud resources, provisioning and updating them in an orderly and predictable fashion. 

In such scenario, Cloudbreak needs a reference to the template itself because every change in the infrastructure (for example, creating new instance or deleting one) is managed through this templating mechanism.

If a provider has templating support, then the provider's [gradle](http://gradle.org/) module depends on the [cloud-api](https://github.com/hortonworks/cloudbreak/tree/master/cloud-api) module:

```
apply plugin: 'java'

sourceCompatibility = 1.7

repositories {
    mavenCentral()
}

jar {
    baseName = 'cloud-new-provider'
}

dependencies {

    compile project(':cloud-api')

}
```

The entry point for the provider is the  [CloudConnector](https://github.com/hortonworks/cloudbreak/blob/master/cloud-api/src/main/java/com/sequenceiq/cloudbreak/cloud/CloudConnector.java) interface and every interface that needs to be implemented is reachable trough this interface.

### Individual Resource Based Deployments

There are providers such as GCP that do not support suitable templating mechanism, and customisable providers such as OpenStack where the Heat Orchestration (templating) component is optional and individual resources need to be handled separately. 

In such scenarios, resources such as networks, discs, and compute instances need to be created and managed with an ordered sequence of API calls, and Cloudbreak needs to provide a solution to manage the collection of related cloud resources as a whole.

If the provider has no templating support, then the provider's [gradle](http://gradle.org/) module typically depends on the [cloud-template](https://github.com/hortonworks/cloudbreak/tree/master/cloud-template) module, which includes Cloudbreak defined abstract template. This template is a set of abstract and utility classes to support provisioning and updating related resources in an orderly and predictable manner trough ordered sequences of cloud API calls:

```
apply plugin: 'java'

sourceCompatibility = 1.7

repositories {
    mavenCentral()
}

jar {
    baseName = 'cloud-new-provider'
}

dependencies {

    compile project(':cloud-template')

}
```

## Variants

OpenStack is highly modular. It allows you to install different components, for example for volume storage or networking (Nova networking, Neutron, etc.). Or, in some scenarios, some components such as Heat may not installed at all.

Cloudbreak's SPI interface reflects this flexibility using so called variants. This means that if some part of cloud provider (typically OpenStack) is using different component, you don't need re-implement the complete stack but just use a different variant and re-implement the part that is different.

The reference implementation for this feature can be found in  [cloud-openstack](https://github.com/hortonworks/cloudbreak/tree/master/cloud-openstack) module which support a HEAT and NATIVE variants. The HEAT variant utilizes the Heat templating to launch a stack, but the NATIVE variant starts the cluster by using a sequence of API calls without Heat to achieve the same result, although both of them are using the same authentication and credential management.
