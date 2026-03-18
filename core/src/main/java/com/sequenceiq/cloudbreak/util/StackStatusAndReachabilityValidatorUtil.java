package com.sequenceiq.cloudbreak.util;

import java.util.EnumSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Component
public class StackStatusAndReachabilityValidatorUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusAndReachabilityValidatorUtil.class);

    private static final EnumSet<Status> JOB_ALLOWED_STATUSES = EnumSet.of(Status.AVAILABLE, Status.NODE_FAILURE);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private SaltOrchestrator saltOrchestrator;

    public boolean validateStackStatusAndReachability(Stack stack) {
        if (!JOB_ALLOWED_STATUSES.contains(stack.getStatus())) {
            LOGGER.warn("Attached volumes patch/sync is needed for {} stack, but will be skipped, because its status is not in {}. Current status: {}",
                    stack.getName(), JOB_ALLOWED_STATUSES, stack.getStatus());
            return false;
        }

        GatewayConfig primaryGateway = gatewayConfigService.getPrimaryGatewayConfig(stack);

        Set<Node> allNodes = stackUtil.collectNodes(stack);
        Set<Node> reachableNodes = saltOrchestrator.getResponsiveNodes(allNodes, primaryGateway, true).getReachableNodes();
        if (allNodes.size() != reachableNodes.size()) {
            LOGGER.warn("Attached volumes patch/sync is needed for {} stack, but will be skipped, because not all the nodes are reachable.",
                    stack.getName());
            return false;
        }

        return true;
    }
}
