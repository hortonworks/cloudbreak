package com.sequenceiq.environment.encryptionprofile.v1.controller;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
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

    public EncryptionProfileController(EncryptionProfileService encryptionProfileService,
            EncryptionProfileRequestToEncryptionProfileConverter encryptionProfileConverter,
            EncryptionProfileToEncryptionProfileResponseConverter encryptionProfileResponseConverter) {
        this.encryptionProfileService = encryptionProfileService;
        this.encryptionProfileConverter = encryptionProfileConverter;
        this.encryptionProfileResponseConverter = encryptionProfileResponseConverter;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENCRYPTION_PROFILE)
    public EncryptionProfileResponse create(EncryptionProfileRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        String creator = ThreadBasedUserCrnProvider.getUserCrn();

        EncryptionProfile createdEncryptionProfile = encryptionProfileService
                .create(encryptionProfileConverter.convert(request), accountId, creator);
        notify(ResourceEvent.ENCRYPTION_PROFILE_CREATED);

        return encryptionProfileResponseConverter.convert(createdEncryptionProfile);
    }
}
