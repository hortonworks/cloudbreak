package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.response.CredentialViewResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.domain.CredentialView;

@Component
public class CredentialViewConverter {

    public CredentialViewResponse convert(CredentialView credentialView) {
        CredentialViewResponse response = new CredentialViewResponse();
        response.setName(credentialView.getName());
        response.setCloudPlatform(credentialView.getCloudPlatform());
        response.setCreator(credentialView.getCreator());
        response.setCrn(credentialView.getResourceCrn());
        response.setDescription(credentialView.getDescription());
        response.setVerificationStatusText(credentialView.getVerificationStatusText());
        response.setType(credentialView.getType());
        response.setGovCloud(credentialView.getGovCloud());
        return response;
    }

    public CredentialView convert(Credential credential) {
        CredentialView credentialView = new CredentialView();
        credentialView.setAccountId(credential.getAccountId());
        credentialView.setArchived(credential.isArchived());
        credentialView.setCloudPlatform(credential.getCloudPlatform());
        credentialView.setCreator(credential.getCreator());
        credentialView.setDescription(credential.getDescription());
        credentialView.setGovCloud(credential.getGovCloud());
        credentialView.setName(credential.getName());
        credentialView.setResourceCrn(credential.getResourceCrn());
        credentialView.setVerificationStatusText(credential.getVerificationStatusText());
        credentialView.setType(credential.getType());
        return credentialView;
    }
}
