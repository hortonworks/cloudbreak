package com.sequenceiq.cloudbreak.util;

import java.util.EnumSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Component
public class StackStatusAndReachabilityValidatorUtil {

    private static final EnumSet<Status> JOB_ALLOWED_STATUSES = EnumSet.of(Status.AVAILABLE, Status.NODE_FAILURE);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private SaltOrchestrator saltOrchestrator;

    public boolean validateStackStatusAndReachability(Stack stack) {
        if (!JOB_ALLOWED_STATUSES.contains(stack.getStatus())) {
            return false;
        }

        GatewayConfig primaryGateway = gatewayConfigService.getPrimaryGatewayConfig(stack);

        Set<Node> allNodes = stackUtil.collectNodes(stack);
        Set<Node> reachableNodes = saltOrchestrator.getResponsiveNodes(allNodes, primaryGateway, true).getReachableNodes();
        return allNodes.size() == reachableNodes.size();
    }
}
