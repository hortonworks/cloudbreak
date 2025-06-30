package com.sequenceiq.cloudbreak.cloud;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.model.database.CloudDatabaseServerSslCertificate;
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * Cloudbreak handles the entities on the Cloud provider side as generic resources and supports CRUD operations on them.
 * <br>
 * For example a resource from Cloudbreak point could one or more from the followings:
 * - GCP_DISK
 * - GCP_ATTACHED_DISK
 * - GCP_INSTANCE
 * <br>
 * Take a look at {@link com.sequenceiq.common.api.type.ResourceType} for more resource types.
 * <br>
 * Cloud providers which support template based deployments (like AWS Cloudformation, Azure ARM) usually use only one {@link
 * CloudResource} which is  CLOUDFORMATION_STACK, HEAT_STACK or ARM_TEMPLATE and all the infrastructure related changes are done through that resource.
 * In other words when a new VM is removed from stack (cluster) then the Cloudbreak is not addressing that VM resource to be removed from stack, but uses
 * the template resource reference e.g HEAT_STACK to inform the Cloud provider to remove the VM instance from stack.
 */
public interface ResourceConnector {

    Logger LOGGER = LoggerFactory.getLogger(ResourceConnector.class);

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
     * @param authenticatedContext      the authenticated context which holds the client object
     * @param stack                     contains the full description of infrastructure
     * @param persistenceNotifier       Cloud platform notifies the Cloudbreak over this interface if a resource is allocated on the Cloud platform
     * @param adjustmentTypeWithThreshold   defines the failure policy (i.e. what shall the cloudplatform do if not all of the VMs can be started)

     * @return the status of resources allocated on Cloud platform
     * @throws Exception in case of any error
     */
    List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws Exception;

    /**
     * Updates an existing stack with one or more load balancers, if the load balancers do not already exist. This method will initiate the
     * creation of load balancers on the Cloud platform, and will configure the load balancer routing in accordance with the specified
     * target group configuration. It returns a list of CloudResourceStatus for any created load balancers.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     * @param persistenceNotifier  Cloud platform notifies the Cloudbreak over this interface if a resource is allocated on the Cloud platform
     * @return the status of load balancers allocated on Cloud platform
     * @throws Exception in case of any error
     */
    List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception;

    /**
     * Updates the load balancers routing in accordance with the specified target group configuration.
     * If the load balancers do not already exist, then creating them with their dependencies.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     * @param persistenceNotifier  Cloud platform notifies the Cloudbreak over this interface if a resource is allocated on the Cloud platform
     * @return the status of load balancers allocated on Cloud platform
     * @throws Exception in case of any error
     */
    default List<CloudResourceStatus> updateLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception {
        return List.of();
    }

