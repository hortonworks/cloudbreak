package com.sequenceiq.cloudbreak.cloud.openstack.common

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

object OpenStackConstants {
    val OPENSTACK_PLATFORM = Platform.platform("OPENSTACK")

    val FACING = "facing"
    val TENANT_ID = "tenantId"
    val NETWORK_ID = "networkId"
    val SUBNET_ID = "subnetId"
    val ROUTER_ID = "routerId"
    val SECURITYGROUP_ID = "securityGroupId"
    val VOLUME_MOUNT_POINT = "volumeMountPoint"
    val INSTANCE_ID = "instanceId"
    val PORT_ID = "portId"
    val SERVER = "server"
    val FLOATING_IP_IDS = "floatingIpIds"
    val PUBLIC_NET_ID = "publicNetId"

    enum class OpenStackVariant private constructor(variant: String) {
        HEAT("HEAT"),
        NATIVE("NATIVE");

        private val variant: Variant

        init {
            this.variant = Variant.variant(variant)
        }

        fun variant(): Variant {
            return this.variant
        }
    }
}
