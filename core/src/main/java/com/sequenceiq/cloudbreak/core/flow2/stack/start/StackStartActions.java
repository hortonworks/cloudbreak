package com.sequenceiq.cloudbreak.core.flow2.stack.start;

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
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow.FlowPhases;
import com.sequenceiq.cloudbreak.core.flow.context.StackStatusUpdateContext;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.MessageFactory;
import com.sequenceiq.cloudbreak.core.flow2.SelectableEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.SelectableFlowStackEvent;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Configuration
public class StackStartActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartActions.class);
    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;
    @Inject
    private ResourceToCloudResourceConverter cloudResourceConverter;
    @Inject
    private StackStartStopService stackStartStopService;


    @Bean(name = "START_STATE")
    public Action stackStartAction() {
        return new AbstractStackStartAction<StackStatusUpdateContext>(StackStatusUpdateContext.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StackStatusUpdateContext payload, Map<Object, Object> variables) throws Exception {
                stackStartStopService.startStackStart(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                LOGGER.info("Assembling start request for stack: {}", context.getStack());
                List<CloudInstance> instances = cloudInstanceConverter.convert(context.getStack().getInstanceMetaDataAsList());
                List<CloudResource> resources = cloudResourceConverter.convert(context.getStack().getResources());
                return new StartInstancesRequest(context.getCloudContext(), context.getCloudCredential(), resources, instances);
            }
        };
    }

    @Bean(name = "START_FINISHED_STATE")
    public Action stackStartFinishedAction() {
        return new AbstractStackStartAction<StartInstancesResult>(StartInstancesResult.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StartInstancesResult payload, Map<Object, Object> variables) throws Exception {
                stackStartStopService.finishStackStart(context);
                sendEvent(context);
                sendEvent(context.getFlowId(), FlowPhases.METADATA_COLLECT.name(),
                        new StackStatusUpdateContext(context.getStack().getId(), Platform.platform(context.getStack().cloudPlatform()), true));
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                return new SelectableEvent(StackStartEvent.START_FINALIZED_EVENT.stringRepresentation());
            }
        };
    }

    @Bean(name = "START_FAILED_STATE")
    public Action stackStartFailedAction() {
        return new AbstractStackStartAction<StartInstancesResult>(StartInstancesResult.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StartInstancesResult payload, Map<Object, Object> variables) throws Exception {
                stackStartStopService.handleStackStartError(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                return new SelectableFlowStackEvent(context.getStack().getId(), StackStartEvent.START_FAIL_HANDLED_EVENT.stringRepresentation());
            }
        };
    }

    private abstract static class AbstractStackStartAction<P extends Payload>
                extends AbstractAction<StackStartState, StackStartEvent, StackStartStopContext, P> {
        private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStackStartAction.class);
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
        @Inject
        private CloudbreakMessagesService messagesService;
        @Inject
        private CloudbreakEventService cloudbreakEventService;

        protected AbstractStackStartAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected StackStartStopContext createFlowContext(StateContext<StackStartState, StackStartEvent> stateContext, P payload) {
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
            StartInstancesRequest startRequest = new StartInstancesRequest(flowContext.getCloudContext(), flowContext.getCloudCredential(),
                    cloudResources, cloudInstances);
            return new StartInstancesResult(ex.getMessage(), ex, startRequest);
        }
    }
}
