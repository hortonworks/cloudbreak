package com.sequenceiq.cloudbreak.converter;

import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;
import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.validation.RequiredAzureCredentialParam;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

@Component
public class JsonToAzureCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialRequest, AzureCredential> {
    private static final String AZURE_USER_NAME = "cloudbreak";

    @Override
    public AzureCredential convert(CredentialRequest source) {
        AzureCredential azureCredential = new AzureCredential();
        azureCredential.setJks(AzureStackUtil.DEFAULT_JKS_PASS);
        azureCredential.setName(source.getName());
        azureCredential.setDescription(source.getDescription());
        azureCredential.setSubscriptionId(String.valueOf(source.getParameters().get(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName())));
        azureCredential.setPostFix(String.valueOf(new Date().getTime()));
        azureCredential.setPublicKey(source.getPublicKey());
        azureCredential.setPublicKey(StringUtils.newStringUtf8(Base64.decodeBase64(source.getPublicKey())));
        azureCredential.setLoginUserName(source.getLoginUserName() == null ? AZURE_USER_NAME : source.getLoginUserName());
        return azureCredential;
    }
}
