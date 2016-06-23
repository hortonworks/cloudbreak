package com.sequenceiq.cloudbreak.service.cluster.filter

import com.sequenceiq.cloudbreak.domain.HostMetadata

interface HostFilter {

    @Throws(HostFilterException::class)
    fun filter(clusterId: Long, config: Map<String, String>, hosts: List<HostMetadata>): List<HostMetadata>
}
