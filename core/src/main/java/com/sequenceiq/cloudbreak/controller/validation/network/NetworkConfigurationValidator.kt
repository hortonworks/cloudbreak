package com.sequenceiq.cloudbreak.controller.validation.network

import org.apache.commons.net.util.SubnetUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Network

@Component
class NetworkConfigurationValidator {

    fun validateNetworkForStack(network: Network, instanceGroups: Set<InstanceGroup>): Boolean {
        if (network.subnetCIDR != null) {
            val utils = SubnetUtils(network.subnetCIDR)
            val addressCount = utils.info.addressCount
            var nodeCount = 0
            for (instanceGroup in instanceGroups) {
                nodeCount += instanceGroup.nodeCount!!
            }
            if (addressCount < nodeCount) {
                LOGGER.error("Cannot assign more than {} addresses in the selected subnet.", addressCount)
                throw BadRequestException(
                        String.format("Cannot assign more than %s addresses in the selected subnet.", addressCount))
            }
        }
        return true
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(NetworkConfigurationValidator::class.java)
    }
}
