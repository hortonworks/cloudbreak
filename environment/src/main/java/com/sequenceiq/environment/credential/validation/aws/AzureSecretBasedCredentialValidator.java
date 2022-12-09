package com.sequenceiq.environment.credential.validation.aws;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;

@Component
public class AzureSecretBasedCredentialValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSecretBasedCredentialValidator.class);

    public ValidationResult.ValidationResultBuilder validateCreate(AppBasedRequest appBasedRequest,
            ValidationResult.ValidationResultBuilder resultBuilder) {

        if (StringUtils.isEmpty(appBasedRequest.getAccessKey())) {
            LOGGER.warn("AccessKey cannot be empty secret based Azure credential! AccessKey: {}", appBasedRequest.getAccessKey());
            resultBuilder.error("AccessKey cannot be empty secret based Azure credential!");
        }

        if (StringUtils.isEmpty(appBasedRequest.getSecretKey())) {
            LOGGER.warn("Secret cannot be empty secret based Azure credential!");
            resultBuilder.error("Secret cannot be empty secret based Azure credential!");
        }
        return resultBuilder;
    }
}
