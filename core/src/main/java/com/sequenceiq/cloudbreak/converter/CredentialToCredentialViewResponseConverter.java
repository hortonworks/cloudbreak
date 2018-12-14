package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialViewResponse;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class CredentialToCredentialViewResponseConverter extends AbstractConversionServiceAwareConverter<Credential, CredentialViewResponse> {

    @Override
    public CredentialViewResponse convert(Credential source) {
        CredentialViewResponse credentialJson = new CredentialViewResponse();
        credentialJson.setName(source.getName());
        credentialJson.setCloudPlatform(source.cloudPlatform());
        credentialJson.setGovCloud(source.getGovCloud());
        return credentialJson;
    }

}
