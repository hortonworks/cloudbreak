package com.sequenceiq.cloudbreak.cloud.notification;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;

import reactor.rx.Promise;

/**
 * When the Cloud provider allocates a resource (e.g starts a VM, or creates a disk) then it notifies the Cloudbreak through this interface.
 * The interface can be used in synch and also in asych way, synhronisation for the returned {@link Promise} with the {@link Promise#await()} method call.
 * <p/>
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
    Promise<ResourcePersisted> notifyAllocation(CloudResource cloudResource, CloudContext cloudContext);

    /**
     * Inform Cloudbreak about a resource has been updated
     *
     * @param cloudResource the allocated {@link CloudResource}
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * @return status of update resource
     */
    Promise<ResourcePersisted> notifyUpdate(CloudResource cloudResource, CloudContext cloudContext);

    /**
     * Inform Cloudbreak about a resource has been deleted
     *
     * @param cloudResource the allocated {@link CloudResource}
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * @return status of deleted resource
     */
    Promise<ResourcePersisted> notifyDeletion(CloudResource cloudResource, CloudContext cloudContext);

}
