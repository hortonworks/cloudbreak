package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.CredentialV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.filter.AuthCodeGrantFlowFilter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.InteractiveCredentialV4Response;
import com.sequenceiq.cloudbreak.api.model.v3.credential.CredentialPrerequisitesV4Response;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.common.NotificationController;
import com.sequenceiq.cloudbreak.controller.validation.credential.CredentialValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialPropertyCollector;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Credential.class)
public class CredentialV4Controller extends NotificationController implements CredentialV4Endpoint {

    @Inject
    private CredentialService credentialService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private CredentialValidator credentialValidator;

    @Inject
    private CredentialPropertyCollector credentialPropertyCollector;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public CredentialV4Responses list(Long workspaceId) {
        Set<Credential> credentials = credentialService.listAvailablesByWorkspaceId(workspaceId);
        return new CredentialV4Responses(converterUtil.convertAllAsSet(credentials, CredentialV4Response.class));
    }

    @Override
    public CredentialV4Response get(Long workspaceId, String name) {
        return conversionService.convert(credentialService.getByNameForWorkspaceId(name, workspaceId), CredentialV4Response.class);
    }

    @Override
    public CredentialV4Response post(Long workspaceId, CredentialV4Request request) {
        Credential credential = credentialService.createForLoggedInUser(conversionService.convert(request, Credential.class), workspaceId);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return conversionService.convert(credential, CredentialV4Response.class);
    }

    @Override
    public CredentialV4Response delete(Long workspaceId, String name) {
        Credential deleted = credentialService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return conversionService.convert(deleted, CredentialV4Response.class);
    }

    @Override
    public CredentialV4Response put(Long workspaceId, CredentialV4Request credentialRequest) {
        return conversionService.convert(credentialService.updateByWorkspaceId(
                workspaceId, conversionService.convert(credentialRequest, Credential.class)), CredentialV4Response.class);
    }

    @Override
    public InteractiveCredentialV4Response interactiveLogin(Long workspaceId, CredentialV4Request credentialRequest) {
        Map<String, String> result = credentialService.interactiveLogin(workspaceId, conversionService.convert(credentialRequest, Credential.class));
        return new InteractiveCredentialV4Response(result.get("user_code"), result.get("verification_url"));
    }

    @Override
    public CredentialPrerequisitesV4Response getPrerequisitesForCloudPlatform(Long workspaceId, String platform, String deploymentAddress) {
        return credentialService.getPrerequisites(workspaceId, platform, deploymentAddress);
    }

    @Override
    public Response initCodeGrantFlow(Long workspaceId, CredentialV4Request credentialRequest) {
        String loginURL = credentialService.initCodeGrantFlow(workspaceId, conversionService.convert(credentialRequest, Credential.class));
        return Response.status(Response.Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public Response initCodeGrantFlowOnExisting(Long workspaceId, String name) {
        String loginURL = credentialService.initCodeGrantFlow(workspaceId, name);
        return Response.status(Response.Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public CredentialV4Response authorizeCodeGrantFlow(Long workspaceId, String platform, AuthCodeGrantFlowFilter filter) {
        Credential credential = credentialService.authorizeCodeGrantFlow(filter.getCode(), filter.getState(), workspaceId, platform);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return conversionService.convert(credential, CredentialV4Response.class);
    }
}