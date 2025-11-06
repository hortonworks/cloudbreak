package com.sequenceiq.environment.credential.v1;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;
import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
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
import com.sequenceiq.environment.credential.service.CredentialCreateService;
import com.sequenceiq.environment.credential.service.CredentialDeleteService;
import com.sequenceiq.environment.credential.service.CredentialEntitlementService;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CreateCredentialRequestToCredentialConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.credential.v1.converter.EditCredentialRequestToCredentialConverter;
import com.sequenceiq.notification.WebSocketNotificationController;

@Controller
public class CredentialV1Controller extends WebSocketNotificationController implements CredentialEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialV1Controller.class);

    private final CredentialCreateService credentialCreateService;

    private final CredentialService credentialService;

    private final CreateCredentialRequestToCredentialConverter credentialRequestConverter;

    private final EditCredentialRequestToCredentialConverter credentialEditConverter;

    private final CredentialToCredentialV1ResponseConverter credentialResponseConverter;

    private final CredentialDeleteService credentialDeleteService;

    private final EnvironmentCredentialFiltering environmentCredentialFiltering;

    private final CredentialEntitlementService credentialEntitlementService;

    public CredentialV1Controller(
            CredentialCreateService credentialCreateService,
            CredentialService credentialService,
            CreateCredentialRequestToCredentialConverter credentialRequestConverter,
            EditCredentialRequestToCredentialConverter credentialEditConverter,
            CredentialToCredentialV1ResponseConverter credentialResponseConverter,
            CredentialDeleteService credentialDeleteService,
            EnvironmentCredentialFiltering environmentCredentialFiltering,
            CredentialEntitlementService credentialEntitlementService) {
        this.credentialCreateService = credentialCreateService;
        this.credentialService = credentialService;
        this.credentialRequestConverter = credentialRequestConverter;
        this.credentialResponseConverter = credentialResponseConverter;
        this.credentialEditConverter = credentialEditConverter;
        this.credentialDeleteService = credentialDeleteService;
        this.environmentCredentialFiltering = environmentCredentialFiltering;
        this.credentialEntitlementService = credentialEntitlementService;
    }

    @Override
    @FilterListBasedOnPermissions
    public CredentialResponses list() {
        Set<Credential> credentials = environmentCredentialFiltering.filterCredntials(AuthorizationResourceAction.DESCRIBE_CREDENTIAL);
        return new CredentialResponses(
                credentials
                        .stream()
                        .map(credentialResponseConverter::convert)
                        .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public CredentialResponse getByName(@ResourceName String credentialName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByNameForAccountId(credentialName, accountId, ENVIRONMENT);
        return credentialResponseConverter.convert(credential);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL_ON_ENVIRONMENT)
    public CredentialResponse getByEnvironmentCrn(@ResourceCrn String environmentCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId, ENVIRONMENT);
        return credentialResponseConverter.convert(credential);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL_ON_ENVIRONMENT)
    public CredentialResponse getByEnvironmentName(@ResourceName String environmentName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByEnvironmentNameAndAccountId(environmentName, accountId, ENVIRONMENT);
        return credentialResponseConverter.convert(credential);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public CredentialResponse getByResourceCrn(@ResourceCrn String credentialCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return credentialResponseConverter.convert(credentialService.getByCrnForAccountId(credentialCrn, accountId, ENVIRONMENT));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CREDENTIAL)
    public CredentialResponse create(CredentialRequest createCredentialRequest) {
        LOGGER.debug("Create credential request has received: {}", createCredentialRequest);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        credentialEntitlementService.checkAzureEntitlement(accountId, createCredentialRequest.getAzure());
        Credential createdCredential = credentialCreateService.create(createCredentialRequest, accountId, ThreadBasedUserCrnProvider.getUserCrn(), ENVIRONMENT);
        notify(ResourceEvent.CREDENTIAL_CREATED);
        LOGGER.debug("Credential has been created: {}", createdCredential);
        return credentialResponseConverter.convert(createdCredential);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_CREDENTIAL)
    public CredentialResponse deleteByName(@ResourceName String name) {
        LOGGER.debug("Delete credential request has received: {}", name);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.deleteByName(name, accountId, ENVIRONMENT);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        LOGGER.debug("Credential has been deleted: {}", deleted);
        return credentialResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_CREDENTIAL)
    public CredentialResponse deleteByResourceCrn(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.deleteByCrn(crn, accountId, ENVIRONMENT);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return credentialResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.DELETE_CREDENTIAL)
    public CredentialResponses deleteMultiple(@ResourceNameList Set<String> names) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Set<Credential> credentials = credentialDeleteService.deleteMultiple(names, accountId, ENVIRONMENT);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return new CredentialResponses(credentials.stream().map(credentialResponseConverter::convert).collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByRequestProperty(path = "name", type = NAME, action = EDIT_CREDENTIAL)
    public CredentialResponse modify(@RequestObject EditCredentialRequest editCredentialRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        credentialEntitlementService.checkAzureEntitlement(accountId, editCredentialRequest.getAzure());
        Credential originalCredential = credentialService.getByNameForAccountId(editCredentialRequest.getName(), accountId, ENVIRONMENT);
        Credential credential = credentialEditConverter.convert(editCredentialRequest, originalCredential);
        credential.setType(ENVIRONMENT);
        credential = credentialService.updateByAccountId(credential, ThreadBasedUserCrnProvider.getAccountId(), ENVIRONMENT);
        notify(ResourceEvent.CREDENTIAL_MODIFIED);
        return credentialResponseConverter.convert(credential);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CREDENTIAL)
    public InteractiveCredentialResponse interactiveLogin(CredentialRequest credentialRequest) {
        throw new UnsupportedOperationException("Interactive login is not supported.");
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
        throw new UnsupportedOperationException("Init code grant flow is not supported.");
    }

    @Override
    @CheckPermissionByResourceName(action = EDIT_CREDENTIAL)
    public Response initCodeGrantFlowOnExisting(@ResourceName String name) {
        throw new UnsupportedOperationException("Init code grant flow is not supported.");
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CREDENTIAL)
    public CredentialResponse authorizeCodeGrantFlow(String platform, String code, String state) {
        throw new UnsupportedOperationException("Authorization code grant flow is not supported.");
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public CredentialResponse verifyByName(@ResourceName String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByNameForAccountId(name, accountId, ENVIRONMENT);
        Credential verifiedCredential = credentialService.verify(credential);
        return credentialResponseConverter.convert(verifiedCredential);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public CredentialResponse verifyByCrn(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential credential = credentialService.getByCrnForAccountId(crn, accountId, ENVIRONMENT);
        Credential verifiedCredential = credentialService.verify(credential);
        return credentialResponseConverter.convert(verifiedCredential);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CREDENTIAL)
    public Object getCreateCredentialForCli(CredentialRequest credentialRequest) {
        throw new UnsupportedOperationException("not supported request");
    }
}
