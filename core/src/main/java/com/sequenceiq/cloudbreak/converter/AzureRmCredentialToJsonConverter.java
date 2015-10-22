package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import com.sequenceiq.cloudbreak.controller.validation.RequiredAzureRmCredentialParam;
import com.sequenceiq.cloudbreak.domain.AzureRmCredential;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

@Component
public class AzureRmCredentialToJsonConverter extends AbstractConversionServiceAwareConverter<AzureRmCredential, CredentialResponse> {
    @Override
    public CredentialResponse convert(AzureRmCredential source) {
        CredentialResponse credentialJson = new CredentialResponse();
        credentialJson.setId(source.getId());
        credentialJson.setCloudPlatform(CloudPlatform.AZURE_RM);
        credentialJson.setName(source.getName());
        credentialJson.setDescription(source.getDescription());
        credentialJson.setPublicKey(source.getPublicKey());
        credentialJson.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> params = new HashMap<>();
        params.put(RequiredAzureRmCredentialParam.SUBSCRIPTION_ID.getName(), source.getSubscriptionId());
        params.put(RequiredAzureRmCredentialParam.ACCES_KEY.getName(), source.getAccesKey());
        params.put(RequiredAzureRmCredentialParam.TENANT_ID.getName(), source.getTenantId());
        params.put(RequiredAzureRmCredentialParam.SECRET_KEY.getName(), source.getSecretKey());
        credentialJson.setParameters(params);
        credentialJson.setLoginUserName(source.getLoginUserName());
        return credentialJson;
    }
}
