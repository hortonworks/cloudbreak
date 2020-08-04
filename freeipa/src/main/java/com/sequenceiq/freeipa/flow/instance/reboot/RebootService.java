package com.sequenceiq.freeipa.flow.instance.reboot;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.REPAIR_COMPLETED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.REPAIR_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.REPAIR_IN_PROGRESS;

import javax.inject.Inject;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@Component
public class RebootService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebootService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private FreeipaJobService jobService;

    public void startInstanceReboot(RebootContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), REPAIR_IN_PROGRESS, "Starting to reboot instances");
        instanceMetaDataService.updateStatus(stack, context.getInstanceIdList(), InstanceStatus.REBOOTING);
        stopStatusChecker(context);
    }

    private void stopStatusChecker(RebootContext context) {
        LOGGER.info("Disable statuschecker while rebooting instances");
        jobService.unschedule(context.getStack());
    }

    public void handleInstanceRebootError(RebootContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), REPAIR_FAILED, "Rebooting instances failed");
        instanceMetaDataService.updateStatus(stack, context.getInstanceIdList(), InstanceStatus.FAILED);
        LOGGER.error("Reboot failed for instances: {}", context.getInstanceIds());
        reenableStatusChecker(context);
    }

    public void finishInstanceReboot(RebootContext context, RebootInstancesResult rebootInstancesResult) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), REPAIR_COMPLETED, "Finished rebooting instances");
        instanceMetaDataService.updateStatus(stack, context.getInstanceIdList(), InstanceStatus.CREATED);
        reenableStatusChecker(context);
    }

    private void reenableStatusChecker(RebootContext context) {
        LOGGER.info("Reenable status checker after reboot attempt");
        jobService.schedule(context.getStack());
    }
}
