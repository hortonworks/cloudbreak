package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.FINALIZED_EVENT;

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
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncFinalizedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncProcessFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;

@Configuration
public class DiskSyncActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskSyncActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "DISK_SYNC_INIT_STATE")
    public Action<?, ?> diskSyncInitAction() {
        return new AbstractClusterAction<>(DiskSyncRequest.class) {
            @Override
            protected void doExecute(ClusterViewContext ctx, DiskSyncRequest payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                LOGGER.debug("Disk sync flow init for stack {}", stackId);
                sendEvent(ctx, DISK_SYNC_HANDLER_EVENT.event(), new DiskSyncHandlerEvent(stackId, payload.getDiskSyncMode()));
            }
        };
    }

    @Bean(name = "DISK_SYNC_FINISHED_STATE")
    public Action<?, ?> diskSyncFinishedAction() {
        return new AbstractClusterAction<>(DiskSyncProcessFinishedEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext ctx, DiskSyncProcessFinishedEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                LOGGER.debug("Disk sync finishing for stack {}", stackId);
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Disk metadata synchronization finished.");
                sendEvent(ctx, FINALIZED_EVENT.event(), new DiskSyncFinalizedEvent(stackId));
            }
        };
    }

    @Bean(name = "DISK_SYNC_FAILED_STATE")
    public Action<?, ?> diskSyncFailedAction() {
        return new AbstractStackFailureAction<DiskSyncState, DiskSyncEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(payload.getResourceId(), DetailedStackStatus.DISK_METADATA_SYNC_FAILED,
                    "Disk metadata synchronization failed.");
                LOGGER.warn("Disk sync flow failed for stack {}: {}", payload.getResourceId(), payload.getException().getMessage());
                sendEvent(context, DISK_SYNC_FAILURE_HANDLED_EVENT.selector(), payload);
            }
        };
    }
}
