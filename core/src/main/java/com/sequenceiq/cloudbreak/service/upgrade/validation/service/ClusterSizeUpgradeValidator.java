package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@Component
public class ClusterSizeUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSizeUpgradeValidator.class);

    @Value("${cb.upgrade.validation.distrox.maxNumberOfInstancesForRollingUpgrade}")
    private int maxNumberOfInstancesForRollingUpgrade;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private EntitlementService entitlementService;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        if (validationRequest.rollingUpgradeEnabled() && rollingUpgradeValidationEnabled()) {
            validateClusterSizeForRollingUpgrade(validationRequest.stack());
        } else {
            LOGGER.debug("Skipping cluster size validation because the rolling upgrade is not enabled");
        }
    }

    private void validateClusterSizeForRollingUpgrade(StackDto stack) {
        int numberOfInstances = stack.getAllAvailableInstances().size();
        if (numberOfInstances > maxNumberOfInstancesForRollingUpgrade) {
            String errorMessage = String.format("Rolling upgrade is not permitted because this cluster has %s nodes but the maximum number of "
                    + "allowed nodes is %s.", numberOfInstances, maxNumberOfInstancesForRollingUpgrade);
            throw new UpgradeValidationFailedException(errorMessage);
        } else {
            LOGGER.debug("Rolling upgrade is permitted for this cluster because this cluster has {} nodes and the maximum number of nodes for rolling "
                    + "upgrade is {}", numberOfInstances, maxNumberOfInstancesForRollingUpgrade);
        }
    }

    private boolean rollingUpgradeValidationEnabled() {
        boolean skipRollingUpgradeValidationEnabled = entitlementService.isSkipRollingUpgradeValidationEnabled(ThreadBasedUserCrnProvider.getAccountId());
        if (skipRollingUpgradeValidationEnabled) {
            LOGGER.debug("Skipping cluster size validation because rolling upgrade validation is disabled.");
        }
        return !skipRollingUpgradeValidationEnabled;
    }
}
