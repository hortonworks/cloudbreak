package com.sequenceiq.cloudbreak.cloud.template;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Cloud providers which do not support template based deployments (like AWS Cloudformation, Azure ARM) this interface is used to create the
 * compute
 * resources <b>individually</b>. Compute resources are grouped by the {@link ResourceType}. These types of builders will be called after all the
 * {@link GroupResourceBuilder} are finished.
 * For example to create an instance on the Google Cloud Platform it is required to create
 * - GCP_F
 * - GCP_ATTACHED_DISK
 * - GCP_INSTANCE
 * which means 3 different compute resource builders. These resource builders are ordered {@link OrderedBuilder#order()} which means you have to provide the
 * order of the resource creation. In the example above on GCP first the root disk will be created after that the attached disks and then at the end the
 * actual instance will be created. For instance creation it is most likely to need the resources created by an early builder so these resources should
 * be provided by the generic {@link ResourceBuilderContext} objects which will be passed along with the creation process.
 * <br>
 * To remove the corresponding compute resources the builders will be called in <b>reverse</b> order. It the example above it will be called as:
 * - GCP_INSTANCE
 * - GCP_ATTACHED_DISK
 * - GCP_DISK
 * It is possible that the instance deletion will delete all the resources as well which is a normal scenario, see the documentation on the delete method.
 * <br>
 * These type of resources <b>can be rolled back</b>. It means if the creation of an instance fails does not automatically mean that the whole deployment
 * fails. The failure policy can determine when to rollback the whole deployment {@link com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler}.
 * The rollback is handled automatically by Cloudbreak by calling the appropriate delete methods of the different resource builders.
 * <br>
 * In order to make use of this interface and call the resource builders in ordered fashion the cloud provider implementation should extend
 * {@link AbstractResourceConnector} which is a base implementation of {@link com.sequenceiq.cloudbreak.cloud.ResourceConnector}.
 * Eventually all the cloud provider implementations use {@link com.sequenceiq.cloudbreak.cloud.ResourceConnector}. Providers which support some form of
 * template deployments should use that interface directly.
 */
public interface ComputeResourceBuilder<C extends ResourceBuilderContext> extends CloudPlatformAware, OrderedBuilder, ResourceChecker<C> {

    /**
     * Create the reference {@link CloudResource} objects with proper resource naming to persist them into the DB. In the next phase these objects
     * will be provided to the {@link #build(ResourceBuilderContext, long, AuthenticatedContext, Group, List, CloudStack)} method to actually create these
     * resources on the cloud provider. In case the resource creation fails it will be rolled back using the resource name as a reference. To provide
     * resource names implement the {@link com.sequenceiq.cloudbreak.cloud.service.ResourceNameService} interface and inject it to the implementation
     * using {@link javax.inject.Inject}.
     *
     * @param context   Generic context object passed along with the flow to all methods. It is created by the {@link ResourceContextBuilder}.
     * @param privateId Each compute resource is grouped by a private id used by Cloudbreak only. It is generally advised to include the private id
     *                  in the resource name for easier identification. For example if an instance is rolled back all the compute resources will be rolled
     *                  back with the same private id to clean up the corresponding resources.
     * @param auth      Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param group     Compute resources which are required for a deployment. Each group represents an instance group in Cloudbreak. One group contains
     *                  multiple instance templates for the same instance type so this method supposed to create for 1 template at a time, because it will
     *                  be called as many times as there are templates in the group.
     * @param image     Base cloud image for the deployment.
     * @return Returns the buildable cloud resources. If this resource builder is responsible to create the attached disks for an instance it is expected
     * to return multiple cloud resources. If this builder is responsible for the instance creation it should return 1 resource.
     */
    List<CloudResource> create(C context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image);

    /**
     * This method will be called after the {@link #create(ResourceBuilderContext, long, AuthenticatedContext, Group, Image)} method with the constructed
     * cloud resources. It's purpose to actually create these resources on the cloud provider side.
     *
     * @param context           Generic context object passed along with the flow to all methods. It is created by the {@link ResourceContextBuilder}.
     * @param privateId         Each compute resource is grouped by a private id used by Cloudbreak only.
     * @param auth              Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param group             Compute resources which are required for a deployment. Each group represents an instance group in Cloudbreak. One group contains
     *                          multiple instance templates for the same instance type so this method supposed to create for 1 template at a time, because it
     *                          will be called as many times as there are templates in the group.
     * @param buildableResource Resources created earlier by the {@link #create(ResourceBuilderContext, long, AuthenticatedContext, Group, Image)}. If this
     *                          builder is responsible to create the instance the list will contain 1 resource. It does not contain all the required resources
     *                          for the instance, like the attached disks or network interface. Those resources should be retrievable from the context object
     *                          by private id.
     * @param cloudStack        The entire cloud stack object for deployment
     * @return Returns the created cloud resources which can be extended with extra information since the object itself is a dynamic model. These objects
     * will be passed along with the extra information if it's provided so later it can be used to track the status of the deployment.
     * @throws Exception Exception can be thrown if the resource creation request fails for some reason and then the resources will be rolled back.
     */
    List<CloudResource> build(C context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception;

    List<CloudResource> update(C context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            CloudStack cloudStack) throws Exception;

    /**
     * This method will be called if an instance stop/start is requested to check the state of the instance.
     *
     * @param context   Generic context object passed along with the flow to all methods. It is created by the {@link ResourceContextBuilder}. It is supposed
     *                  to provide whether the instances should reach the stop or the start state.
     * @param auth      Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param instances List of instances to check their states.
     * @return It return the state of the requested instances.
     */
    List<CloudVmInstanceStatus> checkInstances(C context, AuthenticatedContext auth, List<CloudInstance> instances);

    /**
     * Responsible to delete the provided cloud resource by {@link #create(ResourceBuilderContext, long, AuthenticatedContext, Group, Image)} from the
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
     * Responsible to start the specified instance. If this resource builder is responsible for resource creation which cannot be started or stopped
     * (like disks) simply return <b>null</b> and the state won't be checked later.
     *
     * @param context  Generic context object passed along with the flow to all methods.
     * @param auth     Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param instance The instance which shall be started.
     * @return Returns the initial state of the cloud instance start. If it returns null or a persistent state
     * {@link com.sequenceiq.cloudbreak.cloud.model.InstanceStatus} the {@link #checkInstances(ResourceBuilderContext, AuthenticatedContext, List)}
     * method won't be called.
     */
    CloudVmInstanceStatus start(C context, AuthenticatedContext auth, CloudInstance instance);

    /**
     * Responsible to stop the specified instance. If this resource builder is responsible for resource creation which cannot be started or stopped
     * (like disks) simply return null and the state won't be checked later.
     *
     * @param context  Generic context object passed along with the flow to all methods.
     * @param auth     Authenticated context is provided to be able to send the requests to the cloud provider.
     * @param instance The instance which shall be stopped.
     * @return Returns the initial state of the cloud instance stop. If it returns null or a persistent state
     * {@link com.sequenceiq.cloudbreak.cloud.model.InstanceStatus} the {@link #checkInstances(ResourceBuilderContext, AuthenticatedContext, List)}
     * method won't be called.
     */
    CloudVmInstanceStatus stop(C context, AuthenticatedContext auth, CloudInstance instance);

    /**
     * Defines the type of the resource builder.
     *
     * @return Return the resource type.
     */
    ResourceType resourceType();

}