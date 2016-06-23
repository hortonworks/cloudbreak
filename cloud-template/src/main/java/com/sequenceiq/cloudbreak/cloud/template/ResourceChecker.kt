package com.sequenceiq.cloudbreak.cloud.template

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext

/**
 * Resource creation and deletion requests sent to the provider later needs status checking. The implementation supposed to provide a defined
 * [com.sequenceiq.cloudbreak.cloud.model.ResourceStatus] to track the progress. If the resource status reaches a permanent state the flow will continue.
 */
interface ResourceChecker<C : ResourceBuilderContext> {

    /**
     * Checks the status of the resource creation/deletion. This method will be called as long as the status is in **TRANSIENT** state with a timed delay.

     * @param context   Generic context object passed along with the flow to all methods. It is created by the [ResourceContextBuilder].
     * *
     * @param auth      Authenticated context is provided to be able to send the requests to the cloud provider.
     * *
     * @param resources List of resources to be checked.
     * *
     * @return Returns the status of the requested resources.
     */
    fun checkResources(context: C, auth: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus>
}
