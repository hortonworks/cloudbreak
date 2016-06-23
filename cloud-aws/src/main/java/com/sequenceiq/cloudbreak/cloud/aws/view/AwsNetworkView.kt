package com.sequenceiq.cloudbreak.cloud.aws.view

import org.apache.commons.lang3.StringUtils.isNoneEmpty

import com.sequenceiq.cloudbreak.cloud.model.Network

class AwsNetworkView(private val network: Network) {

    val isExistingVPC: Boolean
        get() = isNoneEmpty(network.getStringParameter(VPC))

    val isExistingSubnet: Boolean
        get() = isNoneEmpty(network.getStringParameter(SUBNET))

    val isExistingIGW: Boolean
        get() = isNoneEmpty(network.getStringParameter(IGW))

    val existingSubnet: String
        get() = network.getStringParameter(SUBNET)

    val existingIGW: String
        get() = network.getStringParameter(IGW)

    val existingVPC: String
        get() = network.getStringParameter(VPC)

    companion object {

        private val VPC = "vpcId"
        private val IGW = "internetGatewayId"
        private val SUBNET = "subnetId"
    }
}
