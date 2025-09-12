package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.conf.LimitConfiguration;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.util.CodUtil;

@Component
public class ClusterSizeUpgradeValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSizeUpgradeValidator.class);

    @Value("${cb.upgrade.validation.distrox.maxNumberOfInstancesForRollingUpgrade}")
    private int maxNumberOfInstancesForRollingUpgrade;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private LimitConfiguration limitConfiguration;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        long numberOfInstances = validationRequest.stack().getFullNodeCount();
        if (validationRequest.rollingUpgradeEnabled() && rollingUpgradeValidationEnabled(validationRequest.stack())) {
            validateClusterSizeForRollingUpgrade(numberOfInstances);
        }
        if (validationRequest.replaceVms()) {
            validateForOsUpgrade(numberOfInstances);
        }
    }

    private void validateClusterSizeForRollingUpgrade(long numberOfInstances) {
        if (isClusterSizeLargerThanAllowedForRollingUpgrade(numberOfInstances)) {
            String errorMessage = String.format("Rolling upgrade is not permitted because this cluster has %s nodes but the maximum number of "
                    + "allowed nodes is %s.", numberOfInstances, maxNumberOfInstancesForRollingUpgrade);
            throw new UpgradeValidationFailedException(errorMessage);
        } else {
            LOGGER.debug("Rolling upgrade is permitted for this cluster because this cluster has {} nodes and the maximum number of nodes for rolling "
                    + "upgrade is {}", numberOfInstances, maxNumberOfInstancesForRollingUpgrade);
        }
    }

    public boolean isClusterSizeLargerThanAllowedForRollingUpgrade(long numberOfInstances) {
        return numberOfInstances > maxNumberOfInstancesForRollingUpgrade;
    }

    private boolean rollingUpgradeValidationEnabled(StackDto stack) {
        if (entitlementService.isSkipRollingUpgradeValidationEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            LOGGER.debug("Skipping rolling upgrade cluster size validation because rolling upgrade validation is disabled.");
            return false;
        }
        if (CodUtil.isCodCluster(stack)) {
            LOGGER.debug("Skipping rolling upgrade cluster size validation because this is a COD cluster.");
            return false;
        }
        return true;
    }

    private void validateForOsUpgrade(long numberOfInstances) {
        Integer upgradeNodeCountLimit = limitConfiguration.getUpgradeNodeCountLimit(Optional.ofNullable(ThreadBasedUserCrnProvider.getAccountId()));
        LOGGER.debug("Instance count: {} and limit: [{}]", numberOfInstances, upgradeNodeCountLimit);
        if (numberOfInstances > upgradeNodeCountLimit) {
            throw new UpgradeValidationFailedException(
                    String.format("There are %s nodes in the cluster. OS upgrade is supported up to %s nodes. " +
                            "Please downscale the cluster below the limit and retry the upgrade.", numberOfInstances, upgradeNodeCountLimit));
        }
    }
}
