package com.sequenceiq.cloudbreak.cloud.openstack.nativ.context

import org.openstack4j.api.OSClient

import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext

class OpenStackContext(name: String, location: Location, parallelResourceRequest: Int, build: Boolean) : ResourceBuilderContext(name, location, parallelResourceRequest, build) {
    private val osClient: OSClient? = null
}
