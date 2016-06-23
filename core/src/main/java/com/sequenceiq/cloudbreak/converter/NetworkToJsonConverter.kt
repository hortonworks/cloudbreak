package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.NetworkJson
import com.sequenceiq.cloudbreak.domain.Network
import com.sequenceiq.cloudbreak.domain.json.Json

@Component
class NetworkToJsonConverter : AbstractConversionServiceAwareConverter<Network, NetworkJson>() {

    override fun convert(source: Network): NetworkJson {
        val json = NetworkJson()
        json.id = source.id!!.toString()
        json.cloudPlatform = source.cloudPlatform()
        json.name = source.name
        json.description = source.description
        json.subnetCIDR = source.subnetCIDR
        json.isPublicInAccount = source.isPublicInAccount
        val attributes = source.attributes
        if (attributes != null) {
            json.parameters = attributes.map
        }
        if (source.topology != null) {
            json.topologyId = source.topology.id
        }
        return json
    }
}
