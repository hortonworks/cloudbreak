package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Date;
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
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.CredentialRepository;

@Component
public class CredentialSourceDecorator implements Decorator<Credential> {

    @Inject
    private CredentialRepository credentialRepository;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    private enum DecorationData {
        SOURCE_CREDENTIAL,
        IDENTITY_USER
    }

    public Credential decorate(Credential credential, Object... data) {
        if (null == data || data.length == 0) {
            return credential;
        } else if (credential == null) {
            CredentialSourceRequest credentialSourceRequest = (CredentialSourceRequest) data[DecorationData.SOURCE_CREDENTIAL.ordinal()];
            IdentityUser identityUser = (IdentityUser) data[DecorationData.IDENTITY_USER.ordinal()];
            if (!Strings.isNullOrEmpty(credentialSourceRequest.getSourceName())) {
                credential = credentialRepository.findOneByName(credentialSourceRequest.getSourceName(), identityUser.getAccount());
            } else {
                credential = credentialRepository.findByIdInAccount(credentialSourceRequest.getSourceId(), identityUser.getAccount());
            }

            if (credential == null) {
                throw new BadRequestException("Source credential does not exist!");
            } else {
                Map<String, Object> map = credential.getAttributes().getMap();
                for (Map.Entry<String, Object> stringObjectEntry : credentialSourceRequest.getParameters().entrySet()) {
                    map.put(stringObjectEntry.getKey(), stringObjectEntry.getValue().toString());
                }
                credential.setId(null);
                credential.setName(String.format("%s%s", "c", new Date().getTime()));
                try {
                    credential.setAttributes(new Json(map));
                } catch (JsonProcessingException e) {
                    throw new BadRequestException("Could not create credential from source credential!");
                }
            }
        }
        return credential;
    }
}
