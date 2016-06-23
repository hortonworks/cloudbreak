package com.sequenceiq.cloudbreak.core.bootstrap.service.container.context

import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig
import com.sequenceiq.cloudbreak.service.StackContext

class ContainerBootstrapApiContext(stack: Stack, val gatewayConfig: GatewayConfig, val containerOrchestrator: ContainerOrchestrator) : StackContext(stack)
