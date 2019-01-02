package com.sequenceiq.cloudbreak.converter.v4.stacks.view;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.CredentialViewV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Credential;

@Component
public class CredentialToCredentialViewV4ResponseConverter extends AbstractConversionServiceAwareConverter<Credential, CredentialViewV4Response> {

    @Override
    public CredentialViewV4Response convert(Credential source) {
        CredentialViewV4Response credentialJson = new CredentialViewV4Response();
        credentialJson.setName(source.getName());
        credentialJson.setCloudPlatform(source.cloudPlatform());
        credentialJson.setGovCloud(source.getGovCloud());
        return credentialJson;
    }

}
