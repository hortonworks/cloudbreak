package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.google.api.client.util.Lists
import com.sequenceiq.cloudbreak.api.model.TopologyRequest
import com.sequenceiq.cloudbreak.domain.Topology
import com.sequenceiq.cloudbreak.domain.TopologyRecord

@Component
class JsonToTopologyConverter : AbstractConversionServiceAwareConverter<TopologyRequest, Topology>() {
    override fun convert(source: TopologyRequest): Topology {
        val result = Topology()
        result.id = source.id
        result.name = source.name
        result.description = source.description
        result.cloudPlatform = source.cloudPlatform
        result.records = convertNodes(source.nodes)
        return result
    }

    private fun convertNodes(nodes: Map<String, String>?): List<TopologyRecord> {
        val result = Lists.newArrayList<TopologyRecord>()
        if (nodes != null) {
            for (node in nodes.entries) {
                result.add(TopologyRecord(node.key, node.value))
            }
        }
        return result
    }
}
