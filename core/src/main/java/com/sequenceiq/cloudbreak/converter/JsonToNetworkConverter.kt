package com.sequenceiq.cloudbreak.converter

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.api.model.NetworkJson
import com.sequenceiq.cloudbreak.domain.Network
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.service.topology.TopologyService

@Component
class JsonToNetworkConverter : AbstractConversionServiceAwareConverter<NetworkJson, Network>() {
    @Inject
    private val topologyService: TopologyService? = null

    override fun convert(source: NetworkJson): Network {
        val network = Network()
        network.name = source.name
        network.description = source.description
        network.subnetCIDR = source.subnetCIDR
        network.isPublicInAccount = source.isPublicInAccount
        network.status = ResourceStatus.USER_MANAGED
        network.setCloudPlatform(source.cloudPlatform)
        val parameters = source.parameters
        if (parameters != null && !parameters.isEmpty()) {
            try {
                network.attributes = Json(parameters)
            } catch (e: JsonProcessingException) {
                throw BadRequestException("Invalid parameters")
            }

        }
        if (source.topologyId != null) {
            network.topology = topologyService!!.get(source.topologyId)
        }
        return network
    }
}
