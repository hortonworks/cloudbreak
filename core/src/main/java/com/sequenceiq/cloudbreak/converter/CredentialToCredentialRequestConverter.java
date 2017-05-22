package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class CredentialToCredentialRequestConverter extends AbstractConversionServiceAwareConverter<Credential, CredentialRequest> {

    @Override
    public CredentialRequest convert(Credential source) {
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName(source.getName());
        credentialRequest.setCloudPlatform(source.cloudPlatform());
        credentialRequest.setDescription(source.getDescription());
        credentialRequest.setTopologyId(source.getTopology().getId());
        credentialRequest.setPublicKey(source.getPublicKey());
        credentialRequest.setParameters(source.getAttributes().getMap());
        return credentialRequest;
    }
}
