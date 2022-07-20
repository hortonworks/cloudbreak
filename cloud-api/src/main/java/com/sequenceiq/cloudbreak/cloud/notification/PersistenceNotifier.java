package com.sequenceiq.cloudbreak.cloud.notification;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;

/**
 * When the Cloud provider allocates a resource (e.g starts a VM, or creates a disk) then it notifies the Cloudbreak through this interface.
 * <br>
 * Note: if the Cloud provider fails not send a notificayion then the resource (e.g VM, disk, etc.) will not be managed by Cloudbreak.
 */
public interface PersistenceNotifier {

    /**
     * Inform Cloudbreak about a resource allocation on Cloud provider side.
     *
     * @param cloudResource the allocated {@link CloudResource}
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * @return status of persisted resource
     */
    ResourcePersisted notifyAllocation(CloudResource cloudResource, CloudContext cloudContext);

    /**
     * Inform Cloudbreak about resources allocations on Cloud provider side.
     *
     * @param cloudResources the allocated {@link CloudResource} list
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * @return status of persisted resource
     */
    ResourcePersisted notifyAllocations(List<CloudResource> cloudResources, CloudContext cloudContext);

    /**
     * Inform Cloudbreak about a resource has been updated
     *
     * @param cloudResource the allocated {@link CloudResource}
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * @return status of update resource
     */
    ResourcePersisted notifyUpdate(CloudResource cloudResource, CloudContext cloudContext);

    /**
     * Inform Cloudbreak about a resources have been updated
     *
     * @param cloudResources the allocated {@link CloudResource} list
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * @return status of update resource
     */
    ResourcePersisted notifyUpdates(List<CloudResource> cloudResources, CloudContext cloudContext);

    /**
     * Inform Cloudbreak about a resource has been deleted
     *
     * @param cloudResource the allocated {@link CloudResource}
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * @return status of deleted resource
     */
    ResourcePersisted notifyDeletion(CloudResource cloudResource, CloudContext cloudContext);

    /**
     * Inform Cloudbreak about resource have been deleted
     *
     * @param cloudResources the allocated {@link CloudResource} list
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * @return status of deleted resource
     */
    ResourcePersisted notifyDeletions(List<CloudResource> cloudResources, CloudContext cloudContext);

}
