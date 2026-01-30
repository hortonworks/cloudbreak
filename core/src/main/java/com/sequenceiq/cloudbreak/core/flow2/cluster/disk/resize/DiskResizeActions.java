package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_RESIZE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_UPDATE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_RESIZE_COMPLETE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_RESIZE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DISK_RESIZE_STARTED;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeHandlerRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class DiskResizeActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiskResizeActions.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "DISK_UPDATE_STATE")
    public Action<?, ?> diskUpdateAction() {
        return new AbstractClusterAction<>(DiskResizeRequest.class) {
            @Override
            protected void doExecute(ClusterViewContext ctx, DiskResizeRequest payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                String targetInstanceGroup = payload.getInstanceGroup();
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.UPDATE_ATTACHED_VOLUMES, String.format("Resizing volumes for instances on the " +
                        "host group %s", targetInstanceGroup));
                LOGGER.debug("Starting disk resize for stack: {}", stackId);
                flowMessageService.fireEventAndLog(stackId,
                        UPDATE_IN_PROGRESS.name(),
                        DISK_RESIZE_STARTED,
                        targetInstanceGroup);
                DiskResizeHandlerRequest handlerRequest = new DiskResizeHandlerRequest(
                        DISK_RESIZE_HANDLER_EVENT.selector(),
                        stackId,
                        targetInstanceGroup,
                        payload.getVolumeType(),
                        payload.getSize(),
                        payload.getVolumesToUpdate());
                sendEvent(ctx, DISK_RESIZE_HANDLER_EVENT.event(), handlerRequest);
            }
        };
    }

    @Bean(name = "DISK_UPDATE_FINISHED_STATE")
    public Action<?, ?> diskUpdateFinishedAction() {
        return new AbstractClusterAction<>(DiskResizeFinishedEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext ctx, DiskResizeFinishedEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                LOGGER.debug("Disk resize for stack {} successfully done!", stackId);
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Resizing volumes for instances is complete.");
                flowMessageService.fireEventAndLog(stackId,
                        AVAILABLE.name(),
                        DISK_RESIZE_COMPLETE);
                DiskResizeFinalizedEvent finalizedEvent = new DiskResizeFinalizedEvent(stackId);
                sendEvent(ctx, FINALIZED_EVENT.event(), finalizedEvent);
            }
        };
    }

    @Bean(name = "DISK_UPDATE_FAILED_STATE")
    public Action<?, ?> diskUpdateFailedAction() {
        return new AbstractStackFailureAction<DiskResizeState, DiskResizeEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.AVAILABLE, "Failed Resizing volumes");
                LOGGER.info("Exception during vertical scaling!: {}", payload.getException().getMessage());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        UPDATE_FAILED.name(),
                        DISK_RESIZE_FAILED,
                        context.getStack().getName(),
                        payload.getException().getMessage());
                sendEvent(context, DISK_UPDATE_FAILURE_HANDLED_EVENT.selector(), payload);
            }
        };
    }
}
