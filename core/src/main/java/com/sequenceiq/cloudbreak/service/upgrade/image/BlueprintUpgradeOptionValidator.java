package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption.ENABLED;

import java.util.Optional;

import jakarta.inject.Inject;

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

    @Inject
    private BlueprintUpgradeOptionCondition blueprintUpgradeOptionCondition;

    BlueprintValidationResult isValidBlueprint(ImageFilterParams imageFilterParams) {
        LOGGER.debug("Validating blueprint upgrade option. {}", imageFilterParams);
        if (imageFilterParams.isSkipValidations()) {
            LOGGER.debug("Skipping blueprint validation because the skipValidations flag is added to the request");
            return new BlueprintValidationResult(true);
        } else {
            Blueprint blueprint = imageFilterParams.getBlueprint();
            return isDefaultBlueprint(blueprint) ?
                    isEnabledForDefaultBlueprint(imageFilterParams, blueprint) :
                    isEnabledForCustomBlueprint(blueprint, imageFilterParams.isDataHubUpgradeEntitled());
        }
    }

    private boolean isDefaultBlueprint(Blueprint blueprint) {
        return blueprint.getStatus().isDefault();
    }

    private BlueprintValidationResult isEnabledForDefaultBlueprint(ImageFilterParams imageFilterParams, Blueprint blueprint) {
        BlueprintUpgradeOption upgradeOption = getBlueprintUpgradeOption(blueprint);
        return blueprintUpgradeOptionCondition.validate(imageFilterParams, upgradeOption);
    }

    private BlueprintUpgradeOption getBlueprintUpgradeOption(Blueprint blueprint) {
        return Optional.ofNullable(blueprint.getBlueprintUpgradeOption()).orElse(ENABLED);
    }

    private BlueprintValidationResult isEnabledForCustomBlueprint(Blueprint blueprint, boolean dataHubUpgradeEntitled) {
        if (dataHubUpgradeEntitled) {
            LOGGER.debug("Custom blueprint options are not validated if the  DH upgrade entitlement is granted");
            return new BlueprintValidationResult(true);
        } else {
            return customTemplateUpgradeValidator.isValid(blueprint);
        }
    }
}
