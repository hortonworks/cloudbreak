package com.sequenceiq.cloudbreak.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;
import com.sequenceiq.cloudbreak.exception.ServiceTypeNotFoundException;

@Component
public class CustomConfigsValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomConfigsValidator.class);

    private static ObjectMapper objectMapper = new ObjectMapper();

    private final String serviceWhiteListFileName = "all-services.json";

    private final String minPlatformVersion = "7.2.8";

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
}
