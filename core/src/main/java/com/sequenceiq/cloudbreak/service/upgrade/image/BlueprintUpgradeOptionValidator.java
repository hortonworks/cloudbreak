package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.ENABLED;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

@Component
class BlueprintUpgradeOptionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintUpgradeOptionValidator.class);

    @Inject
    private CustomTemplateUpgradeValidator customTemplateUpgradeValidator;

    BlueprintValidationResult isValidBlueprint(Blueprint blueprint, boolean lockComponents, boolean skipValidations, boolean dataHubUpgradeEntitled) {
        LOGGER.debug("Validating blueprint upgrade option. Name: {}, type: {}, upgradeOption: {}, lockComponents: {}", blueprint.getName(),
                blueprint.getStatus(), blueprint.getBlueprintUpgradeOption(), lockComponents);
        return isDefaultBlueprint(blueprint) ? isEnabledForDefaultBlueprint(lockComponents, blueprint, skipValidations, dataHubUpgradeEntitled)
                : isEnabledForCustomBlueprint(blueprint, skipValidations, dataHubUpgradeEntitled);
    }

    private boolean isDefaultBlueprint(Blueprint blueprint) {
        return blueprint.getStatus().isDefault();
    }

    private BlueprintValidationResult isEnabledForDefaultBlueprint(boolean lockComponents, Blueprint blueprint, boolean skipValidations,
            boolean dataHubUpgradeEntitled) {
        BlueprintUpgradeOption upgradeOption = getBlueprintUpgradeOption(blueprint);
        BlueprintValidationResult result;
        if (skipValidations) {
            LOGGER.debug("Blueprint options are not validated if the request is internal");
            result = createResult(true);
        } else if (upgradeOption == BlueprintUpgradeOption.GA) {
            LOGGER.debug("Blueprint marks this GA so upgrade is enabled.");
            result = createResult(true);
        } else {
            result = createResult(lockComponents ? upgradeOption.isOsUpgradeEnabled() : isRuntimeUpgradeEnabled(upgradeOption, dataHubUpgradeEntitled));
        }
        return result;
    }

    private boolean isRuntimeUpgradeEnabled(BlueprintUpgradeOption upgradeOption, boolean dataHubUpgradeEntitled) {
        boolean runtimeUpgradeEnabled = upgradeOption.isRuntimeUpgradeEnabled(dataHubUpgradeEntitled);
        if (dataHubUpgradeEntitled) {
            LOGGER.info("Data Hub upgrade entitlement is enabled, check the upgrade option to see if the template"
                    + " is eligible for upgrade or not, upgrade option: {}, eligible: {}", upgradeOption, runtimeUpgradeEnabled);
        } else {
            LOGGER.info("Data Hub upgrade entitlement is disabled, check the upgrade option to see if the template"
                    + " is eligible for maintenance upgrade or not, upgrade option: {}, eligible: {}", upgradeOption, runtimeUpgradeEnabled);
        }
        return runtimeUpgradeEnabled;
    }

    private BlueprintUpgradeOption getBlueprintUpgradeOption(Blueprint blueprint) {
        return Optional.ofNullable(blueprint.getBlueprintUpgradeOption()).orElse(ENABLED);
    }

    private BlueprintValidationResult isEnabledForCustomBlueprint(Blueprint blueprint, boolean skipValidations, boolean dataHubUpgradeEntitled) {
        BlueprintValidationResult result;
        if (skipValidations || dataHubUpgradeEntitled) {
            LOGGER.debug("Custom blueprint options are not validated if the request is internal or the DH upgrade entitlement is granted");
            result = createResult(true);
        } else {
            result = customTemplateUpgradeValidator.isValid(blueprint);
        }
        return result;
    }

    private BlueprintValidationResult createResult(boolean result) {
        return new BlueprintValidationResult(result,
                result ? null : "The cluster template is not eligible for upgrade");
    }
}
