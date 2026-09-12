package com.sequenceiq.freeipa.service.stackpatch;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackPatchType;
import com.sequenceiq.freeipa.orchestrator.SaltUpdateService;
import com.sequenceiq.freeipa.service.GatewayConfigService;

/**
 * Re-pushes the corrected minifi flow config to FreeIPA clusters where minifi is the enabled logging agent.
 * The regression (CB-34474) shipped an unbounded, unthrottled upload-failure self-loop in the minifi flow config;
 * the fix lives in the minifi salt state templates baked into the current build, but already running clusters still
 * hold the old per-cluster salt state. This patch triggers a salt update flow so the minifi salt state is re-applied,
 * config.yml is regenerated with the bounded retry and the minifi service is restarted.
 */
@Service
public class MinifiRetryConfigFixPatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinifiRetryConfigFixPatchService.class);

    private static final String MINIFI_STATUS_COMMAND = "systemctl is-active minifi || true";

    private static final String MINIFI_ACTIVE_STATUS = "active";

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private SaltUpdateService saltUpdateService;

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.MINIFI_RETRY_CONFIG_FIX;
    }

    @Override
    public boolean isAffected(Stack stack) {
        try {
            boolean minifiLoggingActive = isMinifiLoggingActive(stack);
            LOGGER.info("Minifi retry config fix patch, stack: {}, stack id: {}, minifi logging active: {}",
                    stack.getName(), stack.getId(), minifiLoggingActive);
            return minifiLoggingActive;
        } catch (Exception e) {
            throw new CloudbreakServiceException(
                    String.format("Could not determine minifi logging status for stack %s", stack.getResourceCrn()), e);
        }
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        try {
            FlowIdentifier flowIdentifier = saltUpdateService.updateSaltStates(stack.getEnvironmentCrn(), stack.getAccountId());
            LOGGER.info("Started salt update flow for minifi retry config fix on stack '{}' with flow identifier '{}'.",
                    stack.getId(), flowIdentifier);
            return true;
        } catch (Exception e) {
            throw new ExistingStackPatchApplyException(
                    String.format("Minifi retry config fix on %s failed: %s", stack.getResourceCrn(), e.getMessage()), e);
        }
    }

    private boolean isMinifiLoggingActive(Stack stack) throws Exception {
        Set<String> targetFqdns = stack.getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getDiscoveryFQDN)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (targetFqdns.isEmpty()) {
            return false;
        }
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfigForSalt(stack);
        Map<String, String> statusByHost = hostOrchestrator.runCommandOnHosts(List.of(primaryGatewayConfig), targetFqdns, MINIFI_STATUS_COMMAND);
        return statusByHost.values().stream()
                .anyMatch(status -> MINIFI_ACTIVE_STATUS.equalsIgnoreCase(StringUtils.trimToEmpty(status)));
    }
}
