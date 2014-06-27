package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.validation.RequiredAzureCredentialParam;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Component
public class AzureCredentialConverter extends AbstractConverter<CredentialJson, AzureCredential> {


    @Override
    public CredentialJson convert(AzureCredential entity) {
        CredentialJson credentialJson = new CredentialJson();
        credentialJson.setId(entity.getId());
        credentialJson.setCloudPlatform(CloudPlatform.AZURE);
        credentialJson.setName(entity.getName());
        credentialJson.setDescription(entity.getDescription());
        Map<String, Object> params = new HashMap<>();
        params.put(RequiredAzureCredentialParam.JKS_PASSWORD.getName(), entity.getJks());
        params.put(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName(), entity.getSubscriptionId());
        credentialJson.setParameters(params);
        return credentialJson;
    }

    @Override
    public AzureCredential convert(CredentialJson json) {
        AzureCredential azureCredential = new AzureCredential();
        azureCredential.setJks(String.valueOf(json.getParameters().get(RequiredAzureCredentialParam.JKS_PASSWORD.getName())));
        azureCredential.setName(json.getName());
        azureCredential.setDescription(json.getDescription());
        azureCredential.setSubscriptionId(String.valueOf(json.getParameters().get(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName())));
        azureCredential.setCloudPlatform(CloudPlatform.AZURE);
        return azureCredential;
    }
}
