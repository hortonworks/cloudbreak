package com.sequenceiq.environment.credential.validation.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.ProviderCredentialValidator;

@Component
public class AzureCredentialValidator implements ProviderCredentialValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialValidator.class);

    @Override
    public String supportedProvider() {
        return CloudPlatform.AZURE.name();
    }

    @Override
    public ValidationResult validateCreate(CredentialRequest credentialRequest, ValidationResultBuilder resultBuilder) {
        return resultBuilder.build();
    }

    @Override
    public ValidationResult validateUpdate(Credential original, Credential newCred, ValidationResultBuilder resultBuilder) {
        return resultBuilder.build();
    }
}
