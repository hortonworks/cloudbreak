package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.environments.model.CreateAWSCredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;

@Component
public class CredentialRequestToCreateAWSCredentialRequestConverter {

    public CreateAWSCredentialRequest convert(CredentialRequest source) {
        CreateAWSCredentialRequest credentialRequest = new CreateAWSCredentialRequest();
        credentialRequest.setCredentialName(source.getName());
        credentialRequest.setDescription(source.getDescription());
        credentialRequest.setRoleArn(source.getAws().getRoleBased().getRoleArn());
        return credentialRequest;
    }
}
