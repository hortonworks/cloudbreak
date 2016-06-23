package com.sequenceiq.cloudbreak.cloud.gcp.compute

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.gcp.AbstractGcpResourceBuilder
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder

abstract class AbstractGcpComputeBuilder : AbstractGcpResourceBuilder(), ComputeResourceBuilder<GcpContext> {

    override fun checkResources(context: GcpContext, auth: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        return checkResources(resourceType(), context, auth, resources)
    }

    override fun checkInstances(context: GcpContext, auth: AuthenticatedContext, instances: List<CloudInstance>): List<CloudVmInstanceStatus>? {
        return null
    }

    override fun stop(context: GcpContext, auth: AuthenticatedContext, instance: CloudInstance): CloudVmInstanceStatus? {
        return null
    }

    override fun start(context: GcpContext, auth: AuthenticatedContext, instance: CloudInstance): CloudVmInstanceStatus? {
        return null
    }


}
