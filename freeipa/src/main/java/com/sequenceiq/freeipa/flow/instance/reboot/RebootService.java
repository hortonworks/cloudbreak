package com.sequenceiq.freeipa.flow.instance.reboot;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.instance.RebootInstancesResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.service.stack.RebootInstancesService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class RebootService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RebootService.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private RebootInstancesService rebootInstancesService;

    public void startInstanceReboot(RebootContext context) {
        instanceMetaDataService.updateStatus(context.getStack(), context.getInstanceIdList(), InstanceStatus.REBOOTING);
    }

    public void handleInstanceRebootError(RebootContext context) {
        instanceMetaDataService.updateStatus(context.getStack(), context.getInstanceIdList(), InstanceStatus.FAILED);
        LOGGER.error("Reboot failed for instances: {}", context.getInstanceIds());
    }

    public void finishInstanceReboot(RebootContext context, RebootInstancesResult rebootInstancesResult) {
        instanceMetaDataService.updateStatus(context.getStack(), context.getInstanceIdList(), InstanceStatus.CREATED);
    }
}
