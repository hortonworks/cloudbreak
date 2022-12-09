package com.sequenceiq.environment.credential.validation.aws;

import static com.sequenceiq.common.api.credential.AppAuthenticationType.CERTIFICATE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.ProviderCredentialValidator;

@Component
public class AzureCredentialValidator implements ProviderCredentialValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialValidator.class);

    @Inject
    private AzureSecretBasedCredentialValidator azureSecretBasedCredentialValidator;

    @Inject
    private AzureCertificateBasedCredentialValidator azureCertificateBasedCredentialValidator;

    @Override
    public String supportedProvider() {
        return CloudPlatform.AZURE.name();
    }

    @Override
    public ValidationResult validateCreate(CredentialRequest credentialRequest, ValidationResultBuilder resultBuilder) {
        if (credentialRequest.getAzure() == null) {
            LOGGER.error("Azure credential properties are not present in credential create request! Request: {}", credentialRequest);
            resultBuilder.error("Azure credential properties are not present in the request!");
            return resultBuilder.build();
        }
        AppBasedRequest appBasedRequest = credentialRequest.getAzure().getAppBased();
        if (appBasedRequest != null) {
            if (appBasedRequest.getAuthenticationType() == CERTIFICATE) {
                azureCertificateBasedCredentialValidator.validateCreate(appBasedRequest, resultBuilder);
            } else {
                azureSecretBasedCredentialValidator.validateCreate(appBasedRequest, resultBuilder);
            }
        }

        return resultBuilder.build();
    }

    @Override
    public ValidationResult validateUpdate(Credential original, Credential newCred, ValidationResultBuilder resultBuilder) {
        return resultBuilder.build();
    }
}
