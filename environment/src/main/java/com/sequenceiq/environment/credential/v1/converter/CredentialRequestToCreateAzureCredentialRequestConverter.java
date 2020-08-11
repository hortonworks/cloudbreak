package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.environments.model.CreateAzureCredentialRequest;
import com.cloudera.cdp.environments.model.CreateAzureCredentialRequestAppBased;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;

@Component
public class CredentialRequestToCreateAzureCredentialRequestConverter {

    public CreateAzureCredentialRequest convert(CredentialRequest source) {
        CreateAzureCredentialRequest credentialRequest = new CreateAzureCredentialRequest();
        credentialRequest.setCredentialName(source.getName());
        credentialRequest.setDescription(source.getDescription());
        credentialRequest.setSubscriptionId(source.getAzure().getSubscriptionId());
        credentialRequest.setTenantId(source.getAzure().getTenantId());
        if (source.getAzure().getAppBased() != null) {
            CreateAzureCredentialRequestAppBased appBasedProperties = new CreateAzureCredentialRequestAppBased();
            appBasedProperties.setApplicationId(source.getAzure().getAppBased().getAccessKey());
            appBasedProperties.setSecretKey(source.getAzure().getAppBased().getSecretKey());
            credentialRequest.setAppBased(appBasedProperties);
        }
        return credentialRequest;
    }

}
