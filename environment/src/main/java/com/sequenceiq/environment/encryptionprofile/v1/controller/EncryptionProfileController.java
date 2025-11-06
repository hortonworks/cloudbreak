package com.sequenceiq.environment.encryptionprofile.v1.controller;

import java.util.Collections;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.CipherSuitesByTlsVersionResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponses;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.TlsVersionResponse;
import com.sequenceiq.environment.authorization.EncryptionProfileFiltering;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;
import com.sequenceiq.environment.encryptionprofile.service.EncryptionProfileService;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileRequestToEncryptionProfileConverter;
import com.sequenceiq.environment.encryptionprofile.v1.converter.EncryptionProfileToEncryptionProfileResponseConverter;
import com.sequenceiq.notification.WebSocketNotificationController;

@Controller
public class EncryptionProfileController extends WebSocketNotificationController implements EncryptionProfileEndpoint {
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

        if (request.getTlsVersions().size() == 1 && request.getTlsVersions().contains(TlsVersion.TLS_1_3) &&
                !entitlementService.isTlsv13OnlyEnabled(accountId)) {
            throw new BadRequestException("TLS 1.3 only is not supported yet. Use TLSv1.2 and TLSv1.3 or TLSv1.2 only");
        }

        EncryptionProfile createdEncryptionProfile = encryptionProfileService
                .create(encryptionProfileConverter.convert(request), accountId, creator);
        notify(ResourceEvent.ENCRYPTION_PROFILE_CREATED);

        return encryptionProfileResponseConverter.convert(createdEncryptionProfile);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE)
    public EncryptionProfileResponse getByName(@ResourceName String encryptionProfileName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EncryptionProfile encryptionProfile = encryptionProfileService.getByNameAndAccountId(
                encryptionProfileName, accountId);

        return encryptionProfileResponseConverter.convert(encryptionProfile);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE)
    public EncryptionProfileResponse getByCrn(@ResourceCrn String encryptionProfileCrn) {
        EncryptionProfile encryptionProfile = encryptionProfileService.getByCrn(encryptionProfileCrn);
        return encryptionProfileResponseConverter.convert(encryptionProfile);
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public EncryptionProfileResponse getDefaultEncryptionProfile() {
        EncryptionProfile encryptionProfile = encryptionProfileService.getClouderaDefaultEncryptionProfile();
        return encryptionProfileResponseConverter.convert(encryptionProfile);
    }

    @Override
    @FilterListBasedOnPermissions
    public EncryptionProfileResponses list() {
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
        return encryptionProfileResponseConverter.convert(deletedEncryptionProfile);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_ENCRYPTION_PROFILE)
    public EncryptionProfileResponse deleteByCrn(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();

        verifyEncryptionProfileEntitlement(accountId);

        EncryptionProfile deletedEncryptionProfile = encryptionProfileService.deleteByResourceCrn(crn);
        notify(ResourceEvent.ENCRYPTION_PROFILE_DELETED);
        return encryptionProfileResponseConverter.convert(deletedEncryptionProfile);
    }

    @Override
    @FilterListBasedOnPermissions
    public CipherSuitesByTlsVersionResponse listCiphersByTlsVersion() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        verifyEncryptionProfileEntitlement(accountId);

        Set<TlsVersionResponse> tlsVersions = encryptionProfileService.listCiphersByTlsVersion();

        return new CipherSuitesByTlsVersionResponse(tlsVersions);
    }

    private void verifyEncryptionProfileEntitlement(String accountId) {
        if (!entitlementService.isConfigureEncryptionProfileEnabled(accountId)) {
            throw new ForbiddenException("Encryption profile operations are not enabled for account: " + accountId);
        }
    }
}
