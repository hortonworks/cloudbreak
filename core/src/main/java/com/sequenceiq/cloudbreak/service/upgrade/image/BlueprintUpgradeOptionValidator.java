package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.DISABLED;
import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.ENABLED;
import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.OS_UPGRADE_ENABLED;

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

    BlueprintValidationResult isValidBlueprint(Blueprint blueprint, boolean lockComponents) {
        LOGGER.debug("Validating blueprint upgrade option. Name: {}, type: {}, upgradeOption: {}, lockComponents: {}", blueprint.getName(),
                blueprint.getStatus(), blueprint.getBlueprintUpgradeOption(), lockComponents);
        return isDefaultBlueprint(blueprint) ? isEnabledForDefaultBlueprint(lockComponents, blueprint) : isEnabledForCustomBlueprint(blueprint);
    }

    private boolean isDefaultBlueprint(Blueprint blueprint) {
        return blueprint.getStatus().isDefault();
    }

    private BlueprintValidationResult isEnabledForDefaultBlueprint(boolean lockComponents, Blueprint blueprint) {
        BlueprintUpgradeOption upgradeOption = getBlueprintUpgradeOption(blueprint);
        return createResult(lockComponents ? isOsUpgradeEnabled(upgradeOption) : isRuntimeUpgradeEnabled(upgradeOption), upgradeOption);
    }

    private boolean isOsUpgradeEnabled(BlueprintUpgradeOption upgradeOption) {
        return OS_UPGRADE_ENABLED.equals(upgradeOption) || ENABLED.equals(upgradeOption);
    }

    private boolean isRuntimeUpgradeEnabled(BlueprintUpgradeOption upgradeOption) {
        return !DISABLED.equals(upgradeOption);
    }

    private BlueprintUpgradeOption getBlueprintUpgradeOption(Blueprint blueprint) {
        return Optional.ofNullable(blueprint.getBlueprintUpgradeOption()).orElse(ENABLED);
    }

    private BlueprintValidationResult isEnabledForCustomBlueprint(Blueprint blueprint) {
        return customTemplateUpgradeValidator.isValid(blueprint);
    }

    private BlueprintValidationResult createResult(boolean result, BlueprintUpgradeOption upgradeOption) {
        return new BlueprintValidationResult(result,
                result ? null : String.format("The cluster template is not eligible for upgrade because the upgrade option is: %s", upgradeOption));
    }
}
