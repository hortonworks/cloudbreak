package com.sequenceiq.cloudbreak.cloud.openstack.view

import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants

class NeutronNetworkView(private val network: Network) {

    val subnetCIDR: String
        get() = network.subnet.cidr

    fun assignFloatingIp(): Boolean {
        if (publicNetId == null || publicNetId!!.isEmpty()) {
            return false
        }
        return true
    }

    val publicNetId: String?
        get() = network.getParameter<String>(OpenStackConstants.PUBLIC_NET_ID, String::class.java)

}


