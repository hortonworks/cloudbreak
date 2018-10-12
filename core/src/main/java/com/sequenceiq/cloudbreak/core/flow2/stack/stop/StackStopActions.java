package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.ArrayList;
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
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.type.MetricType;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;

@Configuration
public class StackStopActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopActions.class);

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private StackStartStopService stackStartStopService;

    @Bean(name = "STOP_STATE")
    public Action<?, ?> stackStopAction() {
        return new AbstractStackStopAction<StackEvent>(StackEvent.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StackEvent payload, Map<Object, Object> variables) {
                stackStartStopService.startStackStop(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                List<CloudInstance> cloudInstances = converterUtil.convertAll(context.getInstanceMetaData(), CloudInstance.class);
                List<CloudResource> cloudResources = converterUtil.convertAll(context.getStack().getResources(), CloudResource.class);
                cloudInstances.forEach(instance -> context.getStack().getParameters().forEach(instance::putParameter));
                return new StopInstancesRequest<StopInstancesResult>(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances);
            }
        };
    }

    @Bean(name = "STOP_FINISHED_STATE")
    public Action<?, ?> stackStopFinishedAction() {
        return new AbstractStackStopAction<StopInstancesResult>(StopInstancesResult.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StopInstancesResult payload, Map<Object, Object> variables) {
                stackStartStopService.finishStackStop(context, payload);
                metricService.incrementMetricCounter(MetricType.STACK_STOP_SUCCESSFUL, context.getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                return new StackEvent(StackStopEvent.STOP_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "STOP_FAILED_STATE")
    public Action<?, ?> stackStopFailedAction() {
        return new AbstractStackFailureAction<StackStopState, StackStopEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackStartStopService.handleStackStopError(context.getStackView(), payload);
                metricService.incrementMetricCounter(MetricType.STACK_STOP_FAILED, context.getStackView());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackStopEvent.STOP_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
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
        protected StackStartStopContext createFlowContext(String flowId, StateContext<StackStopState, StackStopEvent> stateContext, P payload) {
            Long stackId = payload.getStackId();
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            MDCBuilder.buildMdcContext(stack);
            List<InstanceMetaData> instances = new ArrayList<>(instanceMetaDataRepository.findNotTerminatedForStack(stackId));
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            CloudContext cloudContext = new CloudContext(stack.getId(), stack.getName(), stack.cloudPlatform(), stack.getOwner(), stack.getPlatformVariant(),
                    location, stack.getCreator().getUserId(), stack.getWorkspace().getId());
            CloudCredential cloudCredential = credentialConverter.convert(stack.getCredential());
            return new StackStartStopContext(flowId, stack, instances, cloudContext, cloudCredential);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<StackStartStopContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getStackId(), ex);
        }
    }
}
