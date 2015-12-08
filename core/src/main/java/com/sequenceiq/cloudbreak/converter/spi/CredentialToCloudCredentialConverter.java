package com.sequenceiq.cloudbreak.converter.spi;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.credential.CredentialDefinitionService;

@Component
public class CredentialToCloudCredentialConverter {

    private static final String CREDENTIAL_ID = "id";

    @Inject
    private CredentialDefinitionService definitionService;

    public CloudCredential convert(Credential credential) {
        Json attributes = credential.getAttributes();
        Map<String, Object> fields = attributes == null ? Collections.<String, Object>emptyMap() : attributes.getMap();
        fields = definitionService.revertProperties(credential.cloudPlatform(), fields);
        fields.put(CREDENTIAL_ID, credential.getId());
        return new CloudCredential(credential.getId(), credential.getName(), credential.getPublicKey(), credential.getLoginUserName(), fields);
    }

}
