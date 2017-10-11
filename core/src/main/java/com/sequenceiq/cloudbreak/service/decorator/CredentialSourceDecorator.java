package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.CredentialSourceRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

@Component
public class CredentialSourceDecorator {

    @Inject
    private CredentialService credentialService;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    public Credential decorate(Credential credential, CredentialSourceRequest credentialSourceRequest, IdentityUser user) {
        if (credential == null) {
            if (!Strings.isNullOrEmpty(credentialSourceRequest.getSourceName())) {
                credential = credentialService.get(credentialSourceRequest.getSourceName(), user.getAccount());
            } else {
                credential = credentialService.get(credentialSourceRequest.getSourceId());
            }

            if (credential == null) {
                throw new BadRequestException("Source credential does not exist!");
            } else {
                Map<String, Object> map = credential.getAttributes().getMap();
                for (Entry<String, Object> stringObjectEntry : credentialSourceRequest.getParameters().entrySet()) {
                    map.put(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
                }
                credential.setId(null);
                credential.setName(missingResourceNameGenerator.generateName(APIResourceType.CREDENTIAL));
                try {
                    credential.setAttributes(new Json(map));
                } catch (JsonProcessingException ignored) {
                    throw new BadRequestException("Could not create credential from source credential!");
                }
            }
        }
        return credential;
    }
}
