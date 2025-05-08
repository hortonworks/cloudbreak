package com.sequenceiq.cloudbreak.cloud.template;


import java.util.List;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Cloud providers which do not support template based deployments (like AWS Cloudformation, Azure ARM)
 * this interface is used to create the load balancer resources <b>individually</b>. loadbalancer resources are grouped by the {@link ResourceType}.
 * For example to create the necessary network infrastructure for GCP, it is required to have the following network builders:
 * GCP_HEALTHCHECK
 * GCP_BACKEND_SERVICE
 * GCP_FORWARDING_RULE
 * These resource builders are ordered {@link OrderedBuilder#order()} which means you can provide the
 * order of the resource creation. For instance creation it is most likely to need the resources created by an early builder so these resources should
 * be provided by the generic {@link ResourceBuilderContext} objects which will be passed along with the creation process.
 * <br>
 * To remove the corresponding network resources the builders will be called in <b>reverse</b> order. It the example above it will be called as:
 * GCP_FORWARDING_RULE
 * GCP_BACKEND_SERVICE
 * GCP_HEALTHCHECK
 * <br>
 * In order to make use of this interface and call the resource builders in ordered fashion the Cloud provider implementation should extend
 * {@link AbstractResourceConnector} which is a base implementation of {@link com.sequenceiq.cloudbreak.cloud.ResourceConnector}. Eventually all the
 * Cloud provider implementations use {@link com.sequenceiq.cloudbreak.cloud.ResourceConnector}. Providers which support some form of template deployments
 * should use that interface directly.
 */
public interface LoadBalancerResourceBuilder<C extends ResourceBuilderContext> extends CloudPlatformAware, OrderedBuilder, ResourceChecker<C> {

    /**
     * Create the reference {@link CloudResource} objects with proper resource naming to persist them into the DB. In the next phase these objects
     * will be provided to the {@link #build(ResourceBuilderContext, AuthenticatedContext, List, CloudLoadBalancer, CloudStack)} method to actually create these
     * resources on the cloud provider. In case the resource creation fails it will be rolled back using the resource name as a reference. To provide
     * resource names implement the {@link com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService} interface and inject it to the implementation
     * using {@link jakarta.inject.Inject}.
     *
     * @param context       Generic context object passed along with the flow to all methods. It is created by the {@link ResourceContextBuilder}.
     * @param auth          Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param loadBalancer  LoadBalancer object with all information required to create and address the Resource to be built.
     * @param network       Network object which is stored network related information.
     * @return Returns the buildable cloud resources. If this resource builder is required to create multiple resources to satisfy this load balancer
     *                      then it should return a CloudResource for each.
     */
    List<CloudResource> create(C context, AuthenticatedContext auth, CloudLoadBalancer loadBalancer, Network network);

    /**
     * This method will be called after the {@link #create(ResourceBuilderContext, AuthenticatedContext, CloudLoadBalancer, Network)} method
     * with the constructed cloud resources. It's purpose to actually create these resources on the cloud provider side.
     *
     * @param context            Generic context object passed along with the flow to all methods. It is created by the {@link ResourceContextBuilder}.
     * @param auth               Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param loadBalancer       Loadbalancer object with all information required to create and address the Resource to be built.
     * @param buildableResources Resources created earlier by the {@link #create(ResourceBuilderContext, AuthenticatedContext, CloudLoadBalancer, Network)}.
     *                           If this builder is responsible to create the instance the list will contain 1 resource.
     * @param cloudStack         The entire cloud stack object for deployment
     * @return Returns the created cloud resources which can be extended with extra information since the object itself is a dynamic model. These objects
     * will be passed along with the extra information if it's provided so later it can be used to track the status of the deployment.
     * @throws Exception Exception can be thrown if the resource creation request fails for some reason and then the resources will be rolled back.
     */
    List<CloudResource> build(C context, AuthenticatedContext auth, List<CloudResource> buildableResources,
            CloudLoadBalancer loadBalancer, CloudStack cloudStack)
            throws Exception;

    /**
     * Responsible to delete the provided cloud resource by {@link #create(ResourceBuilderContext, AuthenticatedContext, CloudLoadBalancer)} from the
     * cloud provider. If the specific resource is automatically deleted by a different resource builder (for example on Google Cloud Platform the
     * attached disks will be automatically deleted if the instance delete is requested) simply return <b>null</b> and the status of the deletion won't
     * be checked.
     *
     * @param context  Generic context object passed along with the flow to all methods.
     * @param auth     Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param resource Cloud resource which shall to be deleted.
     * @return Returns the deleted cloud resource which can be extended with extra information since the object itself is a dynamic model. This object
     * will be passed along with the extra information if it's provided so later it can be used to track the status of the deletion.
     * @throws Exception Exception can be thrown if the resource deletion request fails for some reason. In this case the deletion will fails, but later it
     *                   can be issued again.
     */
    CloudResource delete(C context, AuthenticatedContext auth, CloudResource resource) throws Exception;

    /**
     * Defines the type of the resource builder.
     *
     * @return Return the resource type.
     */
    ResourceType resourceType();

}
