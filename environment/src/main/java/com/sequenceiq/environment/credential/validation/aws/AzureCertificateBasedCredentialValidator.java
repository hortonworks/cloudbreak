package com.sequenceiq.environment.credential.validation.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;

@Component
public class AzureCertificateBasedCredentialValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCertificateBasedCredentialValidator.class);

    public ValidationResult.ValidationResultBuilder validateCreate(AppBasedRequest appBasedRequest,
            ValidationResult.ValidationResultBuilder resultBuilder) {

        return resultBuilder;
    }
}
