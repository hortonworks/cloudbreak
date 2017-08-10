package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
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
public class CredentialSourceDecorator implements Decorator<Credential> {

    @Inject
    private CredentialService credentialService;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public Credential decorate(Credential credential, Object... data) {
        if (null == data || data.length == 0) {
            return credential;
        } else if (credential == null) {
            CredentialSourceRequest credentialSourceRequest = (CredentialSourceRequest) data[DecorationData.SOURCE_CREDENTIAL.ordinal()];
            IdentityUser identityUser = (IdentityUser) data[DecorationData.IDENTITY_USER.ordinal()];
            if (!Strings.isNullOrEmpty(credentialSourceRequest.getSourceName())) {
                credential = credentialService.get(credentialSourceRequest.getSourceName(), identityUser.getAccount());
            } else {
                credential = credentialService.get(credentialSourceRequest.getSourceId());
            }

            if (credential == null) {
                throw new BadRequestException("Source credential does not exist!");
            } else {
                Map<String, Object> map = credential.getAttributes().getMap();
                for (Map.Entry<String, Object> stringObjectEntry : credentialSourceRequest.getParameters().entrySet()) {
                    map.put(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
                }
                credential.setId(null);
                credential.setName(missingResourceNameGenerator.generateName(APIResourceType.CREDENTIAL));
                try {
                    credential.setAttributes(new Json(map));
                } catch (JsonProcessingException e) {
                    throw new BadRequestException("Could not create credential from source credential!");
                }
            }
        }
        return credential;
    }

    private enum DecorationData {
        SOURCE_CREDENTIAL,
        IDENTITY_USER
    }
}
