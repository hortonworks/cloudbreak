package com.sequenceiq.cloudbreak.service.network

import com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS
import com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP

import java.util.HashSet

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.model.NetworkConfig
import com.sequenceiq.cloudbreak.common.type.CloudConstants
import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Network
import com.sequenceiq.cloudbreak.repository.NetworkRepository

@Service
class DefaultNetworkCreator {

    @Inject
    private val networkRepository: NetworkRepository? = null

    fun createDefaultNetworks(user: CbUser): Set<Network> {
        var networks: Set<Network> = HashSet()
        val defaultNetworks = networkRepository!!.findAllDefaultInAccount(user.account)

        if (defaultNetworks.isEmpty()) {
            networks = createDefaultNetworkInstances(user)
        }

        return networks
    }

    private fun createDefaultNetworkInstances(user: CbUser): Set<Network> {
        val networks = HashSet<Network>()

        val awsNetwork = Network()
        setNetworkCommonFields(awsNetwork, DEFAULT_AWS_NETWORK_NAME, "Default network settings for AWS clusters.",
                NetworkConfig.SUBNET_16, user, AWS)
        networks.add(networkRepository!!.save(awsNetwork))

        val azureNetwork = Network()
        setNetworkCommonFields(azureNetwork, DEFAULT_AZURE_RM_NETWORK_NAME, "Default network settings for Azure RM clusters.",
                NetworkConfig.SUBNET_16, user, CloudConstants.AZURE_RM)
        networks.add(networkRepository.save(azureNetwork))

        val gcpNetwork = Network()
        setNetworkCommonFields(gcpNetwork, DEFAULT_GCP_NETWORK_NAME, "Default network settings for Gcp clusters.",
                NetworkConfig.SUBNET_16, user, GCP)
        networks.add(networkRepository.save(gcpNetwork))

        return networks
    }

    private fun setNetworkCommonFields(network: Network, name: String, description: String, subnet: String, user: CbUser, platform: String) {
        network.name = name
        network.description = description
        network.subnetCIDR = subnet
        network.owner = user.userId
        network.account = user.account
        network.status = ResourceStatus.DEFAULT
        network.isPublicInAccount = true
        network.setCloudPlatform(platform)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(DefaultNetworkCreator::class.java)

        private val DEFAULT_AWS_NETWORK_NAME = "default-aws-network"
        private val DEFAULT_GCP_NETWORK_NAME = "default-gcp-network"
        private val DEFAULT_AZURE_RM_NETWORK_NAME = "default-azure-rm-network"
    }

}
