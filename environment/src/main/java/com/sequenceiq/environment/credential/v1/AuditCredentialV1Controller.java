package com.sequenceiq.environment.credential.v1;

import static com.sequenceiq.common.model.CredentialType.AUDIT;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.api.v1.credential.endpoint.AuditCredentialEndpoint;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.EditCredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponses;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialDeleteService;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CreateCredentialRequestToCredentialConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.credential.v1.converter.EditCredentialRequestToCredentialConverter;
import com.sequenceiq.notification.NotificationController;

@Controller
public class AuditCredentialV1Controller extends NotificationController implements AuditCredentialEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditCredentialV1Controller.class);

    private final CredentialService credentialService;

    private final CreateCredentialRequestToCredentialConverter credentialRequestConverter;

    private final EditCredentialRequestToCredentialConverter credentialEditConverter;

    private final CredentialToCredentialV1ResponseConverter credentialResponseConverter;

    private final CredentialDeleteService credentialDeleteService;

    public AuditCredentialV1Controller(
            CredentialService credentialService,
            CreateCredentialRequestToCredentialConverter credentialRequestConverter,
            EditCredentialRequestToCredentialConverter credentialEditConverter,
            CredentialToCredentialV1ResponseConverter credentialResponseConverter,
            CredentialDeleteService credentialDeleteService) {
        this.credentialService = credentialService;
        this.credentialRequestConverter = credentialRequestConverter;
        this.credentialEditConverter = credentialEditConverter;
        this.credentialResponseConverter = credentialResponseConverter;
        this.credentialDeleteService = credentialDeleteService;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_AUDIT_CREDENTIAL)
    public CredentialResponses list() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return new CredentialResponses(
                credentialService.listAvailablesByAccountId(accountId, AUDIT)
                        .stream()
                        .map(credentialResponseConverter::convert)
                        .collect(Collectors.toSet()));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_AUDIT_CREDENTIAL)
    public CredentialResponse getByResourceCrn(@ResourceCrn String credentialCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return credentialResponseConverter.convert(credentialService.getByCrnForAccountId(credentialCrn, accountId, AUDIT));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_AUDIT_CREDENTIAL)
    public CredentialResponse getByResourceName(String credentialName, @AccountId String accountId) {
        String userAccountId = ThreadBasedUserCrnProvider.getAccountId();
        return credentialResponseConverter.convert(credentialService.getByNameForAccountId(credentialName, userAccountId, AUDIT));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_AUDIT_CREDENTIAL)
    public CredentialResponse post(CredentialRequest createCredentialRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Set<Credential> auditCredentialsByPlatform = credentialService
                .listAvailablesByAccountId(accountId, AUDIT)
                .stream()
                .filter(c -> c.getCloudPlatform().equals(createCredentialRequest.getCloudPlatform()))
                .collect(Collectors.toSet());
        if (auditCredentialsByPlatform.isEmpty()) {
            Credential createdCredential = credentialService.create(createCredentialRequest, accountId, ThreadBasedUserCrnProvider.getUserCrn(), AUDIT);
            notify(ResourceEvent.CREDENTIAL_CREATED);
            LOGGER.debug("Credential has been created: {}", createdCredential);
            return credentialResponseConverter.convert(createdCredential);
        } else {
            throw new BadRequestException(String.format("Audit credential already exist for %s cloud.", createCredentialRequest.getCloudPlatform()));
        }
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.MODIFY_AUDIT_CREDENTIAL)
    public CredentialResponse put(EditCredentialRequest credentialRequest) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential originalCredential = credentialService.getByNameForAccountId(credentialRequest.getName(), accountId, AUDIT);
        Credential credential = credentialEditConverter.convert(credentialRequest, originalCredential);
        credential.setType(AUDIT);
        credential = credentialService.updateByAccountId(credential, ThreadBasedUserCrnProvider.getAccountId(), AUDIT);
        notify(ResourceEvent.CREDENTIAL_MODIFIED);
        return credentialResponseConverter.convert(credential);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_AUDIT_CREDENTIAL)
    public CredentialPrerequisitesResponse getPrerequisitesForCloudPlatform(String platform, boolean govCloud, String deploymentAddress) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return credentialService.getPrerequisites(platform, govCloud, deploymentAddress, userCrn, AUDIT);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_AUDIT_CREDENTIAL)
    public CredentialResponse deleteByName(String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.deleteByName(name, accountId, AUDIT);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return credentialResponseConverter.convert(deleted);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_AUDIT_CREDENTIAL)
    public CredentialResponse deleteByResourceCrn(String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        Credential deleted = credentialDeleteService.deleteByCrn(crn, accountId, AUDIT);
        notify(ResourceEvent.CREDENTIAL_DELETED);
        return credentialResponseConverter.convert(deleted);
    }
}
