package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.validation.GccCredentialParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;

@Component
public class GcpCredentialToJsonConverter extends AbstractConversionServiceAwareConverter<GccCredential, CredentialResponse> {

    @Override
    public CredentialResponse convert(GccCredential source) {
        CredentialResponse credentialJson = new CredentialResponse();
        credentialJson.setId(source.getId());
        credentialJson.setCloudPlatform(CloudPlatform.GCC);
        credentialJson.setName(source.getName());
        credentialJson.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> params = new HashMap<>();
        params.put(GccCredentialParam.SERVICE_ACCOUNT_ID.getName(), source.getServiceAccountId());
        params.put(GccCredentialParam.PROJECTID.getName(), source.getProjectId());
        credentialJson.setParameters(params);
        credentialJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        return credentialJson;

    }
}
