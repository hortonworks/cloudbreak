package com.sequenceiq.cloudbreak.service.upgrade.image;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

@Component
public class BlueprintUpgradeOptionCondition {

    @Inject
    private EntitlementService entitlementService;

    public BlueprintValidationResult validate(ImageFilterParams imageFilterParams, BlueprintUpgradeOption upgradeOption) {
        if (imageFilterParams.isLockComponents() || imageFilterParams.isReplaceVms()) {
            boolean osUpgradeEnabled = upgradeOption.isOsUpgradeEnabled();
            if (!osUpgradeEnabled) {
                return new BlueprintValidationResult(osUpgradeEnabled, osUpgradeEnabled ? null : "The OS upgrade is not enabled for this blueprint.");
            }
        }
        if (!imageFilterParams.isLockComponents() && imageFilterParams.isRollingUpgradeEnabled()) {
            boolean rollingUpgradeEnabled = validateForRollingUpgrade(upgradeOption);
            if (!rollingUpgradeEnabled) {
                return new BlueprintValidationResult(rollingUpgradeEnabled, rollingUpgradeEnabled ? null : "Rolling upgrade is not supported for " +
                        "this cluster. For details please see the rolling upgrade documentation. To check whether rolling upgrade could be enabled for " +
                        "this cluster please reach out to Cloudera Support.");
            }
        }
        return new BlueprintValidationResult(true);
    }

    private boolean validateForRollingUpgrade(BlueprintUpgradeOption upgradeOption) {
        return upgradeOption.isRollingUpgradeEnabled()
                || entitlementService.isSkipRollingUpgradeValidationEnabled(ThreadBasedUserCrnProvider.getAccountId());
    }

}
