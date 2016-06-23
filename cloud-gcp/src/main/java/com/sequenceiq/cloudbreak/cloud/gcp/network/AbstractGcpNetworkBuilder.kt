package com.sequenceiq.cloudbreak.cloud.gcp.network

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.gcp.AbstractGcpResourceBuilder
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder

abstract class AbstractGcpNetworkBuilder : AbstractGcpResourceBuilder(), NetworkResourceBuilder<GcpContext> {

    override fun checkResources(context: GcpContext, auth: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        return checkResources(resourceType(), context, auth, resources)
    }

    @Throws(Exception::class)
    override fun update(context: GcpContext, auth: AuthenticatedContext,
                        network: Network, security: Security, resource: CloudResource): CloudResourceStatus? {
        return null
    }

}
