package com.sequenceiq.cloudbreak.service.stackpatch;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.service.retry.RetryType;
import com.sequenceiq.cloudbreak.util.StackStatusAndReachabilityValidatorUtil;
import com.sequenceiq.flow.api.model.FlowIdentifier;

/**
 * Re-pushes the corrected minifi flow config to clusters where minifi is the enabled logging agent.
 * The regression (CB-34474) shipped an unbounded, unthrottled upload-failure self-loop in the minifi
 * flow config; the fix lives in the minifi salt state templates baked into the current build, but
 * already running clusters still hold the old per-cluster salt state. This patch triggers a salt update
 * flow so the minifi salt state is re-applied, config.yml is regenerated with the bounded retry and the
 * minifi service is restarted.
 */
@Service
public class MinifiRetryConfigFixPatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinifiRetryConfigFixPatchService.class);

    private static final String MINIFI_STATUS_COMMAND = "systemctl is-active minifi || true";

    private static final String MINIFI_ACTIVE_STATUS = "active";

    @Inject
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackStatusAndReachabilityValidatorUtil stackStatusAndReachabilityValidatorUtil;

    @Inject
    private ReactorFlowManager reactorFlowManager;

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.MINIFI_RETRY_CONFIG_FIX;
    }

    @Override
    public boolean isAffected(Stack stack) {
        if (!stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)) {
            LOGGER.warn("Minifi retry config fix patch will be skipped for {} stack, because its status is not valid or not all nodes are reachable,status: {}",
                    stack.getName(), stack.getStatus());
            throw new CloudbreakServiceException("The stack is not in a valid state to run the minifi retry config fix patch!");
        }
        try {
            boolean minifiLoggingActive = isMinifiLoggingActive(stack);
            LOGGER.info("Minifi retry config fix patch, stack: {}, stack id: {}, minifi logging active: {}",
                    stack.getName(), stack.getId(), minifiLoggingActive);
            return minifiLoggingActive;
        } catch (Exception e) {
            LOGGER.warn("Could not determine minifi logging status for stack {}", stack.getResourceCrn(), e);
            throw new CloudbreakServiceException(e);
        }
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        try {
            FlowIdentifier flowIdentifier = reactorFlowManager.triggerSaltUpdate(stack.getId());
            LOGGER.info("Started salt update flow for minifi retry config fix on stack '{}' with flow identifier '{}'.",
                    stack.getId(), flowIdentifier);
            return true;
        } catch (Exception e) {
            throw new ExistingStackPatchApplyException(
                    String.format("Minifi retry config fix on %s failed: %s", stack.getResourceCrn(), e.getMessage()), e);
        }
    }

    private boolean isMinifiLoggingActive(Stack stack) throws Exception {
        OrchestratorMetadata metadata = orchestratorMetadataProvider.getOrchestratorMetadata(stack.getId());
        Set<String> targetFqdns = metadata.getNodes().stream().map(Node::getHostname).collect(Collectors.toSet());
        if (targetFqdns.isEmpty()) {
            return false;
        }
        Map<String, String> statusByHost = hostOrchestrator.runCommandOnHosts(metadata.getGatewayConfigs(), targetFqdns,
                MINIFI_STATUS_COMMAND, RetryType.NO_RETRY);
        return statusByHost.values().stream()
                .anyMatch(status -> MINIFI_ACTIVE_STATUS.equalsIgnoreCase(StringUtils.trimToEmpty(status)));
    }
}
