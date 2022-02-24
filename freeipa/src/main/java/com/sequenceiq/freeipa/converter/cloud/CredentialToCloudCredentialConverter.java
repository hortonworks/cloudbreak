package com.sequenceiq.freeipa.converter.cloud;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.dto.Credential;

@Component
public class CredentialToCloudCredentialConverter implements Converter<Credential, CloudCredential> {

    public CloudCredential convert(Credential credential) {
        if (credential == null) {
            return null;
        }
        Map<String, Object> fields = isEmpty(credential.getAttributes()) ? new HashMap<>() : new Json(credential.getAttributes()).getMap();
        return new CloudCredential(credential.getCrn(), credential.getName(), fields, credential.getAccountId(), false);
    }

}
