package com.sequenceiq.cloudbreak.converter

import com.sequenceiq.cloudbreak.service.network.ExposedService.SHIPYARD

import java.util.Date
import java.util.HashMap
import java.util.HashSet

import javax.inject.Inject

import org.apache.commons.lang3.BooleanUtils
import org.springframework.stereotype.Component

import com.fasterxml.jackson.databind.JsonNode
import com.google.common.base.Optional
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson
import com.sequenceiq.cloudbreak.api.model.ClusterResponse
import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptor
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptors
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.RDSConfig
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.network.NetworkUtils
import com.sequenceiq.cloudbreak.service.network.Port

@Component
class ClusterToJsonConverter : AbstractConversionServiceAwareConverter<Cluster, ClusterResponse>() {

    @Inject
    private val blueprintValidator: BlueprintValidator? = null
    @Inject
    private val stackServiceComponentDescs: StackServiceComponentDescriptors? = null

    override fun convert(source: Cluster): ClusterResponse {
        val clusterResponse = ClusterResponse()
        clusterResponse.id = source.id
        clusterResponse.name = source.name
        clusterResponse.status = source.status.name
        clusterResponse.statusReason = source.statusReason
        if (source.blueprint != null) {
            clusterResponse.blueprintId = source.blueprint.id
        }
        if (source.upSince != null && source.isAvailable) {
            val now = Date().time
            val uptime = now - source.upSince!!
            val minutes = (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE) % SECONDS_PER_MINUTE).toInt()
            val hours = (uptime / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE)).toInt()
            clusterResponse.hoursUp = hours
            clusterResponse.minutesUp = minutes
        } else {
            clusterResponse.hoursUp = 0
            clusterResponse.minutesUp = 0
        }
        clusterResponse.ldapRequired = source.isLdapRequired
        if (source.sssdConfig != null) {
            clusterResponse.sssdConfigId = source.sssdConfig.id
        }
        val ambariStackDetails = source.ambariStackDetails
        if (ambariStackDetails != null) {
            clusterResponse.ambariStackDetails = conversionService.convert<AmbariStackDetailsJson>(ambariStackDetails, AmbariStackDetailsJson::class.java)
        }
        val rdsConfig = source.rdsConfig
        if (rdsConfig != null) {
            clusterResponse.rdsConfigJson = conversionService.convert<RDSConfigJson>(rdsConfig, RDSConfigJson::class.java)
        }
        clusterResponse.ambariServerIp = source.ambariIp
        clusterResponse.userName = source.userName
        clusterResponse.password = source.password
        clusterResponse.description = if (source.description == null) "" else source.description
        clusterResponse.hostGroups = convertHostGroupsToJson(source.hostGroups)
        clusterResponse.serviceEndPoints = prepareServiceEndpointsMap(source.hostGroups, source.blueprint, source.ambariIp,
                source.enableShipyard)
        clusterResponse.enableShipyard = source.enableShipyard
        clusterResponse.configStrategy = source.configStrategy
        return clusterResponse
    }

    private fun convertHostGroupsToJson(hostGroups: Set<HostGroup>): Set<HostGroupJson> {
        val jsons = HashSet<HostGroupJson>()
        for (hostGroup in hostGroups) {
            jsons.add(conversionService.convert<HostGroupJson>(hostGroup, HostGroupJson::class.java))
        }
        return jsons
    }

    private fun prepareServiceEndpointsMap(hostGroups: Set<HostGroup>, blueprint: Blueprint, ambariIp: String, shipyardEnabled: Boolean?): Map<String, String> {
        val result = HashMap<String, String>()

        val ports = NetworkUtils.getPorts(Optional.absent<Stack>())
        collectPortsOfAdditionalServices(result, ambariIp, shipyardEnabled)
        try {
            val hostGroupsNode = blueprintValidator!!.getHostGroupNode(blueprint)
            val hostGroupMap = blueprintValidator.createHostGroupMap(hostGroups)
            for (hostGroupNode in hostGroupsNode) {
                val hostGroupName = blueprintValidator.getHostGroupName(hostGroupNode)
                val componentsNode = blueprintValidator.getComponentsNode(hostGroupNode)
                val actualHostgroup = hostGroupMap[hostGroupName]
                val serviceAddress: String
                if (actualHostgroup.constraint.instanceGroup != null) {
                    val next = actualHostgroup.constraint.instanceGroup.instanceMetaData.iterator().next()
                    serviceAddress = next.publicIpWrapper
                } else {
                    serviceAddress = actualHostgroup.hostMetadata.iterator().next().hostName
                }
                for (componentNode in componentsNode) {
                    val componentName = componentNode.get("name").asText()
                    val componentDescriptor = stackServiceComponentDescs!!.get(componentName)
                    collectServicePorts(result, ports, serviceAddress, componentDescriptor)
                }
            }
        } catch (ex: Exception) {
            return result
        }

        return result
    }

    private fun collectPortsOfAdditionalServices(result: MutableMap<String, String>, ambariIp: String, shipyardEnabled: Boolean?) {
        if (BooleanUtils.isTrue(shipyardEnabled)) {
            val shipyardPort = NetworkUtils.getPortByServiceName(SHIPYARD)
            result.put(shipyardPort.name, String.format("%s:%s%s", ambariIp, shipyardPort.port, shipyardPort.exposedService.postFix))
        }
    }

    private fun collectServicePorts(result: MutableMap<String, String>, ports: List<Port>, address: String, componentDescriptor: StackServiceComponentDescriptor?) {
        if (componentDescriptor != null && componentDescriptor.isMaster) {
            for (port in ports) {
                if (port.exposedService.serviceName == componentDescriptor.name) {
                    result.put(port.exposedService.portName,
                            String.format("%s:%s%s", address, port.port, port.exposedService.postFix))
                }
            }
        }
    }

    companion object {

        private val SECONDS_PER_MINUTE = 60
        private val MILLIS_PER_SECOND = 1000
    }

}
