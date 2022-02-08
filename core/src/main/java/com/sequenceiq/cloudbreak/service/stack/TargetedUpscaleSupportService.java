package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetedUpscaleSupportService.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackUtil stackUtil;

    @Cacheable(cacheNames = "targetedUpscaleCache", key = "{ #stack.resourceCrn }")
    public boolean targetedUpscaleOperationSupported(Stack stack) {
        try {
            String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
            return entitlementService.targetedUpscaleSupported(accountId) &&
                    isUnboundEliminationSupported(accountId) && isUnboundClusterConfigRemoved(stack);
        } catch (Exception e) {
            LOGGER.error("Error occurred during checking if targeted upscale supported, thus assuming it is not enabled, cause: ", e);
            return false;
        }
    }

    private boolean isUnboundClusterConfigRemoved(Stack stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Set<Node> reachableNodes = stackUtil.collectReachableNodes(stack);
        Set<String> reachableHostnames = reachableNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        boolean unboundClusterConfigPresentOnAnyNodes = hostOrchestrator.unboundClusterConfigPresentOnAnyNodes(primaryGatewayConfig, reachableHostnames);
        LOGGER.info("Result of check whether unbound config is present on nodes of stack [{}] is: {}",
                stack.getResourceCrn(), unboundClusterConfigPresentOnAnyNodes);
        return !unboundClusterConfigPresentOnAnyNodes;
    }

    private boolean isUnboundEliminationSupported(String accountId) {
        if (entitlementService.isUnboundEliminationSupported(accountId)) {
            return true;
        } else {
            LOGGER.info("Unbound elimination is disabled for account {}, thus targeted upscale is not supported!", accountId);
            return false;
        }
    }
}
