package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.controller.validation.GcpCredentialParam;
import com.sequenceiq.cloudbreak.domain.GcpCredential;

@Component
public class JsonToGcpCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialRequest, GcpCredential> {

    @Override
    public GcpCredential convert(CredentialRequest source) {
        GcpCredential gcpCredential = new GcpCredential();
        gcpCredential.setName(source.getName());
        gcpCredential.setServiceAccountId(String.valueOf(source.getParameters().get(GcpCredentialParam.SERVICE_ACCOUNT_ID.getName())));
        gcpCredential.setServiceAccountPrivateKey(String.valueOf(source.getParameters().get(GcpCredentialParam.SERVICE_ACCOUNT_PRIVATE_KEY.getName())));
        gcpCredential.setProjectId(String.valueOf(source.getParameters().get(GcpCredentialParam.PROJECTID.getName())));
        gcpCredential.setDescription(source.getDescription());
        gcpCredential.setPublicKey(source.getPublicKey());
        return gcpCredential;
    }
}
