package com.sequenceiq.environment.encryptionprofile.v1.controller;

import java.util.Collections;

import jakarta.ws.rs.ForbiddenException;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponses;
import com.sequenceiq.environment.authorization.EncryptionProfileFiltering;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileRequestToEncryptionProfileConverter;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileToEncryptionProfileResponseConverter;
import com.sequenceiq.notification.NotificationController;

@Controller
public class EncryptionProfileController extends NotificationController implements EncryptionProfileEndpoint {

    private final EncryptionProfileService encryptionProfileService;

    private final EncryptionProfileRequestToEncryptionProfileConverter encryptionProfileConverter;

    private final EncryptionProfileToEncryptionProfileResponseConverter encryptionProfileResponseConverter;

    private final EncryptionProfileFiltering encryptionProfileFiltering;

    private final EntitlementService entitlementService;

    public EncryptionProfileController(EncryptionProfileService encryptionProfileService,
            EncryptionProfileRequestToEncryptionProfileConverter encryptionProfileConverter,
            EncryptionProfileToEncryptionProfileResponseConverter encryptionProfileResponseConverter,
            EncryptionProfileFiltering encryptionProfileFiltering,
            EntitlementService entitlementService) {
        this.encryptionProfileService = encryptionProfileService;
        this.encryptionProfileConverter = encryptionProfileConverter;
        this.encryptionProfileResponseConverter = encryptionProfileResponseConverter;
        this.encryptionProfileFiltering = encryptionProfileFiltering;
        this.entitlementService = entitlementService;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENCRYPTION_PROFILE)
    public EncryptionProfileResponse create(EncryptionProfileRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();

        verifyEncryptionProfileEntitlement(accountId);

        EncryptionProfile createdEncryptionProfile = encryptionProfileService
                .create(encryptionProfileConverter.convert(request), accountId, creator);
        notify(ResourceEvent.ENCRYPTION_PROFILE_CREATED);

        return encryptionProfileResponseConverter.convert(createdEncryptionProfile);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE)
    public EncryptionProfileResponse getByName(@ResourceName String encryptionProfileName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        verifyEncryptionProfileEntitlement(accountId);

        EncryptionProfile encryptionProfile = encryptionProfileService.getByNameAndAccountId(
                encryptionProfileName, accountId);
        return encryptionProfileResponseConverter.convert(encryptionProfile);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE)
    public EncryptionProfileResponse getByCrn(@ResourceCrn String encryptionProfileCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        verifyEncryptionProfileEntitlement(accountId);

        EncryptionProfile encryptionProfile = encryptionProfileService.getByCrn(encryptionProfileCrn);
        return encryptionProfileResponseConverter.convert(encryptionProfile);
    }

    @Override
    @FilterListBasedOnPermissions
    public EncryptionProfileResponses list() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        verifyEncryptionProfileEntitlement(accountId);

        return encryptionProfileFiltering.filterResources(
                Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()),
                AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE,
                Collections.emptyMap());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_ENCRYPTION_PROFILE)
    public EncryptionProfileResponse deleteByName(@ResourceName String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        verifyEncryptionProfileEntitlement(accountId);

        EncryptionProfile deletedEncryptionProfile = encryptionProfileService.deleteByNameAndAccountId(name, accountId);
        notify(ResourceEvent.ENCRYPTION_PROFILE_DELETED);
        return encryptionProfileResponseConverter.convert(deletedEncryptionProfile, false);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_ENCRYPTION_PROFILE)
    public EncryptionProfileResponse deleteByCrn(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        verifyEncryptionProfileEntitlement(accountId);

        EncryptionProfile deletedEncryptionProfile = encryptionProfileService.deleteByResourceCrn(crn);
        notify(ResourceEvent.ENCRYPTION_PROFILE_DELETED);
        return encryptionProfileResponseConverter.convert(deletedEncryptionProfile, false);
    }

    private void verifyEncryptionProfileEntitlement(String accountId) {
        if (!entitlementService.isConfigureEncryptionProfileEnabled(accountId)) {
            throw new ForbiddenException("Encryption profile operations are not enabled for account: " + accountId);
        }
    }
}
