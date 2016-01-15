package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;

@Component
public class CredentialToJsonConverter extends AbstractConversionServiceAwareConverter<Credential, CredentialResponse> {
    @Override
    public CredentialResponse convert(Credential source) {
        CredentialResponse credentialJson = new CredentialResponse();
        credentialJson.setId(source.getId());
        credentialJson.setCloudPlatform(source.cloudPlatform());
        credentialJson.setName(source.getName());
        credentialJson.setPublicInAccount(source.isPublicInAccount());
        Json attributes = source.getAttributes();
        if (attributes != null) {
            credentialJson.setParameters(attributes.getMap());
        }
        credentialJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        credentialJson.setPublicKey(source.getPublicKey());
        credentialJson.setLoginUserName(source.getLoginUserName());
        return credentialJson;
    }
}
