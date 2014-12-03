package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.validation.GccCredentialParam;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.service.credential.gcc.GccCredentialHandler;

@Component
public class GccCredentialConverter extends AbstractConverter<CredentialJson, GccCredential> {

    @Autowired
    private GccCredentialHandler gccCredentialHandler;

    @Override
    public CredentialJson convert(GccCredential entity) {
        CredentialJson credentialJson = new CredentialJson();
        credentialJson.setId(entity.getId());
        credentialJson.setCloudPlatform(CloudPlatform.GCC);
        credentialJson.setName(entity.getName());
        Map<String, Object> params = new HashMap<>();
        params.put(GccCredentialParam.SERVICE_ACCOUNT_ID.getName(), entity.getServiceAccountId());
        params.put(GccCredentialParam.PROJECTID.getName(), entity.getProjectId());
        credentialJson.setParameters(params);
        credentialJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        return credentialJson;
    }

    @Override
    public GccCredential convert(CredentialJson json) {
        GccCredential gccCredential = new GccCredential();
        gccCredential.setName(json.getName());
        gccCredential.setServiceAccountId(String.valueOf(json.getParameters().get(GccCredentialParam.SERVICE_ACCOUNT_ID.getName())));
        gccCredential.setServiceAccountPrivateKey(String.valueOf(json.getParameters().get(GccCredentialParam.SERVICE_ACCOUNT_PRIVATE_KEY.getName())));
        gccCredential.setProjectId(String.valueOf(json.getParameters().get(GccCredentialParam.PROJECTID.getName())));
        gccCredential.setCloudPlatform(CloudPlatform.GCC);
        gccCredential.setDescription(json.getDescription());
        gccCredential.setPublicKey(json.getPublicKey());
        gccCredentialHandler.init(gccCredential);
        return gccCredential;
    }
}
