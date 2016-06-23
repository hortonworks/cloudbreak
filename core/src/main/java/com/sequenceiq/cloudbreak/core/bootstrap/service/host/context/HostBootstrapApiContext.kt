package com.sequenceiq.cloudbreak.core.bootstrap.service.host.context

import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.service.StackContext

class HostBootstrapApiContext(stack: Stack, val gatewayConfig: GatewayConfig, val hostOrchestrator: HostOrchestrator) : StackContext(stack)
