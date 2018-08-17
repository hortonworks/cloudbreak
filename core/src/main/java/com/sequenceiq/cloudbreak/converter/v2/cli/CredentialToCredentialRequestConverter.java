package com.sequenceiq.cloudbreak.converter.v2.cli;

import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Credential;

import javax.inject.Inject;

@Component
public class CredentialToCredentialRequestConverter
        extends AbstractConversionServiceAwareConverter<Credential, CredentialRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialToCredentialRequestConverter.class);

    @Inject
    private CredentialValidator credentialValidator;

    @Override
    public CredentialRequest convert(Credential source) {
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName("");
        credentialRequest.setDescription(source.getDescription());
        credentialValidator.validateCredentialCloudPlatform(source.cloudPlatform());
        credentialRequest.setCloudPlatform(source.cloudPlatform());
        credentialRequest.setParameters(cleanMap(source.getAttributes().getMap()));
        return credentialRequest;
    }
}
