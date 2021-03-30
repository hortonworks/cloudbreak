package com.sequenceiq.cloudbreak.validation;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.exception.CustomConfigurationsServiceTypeNotFoundException;

@Component
public class CustomConfigurationsValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomConfigurationsValidator.class);

    @Inject
    private EntitlementService entitlementService;

    public void validateServiceNames(CustomConfigurations customConfigurations) {
        LOGGER.info("Validating service names for Custom Configs " + customConfigurations.getName());
        customConfigurations.getConfigurations().forEach(config -> {
            try {
                AllServiceTypes.valueOf(config.getServiceType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CustomConfigurationsServiceTypeNotFoundException("Service name with " + config.getServiceType() + " does not exist.");
            }
        });
    }

    public void validateIfAccountIsEntitled(String accountId) {
        if (!entitlementService.datahubCustomConfigsEnabled(accountId)) {
            throw new BadRequestException("Custom configs not enabled for account");
        }
    }
}

