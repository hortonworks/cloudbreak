package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.model.Status;
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
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
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
    public Action stackSyncAction() {
        return new AbstractStackSyncAction<StackStatusUpdateContext>(StackStatusUpdateContext.class) {
            @Override
            protected void doExecute(StackSyncContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackSyncContext context) {
                List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(context.getInstanceMetaData());
                return new GetInstancesStateRequest<GetInstancesStateResult>(context.getCloudContext(), context.getCloudCredential(), cloudInstances);
            }
        };
    }

    @Bean(name = "SYNC_FINISHED_STATE")
    public Action stackSyncFinishedAction() {
        return new AbstractStackSyncAction<GetInstancesStateResult>(GetInstancesStateResult.class) {
            @Override
            protected void doExecute(StackSyncContext context, GetInstancesStateResult payload, Map<Object, Object> variables) throws Exception {
                // TODO !(actualContext instanceof StackScalingContext) requires for sync during upscale      here
                stackSyncService.updateInstances(context.getStack(), context.getInstanceMetaData(), payload.getStatuses(), true);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackSyncContext context) {
                return new SelectableFlowStackEvent(context.getStack().getId(), StackSyncEvent.SYNC_FINALIZED_EVENT.stringRepresentation());
            }
        };
    }

    @Bean(name = "SYNC_FAILED_STATE")
    public  Action stackSyncFailedAction() {
        return new AbstractStackSyncAction<GetInstancesStateResult>(GetInstancesStateResult.class) {
            @Override
            protected void doExecute(StackSyncContext context, GetInstancesStateResult payload, Map<Object, Object> variables) throws Exception {
                LOGGER.error("Error during Stack synchronization flow:", payload.getErrorDetails());
                flowMessageService.fireEventAndLog(context.getStack().getId(), Msg.STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE, Status.AVAILABLE.name());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackSyncContext context) {
                return new SelectableFlowStackEvent(context.getStack().getId(), StackSyncEvent.SYNC_FAIL_HANDLED_EVENT.stringRepresentation());
            }

            @Override
            protected Object getFailurePayload(StackSyncContext flowContext, Exception ex) {
                return null;
            }
        };
    }

    private abstract static class AbstractStackSyncAction<P extends Payload> extends AbstractAction<StackSyncState, StackSyncEvent, StackSyncContext, P> {
        @Inject
        private StackService stackService;
        @Inject
        private InstanceMetaDataRepository instanceMetaDataRepository;
        @Inject
        private CredentialToCloudCredentialConverter credentialConverter;
        @Inject
        private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

        protected AbstractStackSyncAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected StackSyncContext createFlowContext(StateContext<StackSyncState, StackSyncEvent> stateContext, P payload) {
            String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
            Long stackId = payload.getStackId();
            Stack stack = stackService.getById(stackId);
            MDCBuilder.buildMdcContext(stack);
            List<InstanceMetaData> instances = new ArrayList<>(instanceMetaDataRepository.findNotTerminatedForStack(stackId));
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                    location);
            CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
            return new StackSyncContext(flowId, stack, instances, cloudContext, cloudCredential);
        }

        @Override
        protected Object getFailurePayload(StackSyncContext flowContext, Exception ex) {
            List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(flowContext.getInstanceMetaData());
            GetInstancesStateRequest<GetInstancesStateResult> stateRequest =
                    new GetInstancesStateRequest<>(flowContext.getCloudContext(), flowContext.getCloudCredential(), cloudInstances);
            return new GetInstancesStateResult(ex.getMessage(), ex, stateRequest);
        }
    }
}
