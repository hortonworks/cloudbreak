package com.sequenceiq.cloudbreak.service.cluster.filter

import java.util.ArrayList

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository

@Component
class ConsulServerFilter : HostFilter {

    @Inject
    private val clusterRepository: ClusterRepository? = null

    @Inject
    private val instanceMetadataRepository: InstanceMetaDataRepository? = null

    @Throws(HostFilterException::class)
    override fun filter(clusterId: Long, config: Map<String, String>, hosts: List<HostMetadata>): List<HostMetadata> {
        val copy = ArrayList(hosts)
        val cluster = clusterRepository!!.findById(clusterId)
        if ("BYOS" != cluster.stack.cloudPlatform()) {
            for (host in hosts) {
                val instanceMetaData = instanceMetadataRepository!!.findHostInStack(cluster.stack.id, host.hostName)
                if (instanceMetaData != null && instanceMetaData.consulServer!!) {
                    copy.remove(host)
                }
            }
        }
        return copy
    }

}
