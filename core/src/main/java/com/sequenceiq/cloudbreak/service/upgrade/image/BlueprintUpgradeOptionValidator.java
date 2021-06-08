package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.DISABLED;
import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.ENABLED;
import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.OS_UPGRADE_ENABLED;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

@Component
class BlueprintUpgradeOptionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintUpgradeOptionValidator.class);

    boolean isValidBlueprint(Blueprint blueprint, boolean lockComponents) {
        LOGGER.debug("Validating blueprint upgrade option. Name: {}, type: {}, upgradeOption: {}, lockComponents: {}", blueprint.getName(),
                blueprint.getStatus(), blueprint.getBlueprintUpgradeOption(), lockComponents);
        return isCustomBlueprint(blueprint) || isEnabled(lockComponents, blueprint);
    }

    private boolean isCustomBlueprint(Blueprint blueprint) {
        return !blueprint.getStatus().isDefault();
    }

    private boolean isEnabled(boolean lockComponents, Blueprint blueprint) {
        return lockComponents ? isOsUpgradeEnabled(blueprint) : isRuntimeUpgradeEnabled(blueprint);
    }

    private boolean isOsUpgradeEnabled(Blueprint blueprint) {
        BlueprintUpgradeOption upgradeOption = getBlueprintUpgradeOption(blueprint);
        return OS_UPGRADE_ENABLED.equals(upgradeOption) || ENABLED.equals(upgradeOption);
    }

    private boolean isRuntimeUpgradeEnabled(Blueprint blueprint) {
        return !DISABLED.equals(getBlueprintUpgradeOption(blueprint));
    }

    private BlueprintUpgradeOption getBlueprintUpgradeOption(Blueprint blueprint) {
        return Optional.ofNullable(blueprint.getBlueprintUpgradeOption())
                .orElse(ENABLED);
    }
}
