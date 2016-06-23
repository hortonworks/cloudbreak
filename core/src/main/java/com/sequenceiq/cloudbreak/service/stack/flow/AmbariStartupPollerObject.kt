package com.sequenceiq.cloudbreak.service.stack.flow

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.StackContext

class AmbariStartupPollerObject(stack: Stack, ambariIp: String, var ambariClient: AmbariClient?) : StackContext(stack) {

    var ambariAddress: String? = null
        private set

    init {
        this.ambariAddress = ambariIp
    }

    fun setAmbariIp(ambariIp: String) {
        this.ambariAddress = ambariIp
    }
}
