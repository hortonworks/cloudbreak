package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

@Component
public class CredentialController extends NotificationController implements CredentialEndpoint {
    @Resource
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public CredentialResponse postPrivate(CredentialRequest credentialRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createCredential(user, credentialRequest, false);
    }

    @Override
    public CredentialResponse postPublic(CredentialRequest credentialRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createCredential(user, credentialRequest, true);
    }

    @Override
    public Set<CredentialResponse> getPrivates() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<Credential> credentials = credentialService.retrievePrivateCredentials(user);
        return convertCredentials(credentials);
    }

    @Override
    public Set<CredentialResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<Credential> credentials = credentialService.retrieveAccountCredentials(user);
        return convertCredentials(credentials);
    }

    @Override
    public CredentialResponse getPrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Credential credentials = credentialService.getPrivateCredential(name, user);
        return convert(credentials);
    }

    @Override
    public CredentialResponse getPublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Credential credentials = credentialService.getPublicCredential(name, user);
        return convert(credentials);
    }

    @Override
    public CredentialResponse get(Long id) {
        Credential credential = credentialService.get(id);
        return convert(credential);
    }

    @Override
    public void delete(Long id) {
        executeAndNotify(user -> credentialService.delete(id, user), ResourceEvent.CREDENTIAL_DELETED);
    }

    @Override
    public void deletePublic(String name) {
        executeAndNotify(user -> credentialService.delete(name, user), ResourceEvent.CREDENTIAL_DELETED);
    }

    @Override
    public void deletePrivate(String name) {
        executeAndNotify(user -> credentialService.delete(name, user), ResourceEvent.CREDENTIAL_DELETED);
    }

    @Override
    public Map<String, String> privateInteractiveLogin(CredentialRequest credentialRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return interactiveLogin(user, credentialRequest, false);
    }

    @Override
    public Map<String, String> publicInteractiveLogin(CredentialRequest credentialRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return interactiveLogin(user, credentialRequest, true);
    }

    private Map<String, String> interactiveLogin(IdentityUser user, CredentialRequest credentialRequest, boolean publicInAccount) {
        Credential credential = convert(credentialRequest, publicInAccount);
        return credentialService.interactiveLogin(user, credential);
    }

    private CredentialResponse createCredential(IdentityUser user, CredentialRequest credentialRequest, boolean publicInAccount) {
        Credential credential = convert(credentialRequest, publicInAccount);
        credential = credentialService.create(user, credential);
        notify(user, ResourceEvent.CREDENTIAL_CREATED);
        return convert(credential);
    }

    private Credential convert(CredentialRequest json, boolean publicInAccount) {
        Credential converted = conversionService.convert(json, Credential.class);
        converted.setPublicInAccount(publicInAccount);
        return converted;
    }

    private CredentialResponse convert(Credential credential) {
        return conversionService.convert(credential, CredentialResponse.class);
    }

    private Set<CredentialResponse> convertCredentials(Iterable<Credential> credentials) {
        Set<CredentialResponse> jsonSet = new HashSet<>();
        for (Credential credential : credentials) {
            jsonSet.add(convert(credential));
        }
        return jsonSet;
    }
}
