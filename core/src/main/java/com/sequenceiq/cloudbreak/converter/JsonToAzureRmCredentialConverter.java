package com.sequenceiq.cloudbreak.converter;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;
import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.validation.RequiredAzureRmCredentialParam;
import com.sequenceiq.cloudbreak.domain.AzureRmCredential;

@Component
public class JsonToAzureRmCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialRequest, AzureRmCredential> {
    @Override
    public AzureRmCredential convert(CredentialRequest source) {
        AzureRmCredential azureCredential = new AzureRmCredential();
        azureCredential.setName(source.getName());
        azureCredential.setDescription(source.getDescription());
        azureCredential.setSubscriptionId(String.valueOf(source.getParameters().get(RequiredAzureRmCredentialParam.SUBSCRIPTION_ID.getName())));
        azureCredential.setSecretKey(String.valueOf(source.getParameters().get(RequiredAzureRmCredentialParam.SECRET_KEY.getName())));
        azureCredential.setAccesKey(String.valueOf(source.getParameters().get(RequiredAzureRmCredentialParam.ACCES_KEY.getName())));
        azureCredential.setTenantId(String.valueOf(source.getParameters().get(RequiredAzureRmCredentialParam.TENANT_ID.getName())));
        azureCredential.setPublicKey(StringUtils.newStringUtf8(Base64.decodeBase64(source.getPublicKey())));
        return azureCredential;
    }
}
