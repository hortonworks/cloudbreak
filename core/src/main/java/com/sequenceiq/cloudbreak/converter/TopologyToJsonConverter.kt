package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.google.api.client.util.Maps
import com.sequenceiq.cloudbreak.api.model.TopologyResponse
import com.sequenceiq.cloudbreak.domain.Topology
import com.sequenceiq.cloudbreak.domain.TopologyRecord

@Component
class TopologyToJsonConverter : AbstractConversionServiceAwareConverter<Topology, TopologyResponse>() {

    override fun convert(source: Topology): TopologyResponse {
        val json = TopologyResponse()
        json.id = source.id
        json.name = source.name
        json.description = source.description
        json.cloudPlatform = source.cloudPlatform
        json.nodes = convertNodes(source.records)
        return json
    }

    private fun convertNodes(records: List<TopologyRecord>): Map<String, String> {
        val result = Maps.newHashMap<String, String>()
        for (record in records) {
            result.put(record.hypervisor, record.rack)
        }
        return result
    }
}
