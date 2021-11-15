package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class TargetedUpscaleSupportService {

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackUtil stackUtil;

    public boolean targetedUpscaleOperationSupported(Stack stack) {
        // is unbound elimination supported?
        if (!entitlementService.isUnboundEliminationSupported(Crn.safeFromString(stack.getResourceCrn()).getAccountId())) {
            return false;
        }
        // is unbound cluster config already removed?
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Set<Node> reachableNodes = stackUtil.collectReachableNodes(stack);
        return !hostOrchestrator.unboundClusterConfigPresentOnAnyNodes(primaryGatewayConfig,
                reachableNodes.stream().map(node -> node.getHostname()).collect(Collectors.toSet()));
    }
}
