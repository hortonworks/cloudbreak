package com.sequenceiq.cloudbreak.core.bootstrap.service.host

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.context.HostBootstrapApiContext
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask

@Component
class HostBootstrapApiCheckerTask : StackBasedStatusCheckerTask<HostBootstrapApiContext>() {

    override fun checkStatus(hostBootstrapApiContext: HostBootstrapApiContext): Boolean {
        return hostBootstrapApiContext.hostOrchestrator.isBootstrapApiAvailable(hostBootstrapApiContext.gatewayConfig)
    }

    override fun handleTimeout(t: HostBootstrapApiContext) {
        throw CloudbreakServiceException("Operation timed out. Could not reach bootstrap API in time.")
    }

    override fun successMessage(t: HostBootstrapApiContext): String {
        return "Bootstrap API is available."
    }
}
