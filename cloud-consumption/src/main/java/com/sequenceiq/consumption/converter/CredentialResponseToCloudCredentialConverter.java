package com.sequenceiq.consumption.converter;

import javax.inject.Inject;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialSettings;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

@Component
public class CredentialResponseToCloudCredentialConverter implements Converter<CredentialResponse, CloudCredential> {

    @Inject
    private SecretService secretService;

    public CloudCredential convert(CredentialResponse credentialResponse) {
        if (credentialResponse == null) {
            return null;
        }
        String attributes = secretService.getByResponse(credentialResponse.getAttributes());
        return new CloudCredential(credentialResponse.getCrn(), credentialResponse.getName(), new Json(attributes).getMap(), credentialResponse.getAccountId(),
                new CloudCredentialSettings(credentialResponse.isVerifyPermissions(), credentialResponse.isSkipOrgPolicyDecisions()));
    }
}
