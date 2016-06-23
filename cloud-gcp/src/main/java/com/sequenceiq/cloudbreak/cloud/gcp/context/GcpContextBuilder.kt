package com.sequenceiq.cloudbreak.cloud.gcp.context

import org.springframework.stereotype.Service

import com.google.api.services.compute.Compute
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder

@Service
class GcpContextBuilder : ResourceContextBuilder<GcpContext> {

    override fun contextInit(context: CloudContext, auth: AuthenticatedContext, network: Network, resources: List<CloudResource>, build: Boolean): GcpContext {
        val credential = auth.cloudCredential
        val projectId = GcpStackUtil.getProjectId(credential)
        val compute = GcpStackUtil.buildCompute(credential)
        val location = context.location
        return GcpContext(context.name, location, projectId, compute, PARALLEL_RESOURCE_REQUEST, build)
    }

    override fun platform(): Platform {
        return GcpConstants.GCP_PLATFORM
    }

    override fun variant(): Variant {
        return GcpConstants.GCP_VARIANT
    }

    companion object {

        val PARALLEL_RESOURCE_REQUEST = 30
    }
}
