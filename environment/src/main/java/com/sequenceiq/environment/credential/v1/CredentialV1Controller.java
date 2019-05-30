package com.sequenceiq.environment.credential.v1;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;
import com.sequenceiq.environment.api.v1.credential.model.response.InteractiveCredentialResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialDeleteService;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.notification.NotificationController;
import com.sequenceiq.notification.ResourceEvent;

@Component
public class CredentialV1Controller extends NotificationController implements CredentialEndpoint {

    private final CredentialService credentialService;

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    private final ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    private final CredentialDeleteService credentialDeleteService;

    public CredentialV1Controller(
            CredentialService credentialService,
            CredentialToCredentialV1ResponseConverter credentialConverter,
            ThreadBasedUserCrnProvider threadBasedUserCrnProvider,
            CredentialDeleteService credentialDeleteService) {
        this.credentialService = credentialService;
        this.credentialConverter = credentialConverter;
        this.threadBasedUserCrnProvider = threadBasedUserCrnProvider;
        this.credentialDeleteService = credentialDeleteService;
    }

    @Override
    public CredentialResponses list() {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return new CredentialResponses(
                credentialService.listAvailablesByAccountId(accountId)
                        .stream()
                        .map(credentialConverter::convert)
                        .collect(Collectors.toSet()));
    }

    @Override
    public CredentialResponse get(String credentialName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return credentialConverter.convert(credentialService.getByNameForAccountId(credentialName, accountId));
    }

    @Override
    public CredentialResponse post(@Valid CredentialRequest request) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialConverter.convert(request);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return credentialConverter.convert(credentialService.create(credential, accountId));
    }

    @Override
    public CredentialResponse delete(String name) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.delete(name, accountId);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return credentialConverter.convert(deleted);
    }

    @Override
    public CredentialResponses deleteMultiple(Set<String> names) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Set<Credential> credentials = credentialDeleteService.deleteMultiple(names, accountId);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return new CredentialResponses(credentials.stream().map(credentialConverter::convert).collect(Collectors.toSet()));
    }

    @Override
    public CredentialResponse put(@Valid CredentialRequest credentialRequest) {
        Credential credential = credentialService.updateByAccountId(credentialConverter.convert(credentialRequest), threadBasedUserCrnProvider.getAccountId());
        notify(ResourceEvent.CREDENTIAL_MODIFIED);
        return credentialConverter.convert(credential);
    }

    @Override
    public InteractiveCredentialResponse interactiveLogin(@Valid CredentialRequest credentialRequest) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Map<String, String> result = credentialService.interactiveLogin(accountId, credentialConverter.convert(credentialRequest));
        return new InteractiveCredentialResponse(result.get("user_code"), result.get("verification_url"));
    }

    @Override
    public CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(String platform, String deploymentAddress) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return credentialService.getPrerequisites(accountId, platform, deploymentAddress);
    }

    @Override
    public Response initCodeGrantFlow(CredentialRequest credentialRequest) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String loginURL = credentialService.initCodeGrantFlow(accountId, credentialConverter.convert(credentialRequest));
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public Response initCodeGrantFlowOnExisting(String name) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String loginURL = credentialService.initCodeGrantFlow(accountId, name);
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public CredentialResponse authorizeCodeGrantFlow(String platform, String code, String state) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.authorizeCodeGrantFlow(code, state, accountId, platform);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return credentialConverter.convert(credential);
    }

}
