package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(TxType.NEVER)
public class CredentialController extends NotificationController implements CredentialEndpoint {

    @Autowired
    private CredentialService credentialService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public CredentialResponse postPrivate(CredentialRequest credentialRequest) {
        return postPublic(credentialRequest);
    }

    @Override
    public CredentialResponse postPublic(CredentialRequest credentialRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return createCredential(credentialRequest, user, organization);
    }

    @Override
    public CredentialResponse putPrivate(CredentialRequest credentialRequest) {
        return putPublic(credentialRequest);
    }

    @Override
    public CredentialResponse putPublic(CredentialRequest credentialRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return modifyCredential(credentialRequest, user, organization);
    }

    @Override
    public Set<CredentialResponse> getPrivates() {
        return getPublics();
    }

    @Override
    public Set<CredentialResponse> getPublics() {
        return credentialService.convertAllToResponse(credentialService.listAvailablesByOrganizationId(restRequestThreadLocalService.getRequestedOrgId()));
    }

    @Override
    public CredentialResponse getPrivate(String name) {
        return getPublic(name);
    }

    @Override
    public CredentialResponse getPublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return credentialService.convertToResponse(credentialService.getByNameForOrganization(name, organization));
    }

    @Override
    public CredentialResponse get(Long id) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return credentialService.convertToResponse(credentialService.get(id, organization));
    }

    @Override
    public void delete(Long id) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        executeAndNotify(identityUser -> credentialService.delete(id, organization), ResourceEvent.CREDENTIAL_DELETED);
    }

    @Override
    public void deletePublic(String name) {
        deletePrivate(name);
    }

    @Override
    public void deletePrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        executeAndNotify(identityUser -> credentialService.delete(name, organization), ResourceEvent.CREDENTIAL_DELETED);
    }

    @Override
    public Map<String, String> privateInteractiveLogin(CredentialRequest credentialRequest) {
        return publicInteractiveLogin(credentialRequest);
    }

    @Override
    public Map<String, String> publicInteractiveLogin(CredentialRequest credentialRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return interactiveLogin(credentialRequest, organization, user);
    }

    private Map<String, String> interactiveLogin(CredentialRequest credentialRequest, Organization organization, User user) {
        Credential credential = credentialService.convertToCredential(credentialRequest);
        return credentialService.interactiveLogin(organization.getId(), credential, organization, user);
    }

    private CredentialResponse createCredential(CredentialRequest credentialRequest, User user, Organization organization) {
        Credential credential = credentialService.create(credentialService.convertToCredential(credentialRequest), organization.getId(), user);
        return credentialService.convertToResponse(credential);
    }

    private CredentialResponse modifyCredential(CredentialRequest credentialRequest, User user, Organization organization) {
        Credential credential = credentialService.updateByOrganizationId(organization.getId(), credentialService.convertToCredential(credentialRequest), user);
        return credentialService.convertToResponse(credential);
    }
}