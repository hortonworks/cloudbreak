package com.sequenceiq.environment.credential.v1;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;
import com.sequenceiq.environment.api.v1.credential.model.response.EmptyResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.InteractiveCredentialResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialDeleteService;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.notification.NotificationController;

@Component
@InternalReady
public class CredentialV1Controller extends NotificationController implements CredentialEndpoint {

    private final CredentialService credentialService;

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    private final CredentialDeleteService credentialDeleteService;

    public CredentialV1Controller(
            CredentialService credentialService,
            CredentialToCredentialV1ResponseConverter credentialConverter,
            CredentialDeleteService credentialDeleteService) {
        this.credentialService = credentialService;
        this.credentialConverter = credentialConverter;
        this.credentialDeleteService = credentialDeleteService;
    }

    @Override
    public CredentialResponses list() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return new CredentialResponses(
                credentialService.listAvailablesByAccountId(accountId)
                        .stream()
                        .map(credentialConverter::convert)
                        .collect(Collectors.toSet()));
    }

    @Override
    public CredentialResponse getByName(String credentialName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByNameForAccountId(credentialName, accountId);
        return credentialConverter.convert(credential);
    }

    @Override
    public CredentialResponse getByEnvironmentCrn(@ResourceCrn String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        return credentialConverter.convert(credential);
    }

    @Override
    public CredentialResponse getByEnvironmentName(String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentNameAndAccountId(environmentName, accountId);
        return credentialConverter.convert(credential);
    }

    @Override
    public CredentialResponse getByResourceCrn(String credentialCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return credentialConverter.convert(credentialService.getByCrnForAccountId(credentialCrn, accountId));
    }

    @Override
    public CredentialResponse post(@Valid CredentialRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        Credential credential = credentialConverter.convert(request);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return credentialConverter.convert(credentialService.create(credential, accountId, creator));
    }

    @Override
    public CredentialResponse deleteByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.deleteByName(name, accountId);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return credentialConverter.convert(deleted);
    }

    @Override
    public CredentialResponse deleteByResourceCrn(String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.deleteByCrn(crn, accountId);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return credentialConverter.convert(deleted);
    }

    @Override
    public CredentialResponses deleteMultiple(Set<String> names) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Set<Credential> credentials = credentialDeleteService.deleteMultiple(names, accountId);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return new CredentialResponses(credentials.stream().map(credentialConverter::convert).collect(Collectors.toSet()));
    }

    @Override
    public CredentialResponse put(@Valid CredentialRequest credentialRequest) {
        Credential credential = credentialConverter.convert(credentialRequest);
        credential = credentialService.updateByAccountId(credential, ThreadBasedUserCrnProvider.getAccountId());
        notify(ResourceEvent.CREDENTIAL_MODIFIED);
        return credentialConverter.convert(credential);
    }

    @Override
    public InteractiveCredentialResponse interactiveLogin(@Valid CredentialRequest credentialRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Map<String, String> result = credentialService.interactiveLogin(accountId, userCrn, credentialConverter.convert(credentialRequest));
        return new InteractiveCredentialResponse(result.get("user_code"), result.get("verification_url"));
    }

    @Override
    public CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(String platform, String deploymentAddress) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return credentialService.getPrerequisites(platform, deploymentAddress, userCrn);
    }

    @Override
    public Response initCodeGrantFlow(CredentialRequest credentialRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String loginURL = credentialService.initCodeGrantFlow(accountId, credentialConverter.convert(credentialRequest), userCrn);
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public Response initCodeGrantFlowOnExisting(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String loginURL = credentialService.initCodeGrantFlow(accountId, name, userCrn);
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    public CredentialResponse authorizeCodeGrantFlow(String platform, String code, String state) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.authorizeCodeGrantFlow(code, state, accountId, platform);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return credentialConverter.convert(credential);
    }

    @Override
    public CredentialResponse verifyByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByNameForAccountId(name, accountId);
        Credential verifiedCredential = credentialService.verify(credential);
        return credentialConverter.convert(verifiedCredential);
    }

    @Override
    public CredentialResponse verifyByCrn(String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByCrnForAccountId(crn, accountId);
        Credential verifiedCredential = credentialService.verify(credential);
        return credentialConverter.convert(verifiedCredential);
    }

    @Override
    public Object getCreateCredentialForCli(CredentialRequest credentialRequest) {
        if (!CloudPlatform.AWS.equalsIgnoreCase(credentialRequest.getCloudPlatform())) {
            return new EmptyResponse();
        }
        return credentialService.getCreateAWSCredentialForCli(credentialRequest);
    }
}
