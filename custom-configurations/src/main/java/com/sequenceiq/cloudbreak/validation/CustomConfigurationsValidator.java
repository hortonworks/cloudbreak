package com.sequenceiq.cloudbreak.validation;


import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;

@Component
public class CustomConfigurationsValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomConfigurationsValidator.class);

    public void validateServiceNames(CustomConfigurations customConfigurations) {
        LOGGER.info("Validating service names for Custom Configs " + customConfigurations.getName());
        customConfigurations.getConfigurations().forEach(config -> {
            try {
                AllServiceTypes.valueOf(config.getServiceType().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Service name with " + config.getServiceType() + " does not exist.");
            }
        });
    }
}

