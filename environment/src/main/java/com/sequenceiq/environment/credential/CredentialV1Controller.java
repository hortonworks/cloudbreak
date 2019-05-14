package com.sequenceiq.environment.credential;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.workspace.service.WorkspaceService;
import com.sequenceiq.environment.api.credential.endpoint.CredentialV1Endpoint;
import com.sequenceiq.environment.api.credential.model.request.CredentialV1Request;
import com.sequenceiq.environment.api.credential.model.response.CredentialV1Response;
import com.sequenceiq.environment.api.credential.model.response.CredentialV1Responses;
import com.sequenceiq.environment.api.credential.model.response.InteractiveCredentialV1Response;
import com.sequenceiq.environment.credential.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.credential.converter.CredentialV1RequestToCredentialConverter;
import com.sequenceiq.notification.NotificationController;
import com.sequenceiq.notification.ResourceEvent;

@Component
public class CredentialV1Controller extends NotificationController implements CredentialV1Endpoint {

    private CredentialService credentialService;

    private WorkspaceService workspaceService;

    private CredentialToCredentialV1ResponseConverter credentialToCredentialV1ResponseConverter;

    private CredentialV1RequestToCredentialConverter credentialV1RequestToCredentialConverter;

    @Override
    public CredentialV1Responses list() {
        return new CredentialV1Responses(
                credentialService.listAvailablesByWorkspaceId(workspaceService.getDefaultWorkspaceId())
                        .stream()
                        .map(c -> credentialToCredentialV1ResponseConverter.convert(c))
                        .collect(Collectors.toSet()));
    }

    @Override
    public CredentialV1Response get(String name) {
        return credentialToCredentialV1ResponseConverter.convert(credentialService.getByNameForWorkspaceId(name, workspaceService.getDefaultWorkspaceId()));
    }

    @Override
    public CredentialV1Response post(@Valid CredentialV1Request request) {
        Credential credential = credentialV1RequestToCredentialConverter.convert(request);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return credentialToCredentialV1ResponseConverter
                .convert(credentialService.createForLoggedInUser(credential, workspaceService.getDefaultWorkspaceId()));
    }

    @Override
    public CredentialV1Response delete(String name) {
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return credentialToCredentialV1ResponseConverter.convert(credentialService.deleteByNameFromWorkspace(name, workspaceService.getDefaultWorkspaceId()));
    }

    @Override
    public CredentialV1Responses deleteMultiple(Set<String> names) {
        Set<Credential> credentials = credentialService.deleteMultipleByNameFromWorkspace(names, workspaceService.getDefaultWorkspaceId());
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return new CredentialV1Responses(credentials
                .stream()
                .map(c -> credentialToCredentialV1ResponseConverter.convert(c))
                .collect(Collectors.toSet()));
    }

    @Override
    public CredentialV1Response put(@Valid CredentialV1Request credentialRequest) {
        Credential credential = credentialV1RequestToCredentialConverter.convert(credentialRequest);
        notify(ResourceEvent.CREDENTIAL_MODIFIED);
        return credentialToCredentialV1ResponseConverter.convert(credentialService.updateByWorkspaceId(workspaceService.getDefaultWorkspaceId(), credential));
    }

    @Override
    public InteractiveCredentialV1Response interactiveLogin(@Valid CredentialV1Request credentialRequest) {
        Map<String, String> result = credentialService.interactiveLogin(workspaceService.getDefaultWorkspaceId(),
                credentialV1RequestToCredentialConverter.convert(credentialRequest));
        return new InteractiveCredentialV1Response(result.get("user_code"), result.get("verification_url"));
    }

    @Override
    public CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(String platform, String deploymentAddress) {
        return credentialService.getPrerequisites(workspaceService.getDefaultWorkspaceId(), platform, deploymentAddress);
    }

    @Override
    public Response initCodeGrantFlow(CredentialV1Request credentialRequest) {
        String loginURL = credentialService.initCodeGrantFlow(workspaceService.getDefaultWorkspaceId(),
                credentialV1RequestToCredentialConverter.convert(credentialRequest));
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public Response initCodeGrantFlowOnExisting(String name) {
        String loginURL = credentialService.initCodeGrantFlow(workspaceService.getDefaultWorkspaceId(), name);
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public CredentialV1Response authorizeCodeGrantFlow(String platform, String code, String state) {
        Credential credential = credentialService.authorizeCodeGrantFlow(code, state, workspaceService.getDefaultWorkspaceId(), platform);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return credentialToCredentialV1ResponseConverter.convert(credential);
    }
}
