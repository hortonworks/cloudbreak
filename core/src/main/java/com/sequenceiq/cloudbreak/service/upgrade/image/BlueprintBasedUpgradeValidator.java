package com.sequenceiq.cloudbreak.service.upgrade.image;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Component
public class BlueprintBasedUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintBasedUpgradeValidator.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private BlueprintUpgradeOptionValidator blueprintUpgradeOptionValidator;

    public BlueprintValidationResult isValidBlueprint(ImageFilterParams imageFilterParams, String accountId) {
        String blueprintName = imageFilterParams.getBlueprint().getName();
        LOGGER.debug("Validating blueprint for upgrade: {}", blueprintName);
        if (imageFilterParams.getStackType().equals(StackType.DATALAKE)) {
            boolean mediumDuty = blueprintName.contains("SDX Medium Duty");
            boolean canUpgradeMediumDuty = mediumDuty && isHaUpgradeEnabled(accountId);
            return new BlueprintValidationResult(!mediumDuty || canUpgradeMediumDuty, "The upgrade is not allowed for this template.");
        } else {
            return blueprintUpgradeOptionValidator.isValidBlueprint(imageFilterParams.getBlueprint(), imageFilterParams.isLockComponents(),
                    imageFilterParams.isSkipValidations(), imageFilterParams.isDataHubUpgradeEntitled());
        }
    }

    private boolean isHaUpgradeEnabled(String accountId) {
        boolean haUpgradeEnabled = entitlementService.haUpgradeEnabled(accountId);
        LOGGER.debug("DataLake HA upgrade enabled by entitlement: {}", haUpgradeEnabled);
        return haUpgradeEnabled;
    }
}
