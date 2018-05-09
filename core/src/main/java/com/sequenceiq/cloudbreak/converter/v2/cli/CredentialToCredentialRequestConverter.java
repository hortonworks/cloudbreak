package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class CredentialToCredentialRequestConverter
        extends AbstractConversionServiceAwareConverter<Credential, CredentialRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialToCredentialRequestConverter.class);

    @Override
    public CredentialRequest convert(Credential source) {
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName("");
        credentialRequest.setDescription(source.getDescription());
        credentialRequest.setCloudPlatform(source.cloudPlatform());
        credentialRequest.setParameters(cleanMap(source.getAttributes().getMap()));
        return credentialRequest;
    }
}
