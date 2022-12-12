package com.sequenceiq.environment.credential.validation.aws;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.credential.AppAuthenticationType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
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
        AzureCredentialRequestParameters azureCredentialRequestParameters = credentialRequest.getAzure();
        if (azureCredentialRequestParameters == null) {
            resultBuilder.error("Azure specific parameters are missing from the credential creation request");
        } else if (azureCredentialRequestParameters.getAppBased() == null && azureCredentialRequestParameters.getRoleBased() == null) {
            resultBuilder.error("appBaseRequest or roleBasedRequest have to be defined in azure specific parameters");
        } else {
            AppBasedRequest appBasedRequest = azureCredentialRequestParameters.getAppBased();
            if (appBasedRequest == null || appBasedRequest.getAuthenticationType() == AppAuthenticationType.SECRET) {
                if (StringUtils.isBlank(azureCredentialRequestParameters.getSubscriptionId())) {
                    resultBuilder.error("subscriptionId is mandatory for " + (appBasedRequest == null ? " role based " : " secret based ") +
                            " azure credential creation");
                }
                if (StringUtils.isBlank(azureCredentialRequestParameters.getTenantId())) {
                    resultBuilder.error("tenantId is mandatory for " + (appBasedRequest == null ? " role based " : " secret based ") +
                            " azure credential creation");
                }
            }
        }
        return resultBuilder.build();
    }

    @Override
    public ValidationResult validateUpdate(Credential original, Credential newCred, ValidationResultBuilder resultBuilder) {
        if (newCred.getAttributes() == null) {
            resultBuilder.error("Credential attributes are missing from the credential modification request");
        } else {
            try {
                CredentialAttributes attributes = new Json(newCred.getAttributes()).get(CredentialAttributes.class);
                if (attributes.getAzure() == null) {
                    resultBuilder.error("Azure specific parameters are missing from the credential modification request");
                } else {
                    AzureCredentialAttributes azureCredentialAttributes = attributes.getAzure();
                    if (StringUtils.isBlank(azureCredentialAttributes.getSubscriptionId())) {
                        resultBuilder.error("subscriptionId is mandatory for azure credential modification");
                    }
                    if (StringUtils.isBlank(azureCredentialAttributes.getTenantId())) {
                        resultBuilder.error("tenantId is mandatory for azure credential modification");
                    }
                }
            } catch (IOException ex) {
                resultBuilder.error("Provider specific attributes cannot be read: " + ex.getMessage());
            }
        }
        return resultBuilder.build();
    }
}
