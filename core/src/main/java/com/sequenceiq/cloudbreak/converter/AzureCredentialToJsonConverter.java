package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.validation.RequiredAzureCredentialParam;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Component
public class AzureCredentialToJsonConverter extends AbstractConversionServiceAwareConverter<AzureCredential, CredentialResponse> {
    @Override
    public CredentialResponse convert(AzureCredential source) {
        CredentialResponse credentialJson = new CredentialResponse();
        credentialJson.setId(source.getId());
        credentialJson.setCloudPlatform(CloudPlatform.AZURE);
        credentialJson.setName(source.getName());
        credentialJson.setDescription(source.getDescription());
        credentialJson.setPublicKey(source.getPublicKey());
        credentialJson.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> params = new HashMap<>();
        params.put(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName(), source.getSubscriptionId());
        credentialJson.setParameters(params);
        return credentialJson;
    }
}
