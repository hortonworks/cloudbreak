package com.sequenceiq.cloudbreak.converter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import org.apache.commons.codec.binary.Base64;
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
        credentialJson.setPublicKey(entity.getPublicKey());
        credentialJson.setPublicInAccount(entity.isPublicInAccount());
        Map<String, Object> params = new HashMap<>();
        params.put(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName(), entity.getSubscriptionId());
        credentialJson.setParameters(params);
        return credentialJson;
    }

    @Override
    public AzureCredential convert(CredentialJson json) {
        AzureCredential azureCredential = new AzureCredential();
        azureCredential.setJks(AzureStackUtil.DEFAULT_JKS_PASS);
        azureCredential.setName(json.getName());
        azureCredential.setDescription(json.getDescription());
        azureCredential.setSubscriptionId(String.valueOf(json.getParameters().get(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName())));
        azureCredential.setPostFix(String.valueOf(new Date().getTime()));
        azureCredential.setCloudPlatform(CloudPlatform.AZURE);
        azureCredential.setPublicInAccount(json.isPublicInAccount());
        azureCredential.setPublicKey(json.getPublicKey());
        azureCredential.setPublicKey(StringUtils.newStringUtf8(Base64.decodeBase64(json.getPublicKey())));
        return azureCredential;
    }
}
