package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import com.sequenceiq.cloudbreak.controller.validation.OpenStackCredentialParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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
        parameters.put(OpenStackCredentialParam.KEYSTONE_VERSION.getName(), source.getKeystoneVersion());
        parameters.put(OpenStackCredentialParam.KEYSTONE_AUTH_SCOPE.getName(), source.getKeystoneAuthScope());
        parameters.put(OpenStackCredentialParam.TENANT_NAME.getName(), source.getTenantName());
        parameters.put(OpenStackCredentialParam.USER_DOMAIN.getName(), source.getUserDomain());
        parameters.put(OpenStackCredentialParam.PROJECT_NAME.getName(), source.getProjectName());
        parameters.put(OpenStackCredentialParam.PROJECT_DOMAIN_NAME.getName(), source.getProjectDomainName());
        parameters.put(OpenStackCredentialParam.DOMAIN_NAME.getName(), source.getDomainName());
        parameters.put(OpenStackCredentialParam.ENDPOINT.getName(), source.getEndpoint());
        credentialJson.setParameters(parameters);
        credentialJson.setLoginUserName(source.getLoginUserName());
        return credentialJson;
    }
}
