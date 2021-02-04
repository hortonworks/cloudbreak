package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

@Component
class BlueprintUpgradeOptionValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintUpgradeOptionValidator.class);

    boolean isValidBlueprint(Blueprint blueprint) {
        LOGGER.debug("Validating blueprint upgrade option. Name: {}, type: {}, upgradeOption: {}", blueprint.getName(), blueprint.getStatus(),
                blueprint.getBlueprintUpgradeOption());
        return !blueprint.getStatus().isDefault() || (blueprint.getStatus().isDefault() && isEnabled(blueprint));
    }

    private boolean isEnabled(Blueprint blueprint) {
        return BlueprintUpgradeOption.ENABLED.equals(getBlueprintUpgradeOption(blueprint));
    }

    private BlueprintUpgradeOption getBlueprintUpgradeOption(Blueprint blueprint) {
        return Optional.ofNullable(blueprint.getBlueprintUpgradeOption())
                .orElse(BlueprintUpgradeOption.ENABLED);
    }
}
