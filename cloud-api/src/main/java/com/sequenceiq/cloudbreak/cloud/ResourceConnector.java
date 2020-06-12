package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.AdjustmentType;

/**
 * Cloudbreak handles the entities on the Cloud provider side as generic resources and supports CRUD operations on them.
 * <br>
 * For example a resource from Cloudbreak point could one or more from the followings:
 * - HEAT_STACK,
 * - OPENSTACK_ATTACHED_DISK,
 * - OPENSTACK_NETWORK,
 * - OPENSTACK_SUBNET,
 * - GCP_DISK
 * - GCP_ATTACHED_DISK
 * - GCP_INSTANCE
 * <br>
 * Take a look at {@link com.sequenceiq.common.api.type.ResourceType} for more resource types.
 * <br>
 * Cloud providers which support template based deployments (like AWS Cloudformation, Azure ARM or OpenStack Heat) usually use only one {@link
 * CloudResource} which is  CLOUDFORMATION_STACK, HEAT_STACK or ARM_TEMPLATE and all the infrastructure related changes are done through that resource.
 * In other words when a new VM is removed from stack (cluster) then the CLoudbreak is not addressing that VM resource to be removed from stack, but uses the
 * the template resource reference e.g HEAT_STACK to inform the Cloud provider to remove the VM instance from stack.
 */
public interface ResourceConnector<R> {


    /**
     * Launch a complete stack on Cloud platform. The stack consist of the following resources:
     * - instances (organised in instance groups)
     * - volume(s) associated with instances
     * - vm image definition
     * - network
     * - security
     * - extra dynamic parameters
     * <br>
     * This method shall initiate the infrastructure creation on Cloud platform and shall return of a list of {@link CloudResourceStatus} values. It does not
     * need to wait/block until the infrastructure creation is finished, but it can return immediately and the {@link #check(AuthenticatedContext, List)}
     * method is invoked to check regularly whether the infrastructure and all resources have already been created or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     * @param persistenceNotifier  Cloud platform notifies the Cloudbreak over this interface if a resource is allocated on the Cloud platfrom
     * @param adjustmentType       defines the failure policy (i.e. what shall the cloudbpaltform do if not all of the VMs can be strarted)
     * @param threshold            threshold related adjustmentType
     * @return the status of resources allocated on Cloud platform
     * @throws Exception in case of any error
     */
    List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception;

    /**
     * Launches a database stack on a cloud platform. The stack consists of the following resources:
     * - a single database server instance
     * - depending on the platform, other associated, required resources (e.g., a DB subnet group for RDS)
     * <br>
     * This method initiates infrastructure creation on the cloud platform and returns a list of
     * {@link CloudResourceStatus} values, one for each created resource. The caller does not need
     * to wait/block until infrastructure creation is finished, but can return immediately and use
     * {@link #check(AuthenticatedContext, List)} method to check regularly whether the
     * infrastructure and all resources have been created or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     * @param persistenceNotifier  notifier for when a resource is allocated on the cloud platfrom
     * @return the status of resources allocated on the cloud platform
     * @throws Exception in case of any error
     */
    List<CloudResourceStatus> launchDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception;

