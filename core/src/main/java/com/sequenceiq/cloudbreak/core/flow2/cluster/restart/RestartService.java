package com.sequenceiq.cloudbreak.core.flow2.cluster.restart;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.INSTANCES_RESTART_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.INSTANCES_RESTART_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.INSTANCES_RESTART_STARTED;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Component
public class RestartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartService.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void startInstanceRestart(RestartContext context) {
        instanceMetaDataService.updateStatus(context.getStack().getId(), context.getInstanceIds(), InstanceStatus.RESTARTING);

        flowMessageService.fireEventAndLog(context.getStack().getId(), Status.UPDATE_IN_PROGRESS.name(),
                INSTANCES_RESTART_STARTED,
                String.valueOf(context.getInstanceIds().size()), String.join(", ", context.getInstanceIds()));
    }

    public void instanceRestartFinished(RestartContext context, List<String> failedToRestartInstanceIds, List<String> successOnRestartInstanceIds) {
        instanceMetaDataService.updateStatus(context.getStack().getId(), successOnRestartInstanceIds, InstanceStatus.SERVICES_HEALTHY);
        instanceMetaDataService.updateStatus(context.getStack().getId(), failedToRestartInstanceIds, InstanceStatus.FAILED);

        flowMessageService.fireEventAndLog(context.getStack().getId(), Status.AVAILABLE.name(),
                INSTANCES_RESTART_FINISHED,
                String.valueOf(successOnRestartInstanceIds.size()), String.join(", ", successOnRestartInstanceIds),
                String.valueOf(failedToRestartInstanceIds.size()), String.join(", ", failedToRestartInstanceIds));
    }

    public void allInstanceRestartFailed(RestartContext context, Exception errorDetails) {
        LOGGER.info("Error during restart instance(s) flow: " + errorDetails.getMessage(), errorDetails);
        instanceMetaDataService.updateStatus(context.getStack().getId(), context.getInstanceIds(), InstanceStatus.FAILED);
        flowMessageService.fireEventAndLog(context.getStack().getId(), Status.AVAILABLE.name(),
                INSTANCES_RESTART_FAILED);
    }
}
