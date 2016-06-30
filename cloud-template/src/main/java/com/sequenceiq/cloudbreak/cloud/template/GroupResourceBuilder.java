package com.sequenceiq.cloudbreak.cloud.template;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

/**
 * Cloud providers which do not support template based deployments (like AWS Cloudformation, Azure ARM or OpenStack Heat) this interface is used to create the
 * group
 * resources <b>individually</b>. Group resources are grouped by the {@link ResourceType}. These types of builders will be called after all the
 * {@link NetworkResourceBuilder} are finished.
 * For example to create an instance on the Google Cloud Platform it is required to create
 * - GCP_FIREWALL_IN
 * which means 1 different group resource builders. These resource builders are ordered {@link OrderedBuilder#order()} which means you have to provide the
 * order of the resource creation. In the example above on GCP first the root disk will be created after that the attached disks and then at the end the
 * actual instance will be created. For instance creation it is most likely to need the resources created by an early builder so these resources should
 * be provided by the generic {@link ResourceBuilderContext} objects which will be passed along with the creation process.
 * <p/>
 * To remove the corresponding group resources the builders will be called in <b>reverse</b> order. It the example above it will be called as:
 * - GCP_FIREWALL_IN
 * <p/>
 * In order to make use of this interface and call the resource builders in ordered fashion the cloud provider implementation should extend
 * {@link AbstractResourceConnector} which is a base implementation of {@link com.sequenceiq.cloudbreak.cloud.ResourceConnector}.
 * Eventually all the cloud provider implementations use {@link com.sequenceiq.cloudbreak.cloud.ResourceConnector}. Providers which support some form of
 * template deployments should use that interface directly.
 */
public interface GroupResourceBuilder<C extends ResourceBuilderContext> extends CloudPlatformAware, OrderedBuilder, ResourceChecker<C> {

    /**
     * Create the reference {@link CloudResource} objects with proper resource naming to persist them into the DB. In the next phase these objects
     * will be provided to the {@link #build(ResourceBuilderContext, AuthenticatedContext, Network, Security, CloudResource)} method to actually create these
     * resources on the cloud provider. In case the resource creation fails the whole deployment fails, because the network type resources are not
     * replaceable. In that case the only option is to remove the stack.
     * <p/>
     * There are some cases where you don't want to create some of the resources from the network stack, like when you use custom network or vpc or subnet. In
     * that case return the cloud resource with the existing resource id.
     *
     * @param context Generic context object passed along with the flow to all methods. It is created by the {@link ResourceContextBuilder}.
     * @param auth    Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param group   Compute resources which are required for a deployment. Each group represents an instance group in Cloudbreak. One group contains
     *                  multiple instance templates for the same instance type so this method supposed to create for 1 template at a time, because it will
     *                  be called as many times as there are templates in the group.
     * @param network Network object provided which contains all the necessary information to create the proper network and subnet or the existing ones id.
     * @return Returns the buildable cloud resources.
     */
    CloudResource create(C context, AuthenticatedContext auth, Group group, Network network);

    /**
     * This method will be called after the {@link #create(ResourceBuilderContext, AuthenticatedContext, Network)} method with the constructed
     * cloud resources. It's purpose to actually create these resources on the cloud provider side.
     * <p/>
     * There are some cases where you don't want to create some of the resources from the network stack, like when you use custom network or vpc or subnet. In
     * that case return the cloud resource with the existing resource id.
     *
     * @param context  Generic context object passed along with the flow to all methods. It is created by the {@link ResourceContextBuilder}.
     * @param auth     Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param group   Compute resources which are required for a deployment. Each group represents an instance group in Cloudbreak. One group contains
     *                  multiple instance templates for the same instance type so this method supposed to create for 1 template at a time, because it will
     *                  be called as many times as there are templates in the group.
     * @param network  Network object provided which contains all the necessary information to create the proper network and subnet or the existing ones id.
     * @param security Security object represents the information to create the security rules and limit the accessibility of the cluster. Custom security
     *                 rules can be created and used. It's form is provided in protocol, port sequence and cidr range (0.0.0.0/0).
     * @param resource Resource created earlier by the {@link #create(ResourceBuilderContext, AuthenticatedContext, Network)}.
     * @return Returns the created cloud resource which can be extended with extra information since the object itself is a dynamic model. These objects
     * will be passed along with the extra information if it's provided so later it can be used to track the status of the deployment.
     * @throws Exception Exception can be thrown if the resource create request fails. It will result in stack failure since these resources are not
     *                   replaceable.
     */
    CloudResource build(C context, AuthenticatedContext auth, Group group, Network network, Security security, CloudResource resource) throws Exception;

    /**
     * This functionality is not in use currently, but in the future it will be possible to update an existing resource.
     */
    CloudResourceStatus update(C context, AuthenticatedContext auth, Group group, Network network, Security security, CloudResource resource) throws Exception;

    /**
     * Responsible to delete the provided cloud resource by {@link #create(ResourceBuilderContext, AuthenticatedContext, Network)} from the
     * cloud provider.
     * <p/>
     * There are some cases where you didn't create some of the resources from the network stack, like when you use custom network or vpc or subnet. In
     * that case return <b>null</b>.
     *
     * @param context  Generic context object passed along with the flow to all methods. It is created by the {@link ResourceContextBuilder}.
     * @param auth     Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param resource Resource created earlier by the {@link #create(ResourceBuilderContext, AuthenticatedContext, Network)}.
     * @param network  Network object which has to be deleted
     * @return Returns the deleted cloud resource which can be extended with extra information since the object itself is a dynamic model. These objects
     * will be passed along with the extra information if it's provided so later it can be used to track the status of the deployment.
     * @throws Exception Exception can be thrown if the resource delete request fails. It will result in stack failure since these resources are not
     *                   replaceable.
     */
    CloudResource delete(C context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception;

    /**
     * Defines the type of the resource builder.
     *
     * @return Return the resource type.
     */
    ResourceType resourceType();

}