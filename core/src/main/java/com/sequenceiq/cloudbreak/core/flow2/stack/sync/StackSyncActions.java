package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;

@Configuration
public class StackSyncActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackSyncActions.class);

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Inject
    private StackSyncService stackSyncService;

    @Inject
    private FlowMessageService flowMessageService;

    @Bean(name = "SYNC_STATE")
    public Action<?, ?> stackSyncAction() {
        return new AbstractStackSyncAction<>(StackSyncTriggerEvent.class) {
            @Override
            protected void prepareExecution(StackSyncTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(STATUS_UPDATE_ENABLED, payload.getStatusUpdateEnabled());
            }

            @Override
            protected void doExecute(StackSyncContext context, StackSyncTriggerEvent payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackSyncContext context) {
                List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(context.getInstanceMetaData());
                cloudInstances.forEach(instance -> context.getStack().getParameters().forEach(instance::putParameter));
                return new GetInstancesStateRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudInstances);
            }
        };
    }

    @Bean(name = "SYNC_FINISHED_STATE")
    public Action<?, ?> stackSyncFinishedAction() {
        return new AbstractStackSyncAction<GetInstancesStateResult>(GetInstancesStateResult.class) {
            @Override
            protected void doExecute(StackSyncContext context, GetInstancesStateResult payload, Map<Object, Object> variables) {
                stackSyncService.updateInstances(context.getStack(), context.getInstanceMetaData(), payload.getStatuses(), context.isStatusUpdateEnabled());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackSyncContext context) {
                return new StackEvent(StackSyncEvent.SYNC_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "SYNC_FAILED_STATE")
    public Action<?, ?> stackSyncFailedAction() {
        return new AbstractStackFailureAction<StackSyncState, StackSyncEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Error during Stack synchronization flow:", payload.getException());
                flowMessageService.fireEventAndLog(context.getStackView().getId(), Msg.STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE, UPDATE_FAILED.name());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackSyncEvent.SYNC_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }

    private abstract static class AbstractStackSyncAction<P extends Payload> extends AbstractAction<StackSyncState, StackSyncEvent, StackSyncContext, P> {
        static final String STATUS_UPDATE_ENABLED = "STATUS_UPDATE_ENABLED";

        @Inject
        private StackService stackService;

        @Inject
        private CredentialToCloudCredentialConverter credentialConverter;

        @Inject
        private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

        protected AbstractStackSyncAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected StackSyncContext createFlowContext(String flowId, StateContext<StackSyncState, StackSyncEvent> stateContext, P payload) {
            Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
            Long stackId = payload.getStackId();
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            MDCBuilder.buildMdcContext(stack);
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getPlatformVariant(),
                    location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
            CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
            return new StackSyncContext(flowId, stack, stack.getNotTerminatedInstanceMetaDataList(), cloudContext, cloudCredential,
                    isStatusUpdateEnabled(variables));
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<StackSyncContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getStackId(), ex);
        }

        private Boolean isStatusUpdateEnabled(Map<Object, Object> variables) {
            return (Boolean) variables.get(STATUS_UPDATE_ENABLED);
        }
    }
}
