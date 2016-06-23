package com.sequenceiq.cloudbreak.cloud.template

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.common.type.ResourceType

/**
 * Cloud providers which do not support template based deployments (like AWS Cloudformation, Azure ARM or OpenStack Heat)
 * this interface is used to create the network resources **individually**. Network resources are grouped by the [ResourceType].
 * For example to create the necessary network infrastructure for GCP, it is required to have the following network builders:
 * GCP_NETWORK
 * GCP_FIREWALL_INTERNAL
 * GCP_FIREWALL_IN
 * GCP_RESERVED_IP
 * These resource builders are ordered [OrderedBuilder.order] which means you can provide the
 * order of the resource creation. For instance creation it is most likely to need the resources created by an early builder so these resources should
 * be provided by the generic [ResourceBuilderContext] objects which will be passed along with the creation process.
 *
 *
 * To remove the corresponding network resources the builders will be called in **reverse** order. It the example above it will be called as:
 * GCP_RESERVED_IP
 * GCP_FIREWALL_IN
 * GCP_FIREWALL_INTERNAL
 * GCP_NETWORK
 *
 *
 * In order to make use of this interface and call the resource builders in ordered fashion the Cloud provider implementation should extend
 * [AbstractResourceConnector] which is a base implementation of [com.sequenceiq.cloudbreak.cloud.ResourceConnector]. Eventually all the
 * Cloud provider implementations use [com.sequenceiq.cloudbreak.cloud.ResourceConnector]. Providers which support some form of template deployments
 * should use that interface directly.
 */
interface NetworkResourceBuilder<C : ResourceBuilderContext> : CloudPlatformAware, OrderedBuilder, ResourceChecker<C> {

    /**
     * Create the reference [CloudResource] objects with proper resource naming to persist them into the DB. In the next phase these objects
     * will be provided to the [.build] method to actually create these
     * resources on the cloud provider. In case the resource creation fails the whole deployment fails, because the network type resources are not
     * replaceable. In that case the only option is to remove the stack.
     *
     *
     * There are some cases where you don't want to create some of the resources from the network stack, like when you use custom network or vpc or subnet. In
     * that case return the cloud resource with the existing resource id.

     * @param context Generic context object passed along with the flow to all methods. It is created by the [ResourceContextBuilder].
     * *
     * @param auth    Authenticated context is provided to be able to send the requests to the cloud provider.
     * *
     * @param network Network object provided which contains all the necessary information to create the proper network and subnet or the existing ones id.
     * *
     * @return Returns the buildable cloud resources.
     */
    fun create(context: C, auth: AuthenticatedContext, network: Network): CloudResource

    /**
     * This method will be called after the [.create] method with the constructed
     * cloud resources. It's purpose to actually create these resources on the cloud provider side.
     *
     *
     * There are some cases where you don't want to create some of the resources from the network stack, like when you use custom network or vpc or subnet. In
     * that case return the cloud resource with the existing resource id.

     * @param context  Generic context object passed along with the flow to all methods. It is created by the [ResourceContextBuilder].
     * *
     * @param auth     Authenticated context is provided to be able to send the requests to the cloud provider.
     * *
     * @param network  Network object provided which contains all the necessary information to create the proper network and subnet or the existing ones id.
     * *
     * @param security Security object represents the information to create the security rules and limit the accessibility of the cluster. Custom security
     * *                 rules can be created and used. It's form is provided in protocol, port sequence and cidr range (0.0.0.0/0).
     * *
     * @param resource Resource created earlier by the [.create].
     * *
     * @return Returns the created cloud resource which can be extended with extra information since the object itself is a dynamic model. These objects
     * * will be passed along with the extra information if it's provided so later it can be used to track the status of the deployment.
     * *
     * @throws Exception Exception can be thrown if the resource create request fails. It will result in stack failure since these resources are not
     * *                   replaceable.
     */
    @Throws(Exception::class)
    fun build(context: C, auth: AuthenticatedContext, network: Network, security: Security, resource: CloudResource): CloudResource

    /**
     * This functionality is not in use currently, but in the future it will be possible to update an existing resource.
     */
    @Throws(Exception::class)
    fun update(context: C, auth: AuthenticatedContext, network: Network, security: Security, resource: CloudResource): CloudResourceStatus

    /**
     * Responsible to delete the provided cloud resource by [.create] from the
     * cloud provider.
     *
     *
     * There are some cases where you didn't create some of the resources from the network stack, like when you use custom network or vpc or subnet. In
     * that case return **null**.

     * @param context  Generic context object passed along with the flow to all methods. It is created by the [ResourceContextBuilder].
     * *
     * @param auth     Authenticated context is provided to be able to send the requests to the cloud provider.
     * *
     * @param resource Resource created earlier by the [.create].
     * *
     * @param network  Network object which has to be deleted
     * *
     * @return Returns the deleted cloud resource which can be extended with extra information since the object itself is a dynamic model. These objects
     * * will be passed along with the extra information if it's provided so later it can be used to track the status of the deployment.
     * *
     * @throws Exception Exception can be thrown if the resource delete request fails. It will result in stack failure since these resources are not
     * *                   replaceable.
     */
    @Throws(Exception::class)
    fun delete(context: C, auth: AuthenticatedContext, resource: CloudResource, network: Network): CloudResource

    /**
     * Defines the type of the resource builder.

     * @return Return the resource type.
     */
    fun resourceType(): ResourceType

}
