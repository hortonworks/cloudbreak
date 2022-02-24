package com.sequenceiq.environment.credential.v1.converter;

import static com.sequenceiq.cloudbreak.cloud.model.CloudCredential.GOV_CLOUD;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.credential.domain.Credential;

@Component
public class CredentialToCloudCredentialConverter {

    public CloudCredential convert(Credential credential) {
        if (credential == null) {
            return null;
        }
        final Map<String, Object> fields;
        if (credential.getId() == null) {
            fields = isEmpty(credential.getAttributes()) ? new HashMap<>() : new Json(credential.getAttributes()).getMap();
        } else {
            fields = new Json(credential.getAttributes()).getMap();
        }
        fields.put(GOV_CLOUD, credential.getGovCloud());
        return new CloudCredential(credential.getResourceCrn(), credential.getName(), fields, credential.getAccountId(), credential.isVerifyPermissions());
    }

}
