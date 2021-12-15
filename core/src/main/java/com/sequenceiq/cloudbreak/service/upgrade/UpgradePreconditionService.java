package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.spot.SpotInstanceUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;

@Component
public class UpgradePreconditionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradePreconditionService.class);

    @Inject
    private SpotInstanceUsageCondition spotInstanceUsageCondition;

    @Inject
    private StackStopRestrictionService stackStopRestrictionService;

    public UpgradeV4Response checkForRunningAttachedClusters(StackViewV4Responses stackViewV4Responses, UpgradeV4Response upgradeOptions, Stack stack) {
        String notStoppedAttachedClusters = getNotStoppedAttachedClusters(stackViewV4Responses, stack);
        if (!notStoppedAttachedClusters.isEmpty()) {
            upgradeOptions.setReason(String.format("There are attached Data Hub clusters in incorrect state: %s. "
                    + "Please stop those to be able to perform the upgrade.", notStoppedAttachedClusters));
        }
        return upgradeOptions;
    }

    private String getNotStoppedAttachedClusters(StackViewV4Responses stackViewV4Responses, Stack stack) {
        return stackViewV4Responses.getResponses()
                .stream()
                .filter(stackResponse -> (isStackStatusNotEligible(stackResponse) || isClusterStatusNotEligible(stackResponse)) && isStoppable(stack))
                .map(StackViewV4Response::getName)
                .collect(Collectors.joining(","));
    }

    private boolean isStackStatusNotEligible(StackViewV4Response stackResponse) {
        LOGGER.info("Checking stack status for {}", stackResponse.getName());
        return !Status.getAllowedDataHubStatesForSdxUpgrade().contains(stackResponse.getStatus());
    }

    private boolean isStoppable(Stack stack) {
        LOGGER.info("Checking volume for {}", stack.getName());
        return notUsingEphemeralVolume(stack) && notRunsOnSpotInstances(stack);
    }

    public boolean notUsingEphemeralVolume(Stack stack) {
        StopRestrictionReason stopRestrictionReason = stackStopRestrictionService.isInfrastructureStoppable(stack);
        return !StopRestrictionReason.EPHEMERAL_VOLUMES.equals(stopRestrictionReason)
                && !StopRestrictionReason.EPHEMERAL_VOLUME_CACHING.equals(stopRestrictionReason);
    }

    private boolean notRunsOnSpotInstances(Stack stack) {
        return !spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack);
    }

    private boolean isClusterStatusNotEligible(StackViewV4Response stackResponse) {
        LOGGER.info("Checking cluster status for {}", stackResponse.getName());
        return stackResponse.getCluster() != null
                && !Status.getAllowedDataHubStatesForSdxUpgrade()
                        .contains(stackResponse.getCluster()
                                .getStatus());
    }
}
