package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.validation.OpenStackCredentialParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;

@Component
public class OpenStackCredentialToJsonConverter extends AbstractConversionServiceAwareConverter<OpenStackCredential, CredentialResponse> {
    @Override
    public CredentialResponse convert(OpenStackCredential source) {
        CredentialResponse credentialJson = new CredentialResponse();
        credentialJson.setId(source.getId());
        credentialJson.setCloudPlatform(CloudPlatform.OPENSTACK);
        credentialJson.setName(source.getName());
        credentialJson.setDescription(source.getDescription());
        credentialJson.setPublicKey(source.getPublicKey());
        credentialJson.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OpenStackCredentialParam.TENANT_NAME.getName(), source.getTenantName());
        parameters.put(OpenStackCredentialParam.ENDPOINT.getName(), source.getEndpoint());
        credentialJson.setParameters(parameters);
        return credentialJson;
    }
}
