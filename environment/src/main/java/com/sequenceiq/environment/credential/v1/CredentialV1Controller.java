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
    public CredentialResponse getByName(String credentialName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByNameForAccountId(credentialName, accountId);
        return credentialConverter.convert(credential);
    }

    @Override
    public CredentialResponse getByEnvironmentCrn(String environmentCrn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        return credentialConverter.convert(credential);
    }

    @Override
    public CredentialResponse getByEnvironmentName(String environmentName) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentNameAndAccountId(environmentName, accountId);
        return credentialConverter.convert(credential);
    }

    @Override
    public CredentialResponse getByResourceCrn(String credentialCrn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        return credentialConverter.convert(credentialService.getByCrnForAccountId(credentialCrn, accountId));
    }

    @Override
    public CredentialResponse post(@Valid CredentialRequest request) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String creator = threadBasedUserCrnProvider.getUserCrn();
        Credential credential = credentialConverter.convert(request);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return credentialConverter.convert(credentialService.create(credential, accountId, creator));
    }

    @Override
    public CredentialResponse deleteByName(String name) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.deleteByName(name, accountId);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return credentialConverter.convert(deleted);
    }

    @Override
    public CredentialResponse deleteByResourceCrn(String crn) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.deleteByCrn(crn, accountId);
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
        Credential credential = credentialConverter.convert(credentialRequest);
        credential = credentialService.updateByAccountId(credential, threadBasedUserCrnProvider.getAccountId());
        notify(ResourceEvent.CREDENTIAL_MODIFIED);
        return credentialConverter.convert(credential);
    }

    @Override
    public InteractiveCredentialResponse interactiveLogin(@Valid CredentialRequest credentialRequest) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        Map<String, String> result = credentialService.interactiveLogin(accountId, userCrn, credentialConverter.convert(credentialRequest));
        return new InteractiveCredentialResponse(result.get("user_code"), result.get("verification_url"));
    }

    @Override
    public CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(String platform, String deploymentAddress) {
        return credentialService.getPrerequisites(platform, deploymentAddress);
    }

    @Override
    public Response initCodeGrantFlow(CredentialRequest credentialRequest) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        String loginURL = credentialService.initCodeGrantFlow(accountId, credentialConverter.convert(credentialRequest), userCrn);
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public Response initCodeGrantFlowOnExisting(String name) {
        String accountId = threadBasedUserCrnProvider.getAccountId();
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        String loginURL = credentialService.initCodeGrantFlow(accountId, name, userCrn);
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
