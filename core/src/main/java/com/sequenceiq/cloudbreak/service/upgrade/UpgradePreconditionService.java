package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.spot.SpotInstanceUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;

@Component
public class UpgradePreconditionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradePreconditionService.class);

    @Inject
    private SpotInstanceUsageCondition spotInstanceUsageCondition;

    @Inject
    private StackStopRestrictionService stackStopRestrictionService;

    @Inject
    private EntitlementService entitlementService;

    public String checkForRunningAttachedClusters(List<? extends StackDtoDelegate> datahubsInEnvironment, Boolean skipDataHubValidation,
            boolean rollingUpgradeEnabled, String accountId) {
        String notStoppedAttachedClusters = getNotStoppedAttachedClusters(datahubsInEnvironment);
        if (!skipValidation(skipDataHubValidation, rollingUpgradeEnabled, accountId)
                && StringUtils.hasText(notStoppedAttachedClusters)) {
            return String.format("There are attached Data Hub clusters in incorrect state: %s. "
                    + "Please stop those to be able to perform the upgrade.", notStoppedAttachedClusters);
        }
        return "";
    }

    private boolean skipValidation(Boolean skipDataHubValidation, boolean rollingUpgradeEnabled, String accountId) {
        return rollingUpgradeEnabled || Boolean.TRUE.equals(skipDataHubValidation) || entitlementService.isUpgradeAttachedDatahubsCheckSkipped(accountId);
    }

    private String getNotStoppedAttachedClusters(List<? extends StackDtoDelegate> datahubsInEnvironment) {
        return datahubsInEnvironment
                .stream()
                .filter(datahub -> (isStackStatusNotEligible(datahub)) && isStoppable(datahub))
                .map(StackDtoDelegate::getName)
                .collect(Collectors.joining(","));
    }

    private boolean isStackStatusNotEligible(StackDtoDelegate stackDto) {
        LOGGER.info("Checking stack status for {}", stackDto.getName());
        return !Status.getAllowedDataHubStatesForSdxUpgrade().contains(stackDto.getStatus());
    }

    private boolean isStoppable(StackDtoDelegate stack) {
        LOGGER.info("Checking volume for {}", stack.getName());
        return notUsingEphemeralVolume(stack) && notRunsOnSpotInstances(stack);
    }

    public boolean notUsingEphemeralVolume(StackDtoDelegate stack) {
        StopRestrictionReason stopRestrictionReason = stackStopRestrictionService.isInfrastructureStoppable(stack);
        return !StopRestrictionReason.EPHEMERAL_VOLUMES.equals(stopRestrictionReason)
                && !StopRestrictionReason.EPHEMERAL_VOLUME_CACHING.equals(stopRestrictionReason);
    }

    private boolean notRunsOnSpotInstances(StackDtoDelegate stack) {
        return !spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack);
    }
}
