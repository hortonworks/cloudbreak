package com.sequenceiq.cloudbreak.cloud.notification

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted


/**
 * When the Cloud provider allocates a resource (e.g starts a VM, or creates a disk) then it notifies the Cloudbreak through this interface.
 *
 *
 * Note: if the Cloud provider fails not send a notificayion then the resource (e.g VM, disk, etc.) will not be managed by Cloudbreak.
 */
interface PersistenceNotifier {

    /**
     * Inform Cloudbreak about a resource allocation on Cloud provider side.

     * @param cloudResource the allocated [CloudResource]
     * *
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * *
     * @return status of persisted resource
     */
    fun notifyAllocation(cloudResource: CloudResource, cloudContext: CloudContext): ResourcePersisted

    /**
     * Inform Cloudbreak about a resource has been updated

     * @param cloudResource the allocated [CloudResource]
     * *
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * *
     * @return status of update resource
     */
    fun notifyUpdate(cloudResource: CloudResource, cloudContext: CloudContext): ResourcePersisted

    /**
     * Inform Cloudbreak about a resource has been deleted

     * @param cloudResource the allocated [CloudResource]
     * *
     * @param cloudContext  the context containing information to identify which stack (cluster) is affected
     * *
     * @return status of deleted resource
     */
    fun notifyDeletion(cloudResource: CloudResource, cloudContext: CloudContext): ResourcePersisted

}
