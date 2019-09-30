package com.sequenceiq.freeipa.flow.stack.start;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackStartStopService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.MetadataSetupService;

@Service
public class StackStartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private MetadataSetupService metadataSetupService;

    @Inject
    private StackStartStopService stackStartStopService;

    public void startStack(Stack stack) {
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.START_IN_PROGRESS, "Stack infrastructure is now starting.");
    }

    public void validateStackStartResult(StackStartContext context, StartInstancesResult startInstancesResult) {
        stackStartStopService.validateResourceResults(context.getCloudContext(),
                startInstancesResult.getErrorDetails(), startInstancesResult.getResults(), true);
    }

    public void finishStackStart(StackStartContext context, List<CloudVmMetaDataStatus> coreInstanceMetaData) {
        Stack stack = context.getStack();
        metadataSetupService.saveInstanceMetaData(stack, coreInstanceMetaData, null);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STARTED, "Stack infrastructure started successfully.");
    }

    public void handleStackStartError(Stack stack, StackFailureEvent payload) {
        String logMessage = "Stack start failed: ";
        LOGGER.info(logMessage, payload.getException());
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.START_FAILED, logMessage + payload.getException().getMessage());
    }
}
