package com.sequenceiq.freeipa.service.rebuild;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class RebuildService {
    @Inject
    private EntitlementService entitlementService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private StackService stackService;

    public DescribeFreeIpaResponse rebuild(String accountId, RebuildV2Request request) {
        validate(accountId);
        Stack stack = stackService.getFreeIpaStackWithMdcContext(request.getEnvironmentCrn(), accountId);
        RebuildEvent rebuildEvent = new RebuildEvent(stack.getId(), request.getInstanceToRestoreFqdn(), request.getFullBackupStorageLocation(),
                request.getDataBackupStorageLocation());
        flowManager.notify(rebuildEvent.selector(), rebuildEvent);
        return null;
    }

    private void validate(String accountId) {
        checkEntitlement(accountId);
    }

    private void checkEntitlement(String accountId) {
        if (!entitlementService.isFreeIpaRebuildEnabled(accountId)) {
            throw new BadRequestException("The FreeIPA rebuild capability is disabled.");
        }
    }
}
