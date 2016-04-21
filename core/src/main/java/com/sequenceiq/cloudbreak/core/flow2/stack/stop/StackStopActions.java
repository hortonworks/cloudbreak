package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

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

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopService;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Configuration
public class StackStopActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopActions.class);
    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;
    @Inject
    private StackStartStopService stackStartStopService;

    @Bean(name = "STOP_STATE")
    public Action stackStopAction() {
        return new AbstractStackStopAction<StackStatusUpdateContext>(StackStatusUpdateContext.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) throws Exception {
                stackStartStopService.startStackStop(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                if (stackStartStopService.isStopPossible(context.getStack())) {
                    List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(context.getInstanceMetaData());
                    List<CloudResource> cloudResources = cloudResourceConverter.convert(context.getStack().getResources());
                    return new StopInstancesRequest<StopInstancesResult>(context.getCloudContext(), context.getCloudCredential(),
                            cloudResources, cloudInstances);
                }
                //finish the flow
                return new SelectableFlowStackEvent(context.getStack().getId(), StackStopEvent.STOP_FAILURE_EVENT.stringRepresentation());
            }
        };
    }

    @Bean(name = "STOP_FINISHED_STATE")
    public Action stackStopFinishedAction() {
        return new AbstractStackStopAction<StopInstancesResult>(StopInstancesResult.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StopInstancesResult payload, Map<Object, Object> variables) throws Exception {
                stackStartStopService.finishStackStop(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                return new SelectableFlowStackEvent(context.getStack().getId(), StackStopEvent.STOP_FINALIZED_EVENT.stringRepresentation());
            }
        };
    }

    @Bean(name = "STOP_FAILED_STATE")
    public Action stackStopFailedAction() {
        return new AbstractStackStopAction<StopInstancesResult>(StopInstancesResult.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StopInstancesResult payload, Map<Object, Object> variables) throws Exception {
                stackStartStopService.handleStackStopError(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                return new SelectableFlowStackEvent(context.getStack().getId(), StackStopEvent.STOP_FAIL_HANDLED_EVENT.stringRepresentation());
            }
        };
    }

    private abstract static class AbstractStackStopAction<P extends Payload> extends AbstractAction<StackStopState, StackStopEvent, StackStartStopContext, P> {
        @Inject
        private StackService stackService;
        @Inject
        private InstanceMetaDataRepository instanceMetaDataRepository;
        @Inject
        private CredentialToCloudCredentialConverter credentialConverter;
        @Inject
        private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;
        @Inject
        private ResourceToCloudResourceConverter cloudResourceConverter;

        protected AbstractStackStopAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected StackStartStopContext createFlowContext(StateContext<StackStopState, StackStopEvent> stateContext, P payload) {
            String flowId = (String) stateContext.getMessageHeader(MessageFactory.HEADERS.FLOW_ID.name());
            Long stackId = payload.getStackId();
            Stack stack = stackService.getById(stackId);
            MDCBuilder.buildMdcContext(stack);
            List<InstanceMetaData> instances = new ArrayList<>(instanceMetaDataRepository.findNotTerminatedForStack(stackId));
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                    location);
            CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
            return new StackStartStopContext(flowId, stack, instances, cloudContext, cloudCredential);
        }

        @Override
        protected Object getFailurePayload(StackStartStopContext flowContext, Exception ex) {
            List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(flowContext.getInstanceMetaData());
            List<CloudResource> cloudResources = cloudResourceConverter.convert(flowContext.getStack().getResources());
            StopInstancesRequest<StopInstancesResult> stopRequest = new StopInstancesRequest<>(flowContext.getCloudContext(), flowContext.getCloudCredential(),
                    cloudResources, cloudInstances);
            return new StopInstancesResult(ex.getMessage(), ex, stopRequest);
        }

    }
}
