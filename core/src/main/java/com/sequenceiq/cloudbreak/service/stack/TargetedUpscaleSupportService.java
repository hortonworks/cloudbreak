package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.StackView;

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

    public boolean targetedUpscaleOperationSupported(StackView stack) {
        try {
            return targetedUpscaleEntitlementsEnabled(stack.getResourceCrn()) && DnsResolverType.FREEIPA_FOR_ENV.equals(stack.getDomainDnsResolver());
        } catch (Exception e) {
            LOGGER.error("Error occurred during checking if targeted upscale supported, thus assuming it is not enabled, cause: ", e);
            return false;
        }
    }

    public boolean targetedUpscaleEntitlementsEnabled(String crn) {
        String accountId = Crn.safeFromString(crn).getAccountId();
        return entitlementService.targetedUpscaleSupported(accountId) && isUnboundEliminationSupported(accountId);
    }

    public DnsResolverType getActualDnsResolverType(StackDto stackDto) {
        StackView stack = stackDto.getStack();
        LOGGER.debug("Original value of domainDnsResolver field for stack {} is {}", stack.getResourceCrn(), stack.getDomainDnsResolver());
        if (!isUnboundEliminationSupported(Crn.fromString(stack.getResourceCrn()).getAccountId())) {
            LOGGER.debug("Since unbound elimination is not supported, then targeted upscale also won't be supported, " +
                    "thus all 00-cluster.conf will be regenerated for all nodes of stack {}.", stack.getResourceCrn());
            return DnsResolverType.LOCAL_UNBOUND;
        } else {
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
            Set<Node> reachableNodes = stackUtil.collectReachableNodes(stackDto);
            Set<String> reachableHostnames = reachableNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
            boolean unboundClusterConfigPresentOnAnyNodes = hostOrchestrator.unboundClusterConfigPresentOnAnyNodes(primaryGatewayConfig, reachableHostnames);
            LOGGER.debug("Result of check whether unbound config is present on nodes of stack [{}] is: {}",
                    stack.getResourceCrn(), unboundClusterConfigPresentOnAnyNodes);
            if (unboundClusterConfigPresentOnAnyNodes) {
                LOGGER.debug("Although unbound elimination is enabled in account, 00-cluster.conf files still present on at least one node, " +
                        "thus we will fall back to use local unbound service on all nodes of stack {}", stack.getResourceCrn());
                return DnsResolverType.LOCAL_UNBOUND;
            } else {
                LOGGER.debug("Unbound elimination is enabled in account and 00-cluster.conf files were removed from all nodes, " +
                        "thus we can use freeIPA of environment {} to resolve DNS within environment's domain for stack {}",
                        stack.getEnvironmentCrn(), stack.getResourceCrn());
                return DnsResolverType.FREEIPA_FOR_ENV;
            }
        }
    }

    public boolean isUnboundEliminationSupported(String accountId) {
        if (entitlementService.isUnboundEliminationSupported(accountId)) {
            return true;
        } else {
            LOGGER.info("Unbound elimination is disabled for account {}, thus targeted upscale is not supported!", accountId);
            return false;
        }
    }
}
