package com.sequenceiq.cloudbreak.service.upgrade.image;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;

@Component
public class BlueprintBasedUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintBasedUpgradeValidator.class);

    @Inject
    private BlueprintUpgradeOptionValidator blueprintUpgradeOptionValidator;

    public BlueprintValidationResult isValidBlueprint(ImageFilterParams imageFilterParams, String accountId) {
        String blueprintName = imageFilterParams.getBlueprint().getName();
        LOGGER.debug("Validating blueprint for upgrade: {}", blueprintName);
        if (imageFilterParams.getStackType().equals(StackType.DATALAKE)) {
            return new BlueprintValidationResult(true);
        } else {
            return blueprintUpgradeOptionValidator.isValidBlueprint(imageFilterParams.getBlueprint(), imageFilterParams.isLockComponents(),
                    imageFilterParams.isSkipValidations(), imageFilterParams.isDataHubUpgradeEntitled());
        }
    }

}
