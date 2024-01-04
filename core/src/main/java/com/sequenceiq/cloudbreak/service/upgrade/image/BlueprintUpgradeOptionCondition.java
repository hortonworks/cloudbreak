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
        if (imageFilterParams.isLockComponents()) {
            return createResult(upgradeOption.isOsUpgradeEnabled(), "The OS upgrade is not enabled for this blueprint.");
        } else if (imageFilterParams.isRollingUpgradeEnabled()) {
            return createResult(validateForRollingUpgrade(upgradeOption), "The rolling upgrade is not enabled for this blueprint");
        } else {
            return new BlueprintValidationResult(true);
        }
    }

    private boolean validateForRollingUpgrade(BlueprintUpgradeOption upgradeOption) {
        return upgradeOption.isRollingUpgradeEnabled()
                || entitlementService.isSkipRollingUpgradeValidationEnabled(ThreadBasedUserCrnProvider.getAccountId());
    }

    private BlueprintValidationResult createResult(boolean validationResult, String errorMessage) {
        return new BlueprintValidationResult(validationResult, validationResult ? null : errorMessage);
    }
}
