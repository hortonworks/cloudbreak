package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Controller
@Transactional(TxType.NEVER)
public class CredentialController extends NotificationController implements CredentialEndpoint {

    @Autowired
    private CredentialService credentialService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public CredentialResponse postPrivate(CredentialRequest credentialRequest) {
        return postPublic(credentialRequest);
    }

    @Override
    public CredentialResponse postPublic(CredentialRequest credentialRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return createCredential(credentialRequest, user, workspace);
    }

    @Override
    public CredentialResponse putPrivate(CredentialRequest credentialRequest) {
        return putPublic(credentialRequest);
    }

    @Override
    public CredentialResponse putPublic(CredentialRequest credentialRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return modifyCredential(credentialRequest, user, workspace);
    }

    @Override
    public Set<CredentialResponse> getPrivates() {
        return getPublics();
    }

    @Override
    public Set<CredentialResponse> getPublics() {
        return credentialService.convertAllToResponse(credentialService.listAvailablesByWorkspaceId(restRequestThreadLocalService.getRequestedWorkspaceId()));
    }

    @Override
    public CredentialResponse getPrivate(String name) {
        return getPublic(name);
    }

    @Override
    public CredentialResponse getPublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return credentialService.convertToResponse(credentialService.getByNameForWorkspace(name, workspace));
    }

    @Override
    public CredentialResponse get(Long id) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return credentialService.convertToResponse(credentialService.get(id, workspace));
    }

    @Override
    public void delete(Long id) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        executeAndNotify(identityUser -> credentialService.delete(id, workspace), ResourceEvent.CREDENTIAL_DELETED);
    }

    @Override
    public void deletePublic(String name) {
        deletePrivate(name);
    }

    @Override
    public void deletePrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        executeAndNotify(identityUser -> credentialService.delete(name, workspace), ResourceEvent.CREDENTIAL_DELETED);
    }

    @Override
    public Map<String, String> privateInteractiveLogin(CredentialRequest credentialRequest) {
        return publicInteractiveLogin(credentialRequest);
    }

    @Override
    public Map<String, String> publicInteractiveLogin(CredentialRequest credentialRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return interactiveLogin(credentialRequest, workspace, user);
    }

    private Map<String, String> interactiveLogin(CredentialRequest credentialRequest, Workspace workspace, User user) {
        Credential credential = credentialService.convertToCredential(credentialRequest);
        return credentialService.interactiveLogin(workspace.getId(), credential, workspace, user);
    }

    private CredentialResponse createCredential(CredentialRequest credentialRequest, User user, Workspace workspace) {
        Credential credential = credentialService.create(credentialService.convertToCredential(credentialRequest), workspace.getId(), user);
        return credentialService.convertToResponse(credential);
    }

    private CredentialResponse modifyCredential(CredentialRequest credentialRequest, User user, Workspace workspace) {
        Credential credential = credentialService.updateByWorkspaceId(workspace.getId(), credentialService.convertToCredential(credentialRequest), user);
        return credentialService.convertToResponse(credential);
    }
}