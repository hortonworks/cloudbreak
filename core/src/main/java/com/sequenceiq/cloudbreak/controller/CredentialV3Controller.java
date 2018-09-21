package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.CredentialV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.v3.credential.CredentialPrerequisites;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Credential.class)
public class CredentialV3Controller extends NotificationController implements CredentialV3Endpoint {

    @Inject
    private CredentialService credentialService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Set<CredentialResponse> listByWorkspace(Long workspaceId) {
        return credentialService.listAvailablesByWorkspaceId(workspaceId).stream()
                .map(credential -> conversionService.convert(credential, CredentialResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public CredentialResponse getByNameInWorkspace(Long workspaceId, String name) {
        return conversionService.convert(credentialService.getByNameForWorkspaceId(name, workspaceId), CredentialResponse.class);
    }

    @Override
    public CredentialResponse createInWorkspace(Long workspaceId, CredentialRequest request) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Credential credential = credentialService.create(conversionService.convert(request, Credential.class), workspaceId, user);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return conversionService.convert(credential, CredentialResponse.class);
    }

    @Override
    public CredentialResponse deleteInWorkspace(Long workspaceId, String name) {
        Credential deleted = credentialService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return conversionService.convert(deleted, CredentialResponse.class);
    }

    @Override
    public CredentialResponse putInWorkspace(Long workspaceId, CredentialRequest credentialRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        return conversionService.convert(credentialService.updateByWorkspaceId(
                workspaceId, conversionService.convert(credentialRequest, Credential.class), user), CredentialResponse.class);
    }

    @Override
    public Map<String, String> interactiveLogin(Long workspaceId, CredentialRequest credentialRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return credentialService.interactiveLogin(workspaceId, conversionService.convert(credentialRequest, Credential.class), workspace, user);
    }

    @Override
    public CredentialPrerequisites getPrerequisitesForCloudPlatform(Long workspaceId, String platform) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return credentialService.getPrerequisites(user, workspace, platform);
    }
}