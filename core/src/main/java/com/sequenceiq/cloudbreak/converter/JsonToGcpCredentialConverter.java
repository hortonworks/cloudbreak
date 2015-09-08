package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.validation.GcpCredentialParam;
import com.sequenceiq.cloudbreak.domain.GcpCredential;

@Component
public class JsonToGcpCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialRequest, GcpCredential> {
    private static final String GCP_USER_NAME = "cloudbreak";

    @Override
    public GcpCredential convert(CredentialRequest source) {
        GcpCredential gcpCredential = new GcpCredential();
        gcpCredential.setName(source.getName());
        gcpCredential.setServiceAccountId(String.valueOf(source.getParameters().get(GcpCredentialParam.SERVICE_ACCOUNT_ID.getName())));
        gcpCredential.setServiceAccountPrivateKey(String.valueOf(source.getParameters().get(GcpCredentialParam.SERVICE_ACCOUNT_PRIVATE_KEY.getName())));
        gcpCredential.setProjectId(String.valueOf(source.getParameters().get(GcpCredentialParam.PROJECTID.getName())));
        gcpCredential.setDescription(source.getDescription());
        gcpCredential.setPublicKey(source.getPublicKey());
        gcpCredential.setLoginUserName(source.getLoginUserName() == null ? GCP_USER_NAME : source.getLoginUserName());
        return gcpCredential;
    }
}
