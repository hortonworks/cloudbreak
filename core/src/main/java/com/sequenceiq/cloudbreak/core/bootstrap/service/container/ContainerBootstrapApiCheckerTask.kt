package com.sequenceiq.cloudbreak.core.bootstrap.service.container

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerBootstrapApiContext
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask

@Component
class ContainerBootstrapApiCheckerTask : StackBasedStatusCheckerTask<ContainerBootstrapApiContext>() {

    override fun checkStatus(containerBootstrapApiContext: ContainerBootstrapApiContext): Boolean {
        return containerBootstrapApiContext.containerOrchestrator.isBootstrapApiAvailable(containerBootstrapApiContext.gatewayConfig)
    }

    override fun handleTimeout(t: ContainerBootstrapApiContext) {
        throw CloudbreakServiceException("Operation timed out. Could not reach bootstrap API in time.")
    }

    override fun successMessage(t: ContainerBootstrapApiContext): String {
        return "Bootstrap API is available."
    }
}
