package com.sequenceiq.periscope.rest.converter

import org.springframework.stereotype.Component

import com.sequenceiq.periscope.domain.Cluster
import com.sequenceiq.periscope.api.model.ClusterJson

@Component
class ClusterConverter : AbstractConverter<ClusterJson, Cluster>() {

    override fun convert(source: Cluster): ClusterJson {
        val json = ClusterJson()
        json.id = source.id
        json.stackId = source.stackId
        json.host = source.host
        json.port = source.port
        json.state = source.state.name
        return json
    }

}
