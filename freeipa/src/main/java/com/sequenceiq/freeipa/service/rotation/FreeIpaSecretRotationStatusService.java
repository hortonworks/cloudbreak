package com.sequenceiq.freeipa.service.rotation;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.status.DefaultSecretRotationStatusService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Primary
@Component
public class FreeIpaSecretRotationStatusService extends DefaultSecretRotationStatusService {

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    public void rotationStarted(String resourceCrn, SecretType secretType) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.SECRET_ROTATION_STARTED, "Secret rotation started");
    }

    @Override
    public void rotationFinished(String resourceCrn, SecretType secretType) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.SECRET_ROTATION_FINISHED, "Secret rotation finished");
    }

    @Override
    public void rotationFailed(String resourceCrn, SecretType secretType, String statusReason) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.SECRET_ROTATION_FAILED, "Secret rotation failed");
    }

    @Override
    public void preVaildationFailed(String resourceCrn, SecretType secretType, String statusReason) {
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.AVAILABLE, "");
    }
}
