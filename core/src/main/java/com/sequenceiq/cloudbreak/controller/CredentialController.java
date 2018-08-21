package com.sequenceiq.cloudbreak.controller;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.LegacyCredentialService;

@Component
@Transactional(TxType.NEVER)
public class CredentialController extends NotificationController implements CredentialEndpoint {

    @Autowired
    private LegacyCredentialService credentialService;

    @Resource
    @Qualifier("conversionService")
    private ConversionService conversionService;

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
        return convertAllToResponse(credentialService.listForUsersDefaultOrganization());
    }

    @Override
    public CredentialResponse getPrivate(String name) {
        return getPublic(name);
    }

    @Override
    public CredentialResponse getPublic(String name) {
        return convertToResponse(credentialService.getByNameFromUsersDefaultOrganization(name));
    }

    @Override
    public CredentialResponse get(Long id) {
        return convertToResponse(credentialService.getByIdFromAnyAvailableOrganization(id));
    }

    @Override
    public void delete(Long id) {
        executeAndNotify(user -> credentialService.deleteByIdFromAnyAvailableOrganization(id), ResourceEvent.CREDENTIAL_DELETED);
    }

    @Override
    public void deletePublic(String name) {
        deletePrivate(name);
    }

    @Override
    public void deletePrivate(String name) {
        executeAndNotify(user -> credentialService.deleteByNameFromDefaultOrganization(name), ResourceEvent.CREDENTIAL_DELETED);
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
        Credential credential = convertToCredential(credentialRequest);
        return credentialService.interactiveLogin(credential);
    }

    private CredentialResponse createCredential(CredentialRequest credentialRequest) {
        Credential credential = credentialService.createInDefaultOrganization(convertToCredential(credentialRequest));
        return convertToResponse(credential);
    }

    private CredentialResponse modifyCredential(CredentialRequest credentialRequest) {
        Credential credential = credentialService.update(convertToCredential(credentialRequest));
        return convertToResponse(credential);
    }

    public Set<CredentialResponse> convertAllToResponse(@Nonnull Iterable<Credential> credentials) {
        Set<CredentialResponse> jsonSet = new LinkedHashSet<>();
        for (Credential credential : credentials) {
            jsonSet.add(convertToResponse(credential));
        }
        return jsonSet;
    }

    public CredentialResponse convertToResponse(Credential credential) {
        return conversionService.convert(credential, CredentialResponse.class);
    }

    public Credential convertToCredential(CredentialRequest request) {
        return conversionService.convert(request, Credential.class);
    }
}