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

import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeHandlerRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class DiskResizeActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiskResizeActions.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "DISK_UPDATE_STATE")
    public Action<?, ?> diskUpdateAction() {
        return new AbstractDiskResizeAction<>(DiskResizeRequest.class) {
            @Override
            protected void doExecute(CommonContext ctx, DiskResizeRequest payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting disk resize for stack: {}", payload.getResourceId());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        UPDATE_IN_PROGRESS.name(),
                        DISK_RESIZE_STARTED,
                        String.valueOf(payload.getResourceId()));
                DiskResizeHandlerRequest handlerRequest = new DiskResizeHandlerRequest(DISK_RESIZE_HANDLER_EVENT.selector(), payload.getResourceId(),
                        payload.getInstanceGroup());
                sendEvent(ctx, DISK_RESIZE_HANDLER_EVENT.event(), handlerRequest);
            }
        };
    }

    @Bean(name = "DISK_UPDATE_FINISHED_STATE")
    public Action<?, ?> diskUpdateFinishedAction() {
        return new AbstractDiskResizeAction<>(DiskResizeFinishedEvent.class) {
            @Override
            protected void doExecute(CommonContext ctx, DiskResizeFinishedEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Disk resize for stack {} successfully done!", payload.getResourceId());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        AVAILABLE.name(),
                        DISK_RESIZE_COMPLETE,
                        String.valueOf(payload.getResourceId()));
                DiskResizeFinalizedEvent finalizedEvent = new DiskResizeFinalizedEvent(payload.getResourceId());
                sendEvent(ctx, FINALIZED_EVENT.event(), finalizedEvent);
            }
        };
    }

    @Bean(name = "DISK_UPDATE_FAILED_STATE")
    public Action<?, ?> diskUpdateFailedAction() {
        return new AbstractDiskResizeAction<>(DiskResizeFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, DiskResizeFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Exception during vertical scaling!: {}", payload.getException().getMessage());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        UPDATE_FAILED.name(),
                        DISK_RESIZE_FAILED,
                        payload.getException().getMessage());
                sendEvent(context, DISK_UPDATE_FAILURE_HANDLED_EVENT.selector(), payload);
            }
        };
    }
}
