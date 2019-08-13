package com.sequenceiq.datalake.service.validation.converter;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.datalake.entity.Credential;

@Component
public class CredentialToCloudCredentialConverter {

    public CloudCredential convert(Credential credential) {
        if (credential == null) {
            return null;
        }
        Map<String, Object> fields = isEmpty(credential.getAttributes()) ? new HashMap<>() : new Json(credential.getAttributes()).getMap();
        return new CloudCredential(credential.getCrn(), credential.getName(), fields);
    }

}
