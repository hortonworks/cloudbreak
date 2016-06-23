package com.sequenceiq.cloudbreak.cloud.arm.view

import java.util.ArrayList

import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule

class ArmSecurityView(security: Security) {

    private val ports = ArrayList<ArmPortView>()

    init {
        for (securityRule in security.rules) {
            for (port in securityRule.ports) {
                ports.add(ArmPortView(securityRule.cidr, port, securityRule.protocol))
            }
        }
    }

    fun getPorts(): List<ArmPortView> {
        return ports
    }
}