package com.sequenceiq.cloudbreak.core.flow2.stack.termination.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.TerminationStackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.service.stack.flow.SyncConfig;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class TerminationStackSyncActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationStackSyncActions.class);

    @Inject
    private StackSyncService stackSyncService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "TERMINATION_SYNC_STATE")
    public Action<?, ?> stackSyncAction() {
        return new AbstractTerminationStackSyncAction<>(TerminationStackSyncTriggerEvent.class) {

            @Override
            protected void doExecute(TerminationStackSyncContext context, TerminationStackSyncTriggerEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(TerminationStackSyncContext context) {
                StackView stack = context.getStack();
                List<CloudInstance> cloudInstances = stackSyncService.getCloudInstances(context.getInstanceMetaData(), stack);
                return new GetInstancesStateRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudInstances);
            }
        };
    }

    @Bean(name = "TERMINATION_SYNC_FINISHED_STATE")
    public Action<?, ?> stackSyncFinishedAction() {
        return new AbstractTerminationStackSyncAction<>(GetInstancesStateResult.class) {
            @Override
            protected void doExecute(TerminationStackSyncContext context, GetInstancesStateResult payload, Map<Object, Object> variables) {
                SyncConfig syncConfig = new SyncConfig(false, true);
                stackSyncService.updateInstances(context.getStack(), context.getInstanceMetaData(), payload.getStatuses(), syncConfig);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(TerminationStackSyncContext context) {
                return new StackEvent(TerminationStackSyncEvent.TERMINATION_SYNC_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "TERMINATION_SYNC_FAILED_STATE")
    public Action<?, ?> stackSyncFailedAction() {
        return new AbstractStackFailureAction<TerminationStackSyncState, TerminationStackSyncEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Error during Stack synchronization flow:", payload.getException());
                flowMessageService.fireEventAndLog(context.getStackId(), UPDATE_FAILED.name(), STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(TerminationStackSyncEvent.TERMINATION_SYNC_FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
