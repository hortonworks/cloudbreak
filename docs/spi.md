#Service Provider Interface (SPI)

Cloudbreak already supports multiple cloud platforms and provides an easy way to integrate a new provider trough [Cloudbreak's Service Provider Interface (SPI)](https://github.com/sequenceiq/cloudbreak/tree/master/cloud-api) which is a plugin mechanism to enable a seamless integration of any cloud provider.

The SPI plugin mechanism has been used to integrate all existing providers to Cloudbreak, therefore if a new provider is integrated it immediately becomes a first class citizen in Cloudbreak.
 
 * [cloud-aws](https://github.com/sequenceiq/cloudbreak/tree/master/cloud-aws) module integrates Amazon Web Services
 * [cloud-gcp](https://github.com/sequenceiq/cloudbreak/tree/master/cloud-gcp) module integrates Google Cloud Platform
 * [cloud-arm](https://github.com/sequenceiq/cloudbreak/tree/master/cloud-arm) module integrates Microsoft Azure
 * [cloud-openstack](https://github.com/sequenceiq/cloudbreak/tree/master/cloud-openstack) module integrates OpenStack

The SPI interface is event based, scales well and decoupled from Cloudbreak. The core of Cloudbreak is communicating trough [EventBus](http://projectreactor.io/) with providers, but the complexity of Event handling is hidden from the provider implementation.

##Resource management

There are two kind of deployment/resource management method is supported by cloud providers:

* template based deployments
* individual resource based deployments

Cloudbreak's SPI supports both way of resource management. It provides a well defined interfaces, abstract classes and helper classes like scheduling and polling of resources to aid the integration and to avoid any boilerplate code in the module of cloud provider.

##Template based deployments

Providers with template based deployments like [AWS CloudFormation](https://aws.amazon.com/cloudformation/), [Azure ARM](https://azure.microsoft.com/en-us/documentation/articles/resource-group-overview/#) or [OpenStack Heat](https://wiki.openstack.org/wiki/Heat) have the ability to create and manage a collection of related cloud resources, provisioning and updating them in an orderly and predictable fashion. This means that Cloudbreak needs a reference to the template itself and every change in the infrastructure (e.g creating new instance or deleting one) is managed through this templating mechanism.

If the provider has templating support then the provider's [gradle](http://gradle.org/) module shall depend on the [cloud-api](https://github.com/sequenceiq/cloudbreak/tree/master/cloud-api) module.

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

The entry point of the provider is the  [CloudConnector](https://github.com/sequenceiq/cloudbreak/blob/master/cloud-api/src/main/java/com/sequenceiq/cloudbreak/cloud/CloudConnector.java) interface and every interface that needs to be implemented is reachable trough this interface.

##Individual resource based deployments

Providers like GCP that does not support suitable templating mechanism or for customisable providers like OpenStack where the Heat Orchestration (templating) component optional the individual resources needs to be handlet separately. This means that resources like networks, discs and compute instances needs to be created and managed with an ordered sequence of API calls and Cloudbreak shall provide a solution to manage the collection of related cloud resources together.

If the provider has no templating support then the provider's [gradle](http://gradle.org/) module shall depend on the [cloud-template](https://github.com/sequenceiq/cloudbreak/tree/master/cloud-template) module, that includes Cloudbreak defined abstract template. This template is a set of abstract and utility classes to support provisioning and updating related resources in an orderly and predictable manner trough ordered sequences of cloud API calls.

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

##Variants

OpenStack is very modular and allows to install different components for e.g. volume storage or different components for networking (e.g. Nova networking or Neutron) or even you have a chance that some components like Heat are not installed at all.

Cloudbreak's SPI interface reflects this flexibility using so called variants. This means that if some part of cloud provider (typically OpenStack) is using different component you don't need re-implement the complete stack but just use a different variant and re-implement the part what is different.

The reference implementation for this feature can be found in  [cloud-openstack](https://github.com/sequenceiq/cloudbreak/tree/master/cloud-openstack) module which support a HEAT and NATIVE variants. The HEAT variant utilizes the Heat templating to launch a stack, but the NATIVE variant starts the cluster by using a sequence of API calls without Heat to achieve the same result, although both of them are using the same authentication and credential management.

##Development

In order to set up a development environment please take a look at [Local Development Setup](https://github.com/sequenceiq/cloudbreak/blob/master/docs/dev/development.md) documentation.