    /**
     * Invoked to check whether the resources have already reached a StatusGroup.PERMANENT state.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param resources            the resources for which the status needs to be checked
     * @return the status of resources allocated on Cloud platform
     */
    List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources);

    /**
     * Delete the complete infrastructure from Cloud platform. It does not need to wait/block until the infrastructure termination is finished,
     * but it can return immediately and the {@link #check(AuthenticatedContext, List)} method is invoked to check regularly whether the infrastructure and
     * all resources have already been terminated or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     * @param cloudResources       the resources that needs to be terminated
     * @return the status of resources
     * @throws Exception in case of any error
     */
    List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> cloudResources) throws Exception;

    /**
     * Deletes the infrastructure for a database server from a cloud platform. This method initiates
     * infrastructure deletion on the cloud platform and returns a list of {@link CloudResourceStatus}
     * values, one for each terminated resource. The caller does not need to wait/block until
     * infrastructure deletion is finished, but can return immediately and use
     * {@link #check(AuthenticatedContext, List)} method to check regularly whether the infrastructure
     * and all resources have been deleted or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     * @param resources            contains list of resources to be terminated
     * @param persistenceNotifier  notifies the db to delete the resources
     * @param force                whether to continue termination even if infrastructure deletion fails
     * @return the status of resources terminated on the cloud platform
     * @throws Exception in case of any error
     */
    List<CloudResourceStatus> terminateDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            List<CloudResource> resources, PersistenceNotifier persistenceNotifier, boolean force) throws Exception;

    /**
     * Starts the database server. The caller does not need to wait/block until
     * database server gets started.
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack contains the full description of infrastructure
     * @throws Exception in case of any error
     */
    void startDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception;

    /**
     * Stops the database server. The caller does not need to wait/block until
     * database server gets stopped.
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack contains the full description of infrastructure
     * @throws Exception in case of any error
     */
    void stopDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception;

    /**
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack contains the full description of infrastructure
     * @return The status of the given database server instance
     * @throws Exception in case of any error
     */
    ExternalDatabaseStatus getDatabaseServerStatus(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception;

    /**
     * Update of infrastructure on Cloud platform. (e.g change Security groups). It does not need to wait/block until the infrastructure update is
     * finished, but it can return immediately and the {@link #check(AuthenticatedContext, List)} method is invoked to check regularly whether the
     * infrastructure and all resources have already been updated or not.
     * <br>
     * Note: this method is a bit generic at the moment, but complex changes like replace the existing network with the a new one or add/remove instances
     * are not executed over this method.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of the new infrastructure (e.g new security groups)
     * @param resources            resources that needs to be updated
     * @return the status of updated resources
     * @throws Exception in case of any error
     */
    List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) throws Exception;

    /**
     * Update of infrastructure on Cloud platform, add new instances. It does not need to wait/block until the infrastructure update is
     * finished, but it can return immediately and the {@link #check(AuthenticatedContext, List)} method is invoked to check regularly whether the
     * infrastructure and all resources have already been updated or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of the new infrastructure including new instances
     *                             ({@link com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate}) where the CREATE_REQUESTED status
     *                             {@link com.sequenceiq.cloudbreak.cloud.model.InstanceStatus} denotes that it is a new instance and needs to be created.
     * @param resources            resources that needs to be updated (e.g HEAT_TEMPLATE)
     * @return the status of updated resources
     */
    List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources);

    /**
     * Update of infrastructure on Cloud platform, delete instances. It does not need to wait/block until the infrastructure update is
     * finished, but it can return immediately and the {@link #check(AuthenticatedContext, List)} method is invoked to check regularly whether the
     * infrastructure and all resources have already been updated or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of the new infrastructure including the instances tha needs to be deleted
     *                             ({@link com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate}) where the DELETE_REQUESTED status
     *                             {@link com.sequenceiq.cloudbreak.cloud.model.InstanceStatus} denotes that it is an instance that needs to be terminated.
     * @param resources            resources that needs to be updated (e.g HEAT_TEMPLATE)
     * @param vms                  the {@link CloudInstance}s are listed that needs to be deleted
     * @param resourcesToRemove    previously collected resources to remove
     * @return the status of updated resources
     */
    List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext,
            CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms, R resourcesToRemove);

    /**
     * Collects resources to remove before do exact downscale
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of the new infrastructure including the instances tha needs to be deleted
     *                             ({@link com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate}) where the DELETE_REQUESTED status
     *                             {@link com.sequenceiq.cloudbreak.cloud.model.InstanceStatus} denotes that it is an instance that needs to be terminated.
     * @param resources            resources that needs to be updated (e.g HEAT_TEMPLATE)
     * @param vms                  the {@link CloudInstance}s are listed that needs to be deleted
     * @return the status of updated resources
     */
    R collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms);

    /**
     * Gets the Cloud platform related tls info.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @return the platform related tls info
     */

    TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack);

    /**
     * Gets the Cloud platform related stack template
     *
     * @return the platform related stack template
     * @throws TemplatingDoesNotSupportedException if template not supported by provider
     */
    String getStackTemplate() throws TemplatingDoesNotSupportedException;

    /**
     * Gets the cloud platform related database stack template.
     *
     * @return the platform related database stack template
     * @throws TemplatingDoesNotSupportedException if templating is not supported by provider
     */
    String getDBStackTemplate() throws TemplatingDoesNotSupportedException;
}
