package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.validation.OpenStackCredentialParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;

@Component
public class OpenStackCredentialToJsonConverter extends AbstractConversionServiceAwareConverter<OpenStackCredential, CredentialJson> {
    @Override
    public CredentialJson convert(OpenStackCredential source) {
        CredentialJson credentialJson = new CredentialJson();
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
