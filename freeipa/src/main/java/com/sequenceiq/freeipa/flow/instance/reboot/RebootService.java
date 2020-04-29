package com.sequenceiq.freeipa.flow.instance.reboot;

import javax.inject.Inject;

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
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private FreeipaJobService jobService;

    public void startInstanceReboot(RebootContext context) {
        instanceMetaDataService.updateStatus(context.getStack(), context.getInstanceIdList(), InstanceStatus.REBOOTING);
        stopStatusChecker(context);
    }

    private void stopStatusChecker(RebootContext context) {
        LOGGER.info("Disable statuschecker while rebooting instances");
        jobService.unschedule(context.getStack());
    }

    public void handleInstanceRebootError(RebootContext context) {
        instanceMetaDataService.updateStatus(context.getStack(), context.getInstanceIdList(), InstanceStatus.FAILED);
        LOGGER.error("Reboot failed for instances: {}", context.getInstanceIds());
        reenableStatusChecker(context);
    }

    public void finishInstanceReboot(RebootContext context, RebootInstancesResult rebootInstancesResult) {
        instanceMetaDataService.updateStatus(context.getStack(), context.getInstanceIdList(), InstanceStatus.CREATED);
        reenableStatusChecker(context);
    }

    private void reenableStatusChecker(RebootContext context) {
        LOGGER.info("Reenable status checker after reboot attempt");
        jobService.schedule(context.getStack());
    }
}
