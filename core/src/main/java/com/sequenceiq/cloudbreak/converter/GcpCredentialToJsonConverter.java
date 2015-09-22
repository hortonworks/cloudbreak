package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import com.sequenceiq.cloudbreak.controller.validation.GcpCredentialParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GcpCredential;

@Component
public class GcpCredentialToJsonConverter extends AbstractConversionServiceAwareConverter<GcpCredential, CredentialResponse> {

    @Override
    public CredentialResponse convert(GcpCredential source) {
        CredentialResponse credentialJson = new CredentialResponse();
        credentialJson.setId(source.getId());
        credentialJson.setCloudPlatform(CloudPlatform.GCP);
        credentialJson.setName(source.getName());
        credentialJson.setPublicInAccount(source.isPublicInAccount());
        credentialJson.setPublicKey(source.getPublicKey());
        Map<String, Object> params = new HashMap<>();
        params.put(GcpCredentialParam.SERVICE_ACCOUNT_ID.getName(), source.getServiceAccountId());
        params.put(GcpCredentialParam.PROJECTID.getName(), source.getProjectId());
        params.put(GcpCredentialParam.SERVICE_ACCOUNT_PRIVATE_KEY.getName(), source.getServiceAccountPrivateKey());
        credentialJson.setParameters(params);
        credentialJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        credentialJson.setLoginUserName(source.getLoginUserName());
        return credentialJson;

    }
}
