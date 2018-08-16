package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

@Component
@Transactional(TxType.NEVER)
public class CredentialController extends NotificationController implements CredentialEndpoint {

    @Autowired
    private CredentialService credentialService;

    @Override
    public CredentialResponse postPrivate(CredentialRequest credentialRequest) {
        return postPublic(credentialRequest);
    }

    @Override
    public CredentialResponse postPublic(CredentialRequest credentialRequest) {
        return createCredential(credentialRequest);
    }

    @Override
    public CredentialResponse putPrivate(CredentialRequest credentialRequest) {
        return putPublic(credentialRequest);
    }

    @Override
    public CredentialResponse putPublic(CredentialRequest credentialRequest) {
        return modifyCredential(credentialRequest);
    }

    @Override
    public Set<CredentialResponse> getPrivates() {
        return getPublics();
    }

    @Override
    public Set<CredentialResponse> getPublics() {
        return credentialService.convertAllToResponse(credentialService.listForUsersDefaultOrganization());
    }

    @Override
    public CredentialResponse getPrivate(String name) {
        return getPublic(name);
    }

    @Override
    public CredentialResponse getPublic(String name) {
        return credentialService.convertToResponse(credentialService.getByNameFromUsersDefaultOrganization(name));
    }

    @Override
    public CredentialResponse get(Long id) {
        return credentialService.convertToResponse(credentialService.get(id));
    }

    @Override
    public void delete(Long id) {
        executeAndNotify(user -> credentialService.delete(id), ResourceEvent.CREDENTIAL_DELETED);
    }

    @Override
    public void deletePublic(String name) {
        deletePrivate(name);
    }

    @Override
    public void deletePrivate(String name) {
        executeAndNotify(user -> credentialService.delete(name), ResourceEvent.CREDENTIAL_DELETED);
    }

    @Override
    public Map<String, String> privateInteractiveLogin(CredentialRequest credentialRequest) {
        return publicInteractiveLogin(credentialRequest);
    }

    @Override
    public Map<String, String> publicInteractiveLogin(CredentialRequest credentialRequest) {
        return interactiveLogin(credentialRequest);
    }

    private Map<String, String> interactiveLogin(CredentialRequest credentialRequest) {
        Credential credential = credentialService.convertToCredential(credentialRequest);
        return credentialService.interactiveLogin(credential);
    }

    private CredentialResponse createCredential(CredentialRequest credentialRequest) {
        Credential credential = credentialService.create(credentialService.convertToCredential(credentialRequest));
        return credentialService.convertToResponse(credential);
    }

    private CredentialResponse modifyCredential(CredentialRequest credentialRequest) {
        Credential credential = credentialService.update(credentialService.convertToCredential(credentialRequest));
        return credentialService.convertToResponse(credential);
    }
}