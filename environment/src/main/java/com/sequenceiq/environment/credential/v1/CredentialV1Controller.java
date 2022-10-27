package com.sequenceiq.environment.credential.v1;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.api.v1.credential.endpoint.CredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.EditCredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;
import com.sequenceiq.environment.api.v1.credential.model.response.InteractiveCredentialResponse;
import com.sequenceiq.environment.authorization.EnvironmentCredentialFiltering;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialDeleteService;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.notification.NotificationController;

@Controller
public class CredentialV1Controller extends NotificationController implements CredentialEndpoint {

    private final CredentialService credentialService;

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    private final CredentialDeleteService credentialDeleteService;

    private final EnvironmentCredentialFiltering environmentCredentialFiltering;

    public CredentialV1Controller(
            CredentialService credentialService,
            CredentialToCredentialV1ResponseConverter credentialConverter,
            CredentialDeleteService credentialDeleteService,
            EnvironmentCredentialFiltering environmentCredentialFiltering) {
        this.credentialService = credentialService;
        this.credentialConverter = credentialConverter;
        this.credentialDeleteService = credentialDeleteService;
        this.environmentCredentialFiltering = environmentCredentialFiltering;
    }

    @Override
    @FilterListBasedOnPermissions
    public CredentialResponses list() {
        Set<Credential> credentials = environmentCredentialFiltering.filterCredntials(AuthorizationResourceAction.DESCRIBE_CREDENTIAL);
        return new CredentialResponses(
                credentials
                        .stream()
                        .map(credentialConverter::convert)
                        .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public CredentialResponse getByName(@ResourceName String credentialName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByNameForAccountId(credentialName, accountId, ENVIRONMENT);
        return credentialConverter.convert(credential);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL_ON_ENVIRONMENT)
    public CredentialResponse getByEnvironmentCrn(@TenantAwareParam @ResourceCrn String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId, ENVIRONMENT);
        return credentialConverter.convert(credential);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL_ON_ENVIRONMENT)
    public CredentialResponse getByEnvironmentName(@ResourceName String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentNameAndAccountId(environmentName, accountId, ENVIRONMENT);
        return credentialConverter.convert(credential);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public CredentialResponse getByResourceCrn(@TenantAwareParam @ResourceCrn String credentialCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return credentialConverter.convert(credentialService.getByCrnForAccountId(credentialCrn, accountId, ENVIRONMENT));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CREDENTIAL)
    public CredentialResponse post(@Valid CredentialRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();
        Credential credential = credentialConverter.convert(request);
        credential.setType(ENVIRONMENT);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return credentialConverter.convert(credentialService.create(credential, accountId, creator, ENVIRONMENT));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_CREDENTIAL)
    public CredentialResponse deleteByName(@ResourceName String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.deleteByName(name, accountId, ENVIRONMENT);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return credentialConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_CREDENTIAL)
    public CredentialResponse deleteByResourceCrn(@TenantAwareParam @ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.deleteByCrn(crn, accountId, ENVIRONMENT);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return credentialConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_CREDENTIAL)
    public CredentialResponses deleteMultiple(@ResourceNameList Set<String> names) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Set<Credential> credentials = credentialDeleteService.deleteMultiple(names, accountId, ENVIRONMENT);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return new CredentialResponses(credentials.stream().map(credentialConverter::convert).collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByRequestProperty(path = "name", type = NAME, action = EDIT_CREDENTIAL)
    public CredentialResponse put(@RequestObject @Valid EditCredentialRequest credentialRequest) {
        Credential credential = credentialConverter.convert(credentialRequest);
        credential.setType(ENVIRONMENT);
        credential = credentialService.updateByAccountId(credential, ThreadBasedUserCrnProvider.getAccountId(), ENVIRONMENT);
        notify(ResourceEvent.CREDENTIAL_MODIFIED);
        return credentialConverter.convert(credential);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CREDENTIAL)
    public InteractiveCredentialResponse interactiveLogin(@Valid CredentialRequest credentialRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialConverter.convert(credentialRequest);
        credential.setType(ENVIRONMENT);
        Map<String, String> result = credentialService.interactiveLogin(accountId, credential);
        return new InteractiveCredentialResponse(result.get("user_code"), result.get("verification_url"));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CREDENTIAL)
    public CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(String platform, boolean govCloud, String deploymentAddress) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return credentialService.getPrerequisites(platform, govCloud, deploymentAddress, userCrn, ENVIRONMENT);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CREDENTIAL)
    public Response initCodeGrantFlow(CredentialRequest credentialRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Credential credential = credentialConverter.convert(credentialRequest);
        credential.setType(ENVIRONMENT);
        String loginURL = credentialService.initCodeGrantFlow(accountId, credential, userCrn);
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    @CheckPermissionByResourceName(action = EDIT_CREDENTIAL)
    public Response initCodeGrantFlowOnExisting(@ResourceName String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String loginURL = credentialService.initCodeGrantFlow(accountId, name);
        return Response.status(Status.FOUND).header("Referrer-Policy", "origin-when-cross-origin").header("Location", loginURL).build();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CREDENTIAL)
    public CredentialResponse authorizeCodeGrantFlow(String platform, String code, String state) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.authorizeCodeGrantFlow(code, state, accountId, platform);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        return credentialConverter.convert(credential);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public CredentialResponse verifyByName(@ResourceName String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByNameForAccountId(name, accountId, ENVIRONMENT);
        Credential verifiedCredential = credentialService.verify(credential);
        return credentialConverter.convert(verifiedCredential);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public CredentialResponse verifyByCrn(@TenantAwareParam @ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByCrnForAccountId(crn, accountId, ENVIRONMENT);
        Credential verifiedCredential = credentialService.verify(credential);
        return credentialConverter.convert(verifiedCredential);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CREDENTIAL)
    public Object getCreateCredentialForCli(CredentialRequest credentialRequest) {
        throw new UnsupportedOperationException("not supported request");
    }
}
