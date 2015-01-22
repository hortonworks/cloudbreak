package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.validation.OpenStackCredentialParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;

@Component
public class OpenStackCredentialConverter extends AbstractConverter<CredentialJson, OpenStackCredential> {

    @Autowired
    private StandardPBEStringEncryptor encryptor;

    @Override
    public CredentialJson convert(OpenStackCredential entity) {
        CredentialJson credentialJson = new CredentialJson();
        credentialJson.setId(entity.getId());
        credentialJson.setCloudPlatform(CloudPlatform.OPENSTACK);
        credentialJson.setName(entity.getName());
        credentialJson.setDescription(entity.getDescription());
        credentialJson.setPublicKey(entity.getPublicKey());
        credentialJson.setPublicInAccount(entity.isPublicInAccount());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OpenStackCredentialParam.TENANT_NAME.getName(), entity.getTenantName());
        parameters.put(OpenStackCredentialParam.ENDPOINT.getName(), entity.getEndpoint());
        credentialJson.setParameters(parameters);
        return credentialJson;
    }

    @Override
    public OpenStackCredential convert(CredentialJson json) {
        OpenStackCredential openStackCredential = new OpenStackCredential();
        openStackCredential.setName(json.getName());
        openStackCredential.setDescription(json.getDescription());
        String userName = String.valueOf(json.getParameters().get(OpenStackCredentialParam.USER.getName()));
        openStackCredential.setUserName(encryptor.encrypt(userName));
        String password = String.valueOf(json.getParameters().get(OpenStackCredentialParam.PASSWORD.getName()));
        openStackCredential.setPassword(encryptor.encrypt(password));
        openStackCredential.setTenantName(String.valueOf(json.getParameters().get(OpenStackCredentialParam.TENANT_NAME.getName())));
        openStackCredential.setEndpoint(String.valueOf(json.getParameters().get(OpenStackCredentialParam.ENDPOINT.getName())));
        openStackCredential.setPublicKey(json.getPublicKey());
        openStackCredential.setPublicInAccount(json.isPublicInAccount());
        return openStackCredential;
    }
}
