package com.sequenceiq.provisioning.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.CredentialJson;
import com.sequenceiq.provisioning.controller.validation.RequiredAzureCredentialParam;
import com.sequenceiq.provisioning.domain.AzureCredential;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@Component
public class AzureCredentialConverter extends AbstractConverter<CredentialJson, AzureCredential> {


    @Override
    public CredentialJson convert(AzureCredential entity) {
        CredentialJson credentialJson = new CredentialJson();
        credentialJson.setId(entity.getId());
        credentialJson.setCloudPlatform(CloudPlatform.AZURE);
        Map<String, String> params = new HashMap<>();
        params.put(RequiredAzureCredentialParam.JKS_PASSWORD.getName(), entity.getJks());
        params.put(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName(), entity.getSubscriptionId());
        credentialJson.setParameters(params);
        return credentialJson;
    }

    @Override
    public AzureCredential convert(CredentialJson json) {
        AzureCredential azureCredential = new AzureCredential();
        azureCredential.setJks(json.getParameters().get(RequiredAzureCredentialParam.JKS_PASSWORD.getName()));
        azureCredential.setSubscriptionId(json.getParameters().get(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName()));
        azureCredential.setCloudPlatform(CloudPlatform.AZURE);
        return azureCredential;
    }
}
