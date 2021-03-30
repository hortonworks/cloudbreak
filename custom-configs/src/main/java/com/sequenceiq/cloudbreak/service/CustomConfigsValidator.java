package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;
import com.sequenceiq.cloudbreak.exception.ServiceTypeNotFoundException;

@Component
public class CustomConfigsValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomConfigsValidator.class);

    private static ObjectMapper objectMapper = new ObjectMapper();

    private final String serviceWhiteListFileName = "all-services.json";

    @Inject
    private EntitlementService entitlementService;

    public void validateServiceNames(CustomConfigs customConfigs) throws IOException {
        JsonNode allServicesJsonArray = objectMapper.readTree(new ClassPathResource(serviceWhiteListFileName).getInputStream()).get("services");
        List<String> allServiceNames = Arrays.asList(objectMapper.readValue(allServicesJsonArray.toString(), String[].class));
        LOGGER.info("Validating service names for Custom Configs " + customConfigs.getName());
        customConfigs.getConfigs().forEach(config -> {
            Optional<String> serviceNameIfExists = allServiceNames.stream()
                    .filter(serviceName -> serviceName.equalsIgnoreCase(config.getServiceType()))
                    .findFirst();
            if (serviceNameIfExists.isEmpty()) {
                throw new ServiceTypeNotFoundException("Service with name " + config.getServiceType() + " does not exist.");
            }
        });
    }

    public void validateIfAccountIsEntitled() {
        if (!entitlementService.datahubCustomConfigsEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            throw new BadRequestException("Custom configs not enabled for account");
        }
    }
}

