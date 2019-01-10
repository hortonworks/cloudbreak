package com.sequenceiq.cloudbreak.controller;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.CredentialV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.v3.credential.CredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.common.NotificationController;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
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
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private CredentialValidator credentialValidator;

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
        credentialValidator.validateCredentialCloudPlatform(request.getCloudPlatform());
        credentialValidator.validateParameters(Platform.platform(request.getCloudPlatform()), request.getParameters());
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
    public CredentialPrerequisites getPrerequisitesForCloudPlatform(Long workspaceId, String platform, String deploymentAddress) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        return credentialService.getPrerequisites(user, workspace, platform, deploymentAddress);
    }

    @Override
    public Response initCodeGrantFlow(Long workspaceId, CredentialRequest credentialRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        String loginURL = credentialService.initCodeGrantFlow(workspaceId, conversionService.convert(credentialRequest, Credential.class), user);
        return Response.status(Response.Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public Response initCodeGrantFlowOnExisting(Long workspaceId, String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        String loginURL = credentialService.initCodeGrantFlow(workspaceId, name, user);
        return Response.status(Response.Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public CredentialResponse authorizeCodeGrantFlow(Long workspaceId, String platform, String code, String state) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Credential credential = credentialService.authorizeCodeGrantFlow(code, state, workspaceId, user, platform);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return conversionService.convert(credential, CredentialResponse.class);
    }
}