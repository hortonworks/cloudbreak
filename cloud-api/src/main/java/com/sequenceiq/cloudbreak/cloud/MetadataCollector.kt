package com.sequenceiq.cloudbreak.cloud

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus

/**
 * Cloudbreak collects metadata about VM instances like private and floating (public) addresses.
 */
interface MetadataCollector {

    /**
     * Status with the collected metadata.

     * @param authenticatedContext the authenticated context which holds the client object
     * *
     * @param resources            resources managed by Cloudbreak, used to figure out which resources are associated with the given VMs (e.g network port)
     * *
     * @param vms                  the VM instances for which the metadata needs to be collected
     * *
     * @return status of instances including the metadata
     */
    fun collect(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmMetaDataStatus>
}
