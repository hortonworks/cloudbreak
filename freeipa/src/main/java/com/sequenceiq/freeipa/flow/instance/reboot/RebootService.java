package com.sequenceiq.freeipa.flow.instance.reboot;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.REPAIR_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.REPAIR_IN_PROGRESS;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.start.FreeIpaServiceStartService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.sync.StackStatusCheckerJob;

@Component
public class RebootService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebootService.class);

    @Inject
    private StackStatusCheckerJob stackStatusCheckerJob;

    @Inject
    private FreeIpaServiceStartService freeIpaServiceStartService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void startInstanceReboot(RebootContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), REPAIR_IN_PROGRESS, "Starting to reboot instances");
        instanceMetaDataService.updateStatus(stack, context.getInstanceIdList(), InstanceStatus.REBOOTING);
    }

    public void handleInstanceRebootError(RebootContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), REPAIR_FAILED, "Rebooting instances failed");
        instanceMetaDataService.updateStatus(stack, context.getInstanceIdList(), InstanceStatus.FAILED);
        LOGGER.error("Reboot failed for instances: {}", context.getInstanceIds());
    }

    public void waitForAvailableStatus(RebootContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), REPAIR_IN_PROGRESS, "Waiting for FreeIpa to be available");
        instanceMetaDataService.updateStatus(stack, context.getInstanceIdList(), InstanceStatus.REBOOTING);
    }

    public void finishInstanceReboot(RebootContext context) {
        Stack stack = context.getStack();
        instanceMetaDataService.updateStatus(stack, context.getInstanceIdList(), InstanceStatus.CREATED);
    }
}