    /**
     * Delete load balancers for a stack
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     */
    default void deleteLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack, List<String> loadBalancersToRemove) {
        throw new UnsupportedOperationException("Load balancer removal is not supported for this provider.");
    }

    /**
     * Describe load balancers for a stack
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     */
    default List<CloudLoadBalancer> describeLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudLoadBalancerMetadata> loadBalancers) {
        throw new UnsupportedOperationException("Describing load balancers is not supported for this provider.");
    }

    /**
     * Detach public IP addresses from VMs
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     */
    default void detachPublicIpAddressesForVMsIfNotPrivate(AuthenticatedContext authenticatedContext, CloudStack stack) {
        throw new UnsupportedOperationException("Detaching public IP addresses is not supported for this provider.");
    }

    /**
     * Attach public IP addresses for VMs
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     */
    default List<CloudResource> attachPublicIpAddressesForVMsAndAddLB(AuthenticatedContext authenticatedContext, CloudStack stack,
            PersistenceNotifier persistenceNotifier) {
        throw new UnsupportedOperationException("Attaching public IP addresses and LBs is not supported for this provider.");
    }

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
     * @param persistenceNotifier  notifier for when a resource is allocated on the cloud platform
     * @return the status of resources allocated on the cloud platform
     * @throws Exception in case of any error
     */
    List<CloudResourceStatus> launchDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, PersistenceNotifier persistenceNotifier)
            throws Exception;

    /**
     * Validates that the upgrade of a database stack on a cloud platform is possible. The stack consists of the following resources:
     * - a single database server instance
     * - depending on the platform, other associated, required resources (e.g., a DB subnet group for RDS)
     * <br>
     * This method validates the database stack upgrade on the cloud platform and throws Exception in case of any validation error.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     * @param targetMajorVersion   target major version of the database
     * @throws Exception in case of any error
     */
    void validateUpgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack stack, TargetMajorVersion targetMajorVersion) throws Exception;

    /**
     * This method launches the necessary RDS instance and related cloud resources on the cloud platform to validate the database stack
     * upgrade if necessary and throws Exception in case of any validation error.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     * @param targetMajorVersion   target major version of the database
     * @param migratedDbStack      contains the parameters of the migrated database, only relevant in case of change in form factors
     * @param persistenceNotifier  notifier for when a resource is allocated on the cloud platform
     * @return the status of resources created for validation on the cloud platform
     * @throws Exception in case of any error
     */
    List<CloudResourceStatus> launchValidateUpgradeDatabaseServerResources(AuthenticatedContext authenticatedContext, DatabaseStack stack,
            TargetMajorVersion targetMajorVersion, DatabaseStack migratedDbStack, PersistenceNotifier persistenceNotifier) throws Exception;

    /**
     * This method cleans up the canary RDS instance and related cloud resources on the cloud platform that are used to validate the database stack
     * upgrade if necessary and throws Exception in case of any validation error.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     * @param resources            the resources that need to be cleaned up
     * @param persistenceNotifier  notifier for when a resource is allocated on the cloud platform
     * @throws Exception in case of any error
     */
    void cleanupValidateUpgradeDatabaseServerResources(AuthenticatedContext authenticatedContext, DatabaseStack stack, List<CloudResource> resources,
            PersistenceNotifier persistenceNotifier) throws Exception;

    /**
     * Upgrades a database stack on a cloud platform. The stack consists of the following resources:
     * - a single database server instance
     * - depending on the platform, other associated, required resources (e.g., a DB subnet group for RDS)
     * <br>
     * This method initiates infrastructure upgrade on the cloud platform and returns a list of
     * {@link CloudResourceStatus} values, one for each created resource. The caller does not need
     * to wait/block until infrastructure upgrade is finished, but can return immediately and use
     * {@link #check(AuthenticatedContext, List)} method to check regularly whether the
     * infrastructure and all resources have been upgraded or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of infrastructure
     * @param persistenceNotifier  notifier for when a resource is allocated on the cloud platform
     * @throws Exception in case of any error
     */
    void upgradeDatabaseServer(AuthenticatedContext authenticatedContext, DatabaseStack originalStack, DatabaseStack stack,
            PersistenceNotifier persistenceNotifier, TargetMajorVersion targetMajorVersion, List<CloudResource> resources) throws Exception;

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
     * Determines the status of a database server.
     * @param authenticatedContext the authenticated context which holds the client object; must not be {@code null}
     * @param stack contains the full description of infrastructure; must not be {@code null}
     * @return The status of the given database server instance; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     * @throws Exception in case of any error
     */
    ExternalDatabaseStatus getDatabaseServerStatus(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception;

    /**
     * Collect database related parameters of a database server.
     * @param authenticatedContext the authenticated context which holds the client object; must not be {@code null}
     * @param stack contains the full description of infrastructure; must not be {@code null}
     * @return The database parameters of the given database server instance; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     * @throws Exception in case of any error
     */
    default ExternalDatabaseParameters getDatabaseServerParameters(AuthenticatedContext authenticatedContext, DatabaseStack stack) throws Exception {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Queries the SSL root certificate currently active for a database server.
     * @param authenticatedContext the authenticated context which holds the client object; must not be {@code null}
     * @param stack contains the full description of infrastructure; must not be {@code null}
     * @return The active SSL root certificate of the given database server instance, or {@code null} if the database server does not exist anymore
     * @throws NullPointerException if either argument is {@code null}
     * @throws Exception in case of any error
     */
    default CloudDatabaseServerSslCertificate getDatabaseServerActiveSslRootCertificate(AuthenticatedContext authenticatedContext, DatabaseStack stack)
            throws Exception {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Check the possibility of update of infrastructure on Cloud platform. (e.g change Security groups).
     * <br>
     * Note: this method is a bit generic at the moment, but complex changes like replace the existing network with a new one or add/remove instances
     * are not executed over this method.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of the new infrastructure (e.g new security groups)
     * @param resources            resources that need to be updated
     * @throws Exception in case if update is not possible
     */
    void checkUpdate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) throws Exception;

    /**
     * Update of infrastructure on Cloud platform. (e.g change Security groups). It does not need to wait/block until the infrastructure update is
     * finished, but it can return immediately and the {@link #check(AuthenticatedContext, List)} method is invoked to check regularly whether the
     * infrastructure and all resources have already been updated or not.
     * <br>
     * Note: this method is a bit generic at the moment, but complex changes like replace the existing network with a new one or add/remove instances
     * are not executed over this method.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of the new infrastructure (e.g new security groups)
     * @param resources            resources that need to be updated
     * @return the status of updated resources
     * @throws Exception in case of any error
     */
    List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            UpdateType type, Optional<String> group)
            throws Exception;

    /**
     * Update user data for the cluster instances which contains parameters for user-data script run during instances' bootstrap.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of the new infrastructure (e.g new security groups)
     * @param userData             the user data by instance group type
     * @throws Exception in case of any error
     */
    void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, Map<InstanceGroupType, String> userData)
            throws Exception;

    /**
     * Update of infrastructure on Cloud platform, add new instances. It does not need to wait/block until the infrastructure update is
     * finished, but it can return immediately and the {@link #check(AuthenticatedContext, List)} method is invoked to check regularly whether the
     * infrastructure and all resources have already been updated or not.
     *
     * @param authenticatedContext      the authenticated context which holds the client object
     * @param stack                     contains the full description of the new infrastructure including new instances
     *                                  ({@link com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate}) where the CREATE_REQUESTED status
     *                                  {@link com.sequenceiq.cloudbreak.cloud.model.InstanceStatus} denotes that it is a new instance and needs to be created.
     * @param resources                 resources that need to be updated (e.g HEAT_TEMPLATE)
     * @param adjustmentTypeWithThreshold   defines the failure policy (i.e. what shall the cloudplatform do if not all of the VMs can be started)
     * @return the status of updated resources
     */
    List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws QuotaExceededException;

    /**
     * Update of infrastructure on Cloud platform, delete instances. It does not need to wait/block until the infrastructure update is
     * finished, but it can return immediately and the {@link #check(AuthenticatedContext, List)} method is invoked to check regularly whether the
     * infrastructure and all resources have already been updated or not.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of the new infrastructure including the instances tha needs to be deleted
     *                             ({@link com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate}) where the DELETE_REQUESTED status
     *                             {@link com.sequenceiq.cloudbreak.cloud.model.InstanceStatus} denotes that it is an instance that needs to be terminated.
     * @param resources            resources that need to be updated (e.g HEAT_TEMPLATE)
     * @param vms                  the {@link CloudInstance}s are listed that needs to be deleted
     * @param resourcesToRemove    previously collected resources to remove
     * @return the status of updated resources
     */
    List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext,
            CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms, List<CloudResource> resourcesToRemove);

    /**
     * Collects resources to remove before do exact downscale
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param stack                contains the full description of the new infrastructure including the instances tha needs to be deleted
     *                             ({@link com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate}) where the DELETE_REQUESTED status
     *                             {@link com.sequenceiq.cloudbreak.cloud.model.InstanceStatus} denotes that it is an instance that needs to be terminated.
     * @param resources            resources that need to be updated (e.g HEAT_TEMPLATE)
     * @param vms                  the {@link CloudInstance}s are listed that needs to be deleted
     * @return the list of resources to be removed
     */
    List<CloudResource> collectResourcesToRemove(AuthenticatedContext authenticatedContext,
            CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms);

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
     * @throws TemplatingNotSupportedException if template not supported by provider
     */
    String getStackTemplate() throws TemplatingNotSupportedException;

    /**
     * Gets the cloud platform related database stack template.
     *
     * @param databaseStack contains the full description of infrastructure
     * @return the platform related database stack template
     * @throws TemplatingNotSupportedException if templating is not supported by provider
     */
    String getDBStackTemplate(DatabaseStack databaseStack) throws TemplatingNotSupportedException;

    /**
     * Updates the database root password
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param databaseStack contains the full description of infrastructure
     * @param newPassword new password for the database root user
     */
    void updateDatabaseRootPassword(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack, String newPassword);

    /**
     * Updates the database root CA
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param databaseStack contains the full description of infrastructure
     * @param desiredCertificate the cert which should be applied on the database
     */
    default void updateDatabaseServerActiveSslRootCertificate(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack,
            String desiredCertificate) {
        LOGGER.warn("Update database root ca is not implemented!");
    }

    /**
     * Migrate database from non ssl to ssl
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param databaseStack contains the full description of infrastructure
     */
    default void migrateDatabaseFromNonSslToSsl(AuthenticatedContext authenticatedContext, DatabaseStack databaseStack) {
        LOGGER.warn("Update database ssl is not implemented!");
    }

    /**
     * Modifies attached volumes on an instance.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param volumeIds       contains the list of cloud volumes being modified
     * @param diskType       type to which the volume are being modified
     * @param size       size to which the volumes are modified
     * @throws Exception in case of any error
     */
    void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception;

    default ResourceType getInstanceResourceType() {
        throw new UnsupportedOperationException("Getting instance resource type is not supported for this provider.");
    }
}