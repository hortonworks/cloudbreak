package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.CredentialV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialPrerequisitesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.InteractiveCredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Credential.class)
public class CredentialV4Controller extends NotificationController implements CredentialV4Endpoint {

    @Inject
    private CredentialService credentialService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public CredentialV4Responses list(Long workspaceId) {
        Set<Credential> credentials = credentialService.listAvailablesByWorkspaceId(workspaceId);
        return new CredentialV4Responses(converterUtil.convertAllAsSet(credentials, CredentialV4Response.class));
    }

    @Override
    public CredentialV4Response get(Long workspaceId, String name) {
        return converterUtil.convert(credentialService.getByNameForWorkspaceId(name, workspaceId), CredentialV4Response.class);
    }

    @Override
    public CredentialV4Response post(Long workspaceId, CredentialV4Request request) {
        Credential credential = credentialService.createForLoggedInUser(converterUtil.convert(request, Credential.class), workspaceId);
        CredentialV4Response response = converterUtil.convert(credential, CredentialV4Response.class);
        notify(response, NotificationEventType.CREATE_SUCCESS, WorkspaceResource.CREDENTIAL, workspaceId);
        return response;
    }

    @Override
    public CredentialV4Response delete(Long workspaceId, String name) {
        Credential deleted = credentialService.deleteByNameFromWorkspace(name, workspaceId);
        CredentialV4Response response = converterUtil.convert(deleted, CredentialV4Response.class);
        notify(response, NotificationEventType.DELETE_SUCCESS, WorkspaceResource.CREDENTIAL, workspaceId);
        return response;
    }

    @Override
    public CredentialV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
        Set<Credential> deleted = credentialService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        CredentialV4Responses response = new CredentialV4Responses(converterUtil.convertAllAsSet(deleted, CredentialV4Response.class));
        notify(response, NotificationEventType.DELETE_SUCCESS, WorkspaceResource.CREDENTIAL, workspaceId);
        return response;
    }

    @Override
    public CredentialV4Response put(Long workspaceId, CredentialV4Request credentialRequest) {
        return converterUtil.convert(credentialService.updateByWorkspaceId(
                workspaceId, converterUtil.convert(credentialRequest, Credential.class)), CredentialV4Response.class);
    }

    @Override
    public InteractiveCredentialV4Response interactiveLogin(Long workspaceId, CredentialV4Request credentialRequest) {
        Map<String, String> result = credentialService.interactiveLogin(workspaceId, converterUtil.convert(credentialRequest, Credential.class));
        return new InteractiveCredentialV4Response(result.get("user_code"), result.get("verification_url"));
    }

    @Override
    public CredentialPrerequisitesV4Response getPrerequisitesForCloudPlatform(Long workspaceId, String platform, String deploymentAddress) {
        return credentialService.getPrerequisites(workspaceId, platform, deploymentAddress);
    }

    @Override
    public Response initCodeGrantFlow(Long workspaceId, CredentialV4Request credentialRequest) {
        String loginURL = credentialService.initCodeGrantFlow(workspaceId, converterUtil.convert(credentialRequest, Credential.class));
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public Response initCodeGrantFlowOnExisting(Long workspaceId, String name) {
        String loginURL = credentialService.initCodeGrantFlow(workspaceId, name);
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public CredentialV4Response authorizeCodeGrantFlow(Long workspaceId, String platform, String code, String state) {
        Credential credential = credentialService.authorizeCodeGrantFlow(code, state, workspaceId, platform);
        CredentialV4Response response = converterUtil.convert(credential, CredentialV4Response.class);
        notify(response, NotificationEventType.CREATE_SUCCESS, WorkspaceResource.CREDENTIAL, workspaceId);
        return response;
    }
}
