package com.sequenceiq.cloudbreak.cloud.gcp.context

import com.google.api.services.compute.Compute
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext

class GcpContext(name: String, location: Location, projectId: String, compute: Compute, parallelResourceRequest: Int, build: Boolean) : ResourceBuilderContext(name, location, parallelResourceRequest, build) {

    init {
        putParameter(PROJECT_ID, projectId)
        putParameter(COMPUTE, compute)
    }

    val projectId: String
        get() = getParameter(PROJECT_ID, String::class.java)

    val compute: Compute
        get() = getParameter(COMPUTE, Compute::class.java)

    companion object {

        val PROJECT_ID = "pid"
        private val COMPUTE = "compute"
    }
}
