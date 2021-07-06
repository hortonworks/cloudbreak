package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.ArrayList;
import java.util.HashSet;
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

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class StackStopActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopActions.class);

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Inject
    private StackStartStopService stackStartStopService;

    @Bean(name = "STOP_STATE")
    public Action<?, ?> stackStopAction() {
        return new AbstractStackStopAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StackEvent payload, Map<Object, Object> variables) {
                stackStartStopService.startStackStop(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                Stack stack = context.getStack();
                List<CloudInstance> cloudInstances = instanceMetaDataToCloudInstanceConverter.convert(context.getInstanceMetaData(),
                        stack.getEnvironmentCrn(), stack.getStackAuthentication());
                List<CloudResource> cloudResources = converterUtil.convertAll(stack.getResources(), CloudResource.class);
                cloudInstances.forEach(instance -> stack.getParameters().forEach(instance::putParameter));
                return new StopInstancesRequest<StopInstancesResult>(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances);
            }
        };
    }

    @Bean(name = "STOP_FINISHED_STATE")
    public Action<?, ?> stackStopFinishedAction() {
        return new AbstractStackStopAction<>(StopInstancesResult.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StopInstancesResult payload, Map<Object, Object> variables) {
                stackStartStopService.finishStackStop(context, payload);
                getMetricService().incrementMetricCounter(MetricType.STACK_STOP_SUCCESSFUL, context.getStack());
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
                getMetricService().incrementMetricCounter(MetricType.STACK_STOP_FAILED, context.getStackView(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackStopEvent.STOP_FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }

    private abstract static class AbstractStackStopAction<P extends Payload>
            extends AbstractStackAction<StackStopState, StackStopEvent, StackStartStopContext, P> {
        @Inject
        private StackService stackService;

        @Inject
        private InstanceMetaDataService instanceMetaDataService;

        @Inject
        private StackUtil stackUtil;

        @Inject
        private ResourceService resourceService;

        protected AbstractStackStopAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected StackStartStopContext createFlowContext(FlowParameters flowParameters, StateContext<StackStopState, StackStopEvent> stateContext,
                P payload) {
            Long stackId = payload.getResourceId();
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            stack.setResources(new HashSet<>(resourceService.getAllByStackId(payload.getResourceId())));
            MDCBuilder.buildMdcContext(stack);
            List<InstanceMetaData> instances = new ArrayList<>(instanceMetaDataService.findNotTerminatedForStack(stackId));
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            CloudContext cloudContext = CloudContext.Builder.builder()
                    .withId(stack.getId())
                    .withName(stack.getName())
                    .withCrn(stack.getResourceCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withVariant(stack.getPlatformVariant())
                    .withLocation(location)
                    .withWorkspaceId(stack.getWorkspace().getId())
                    .withAccountId(stack.getTenant().getId())
                    .build();
            CloudCredential cloudCredential = stackUtil.getCloudCredential(stack);
            return new StackStartStopContext(flowParameters, stack, instances, cloudContext, cloudCredential);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<StackStartStopContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getResourceId(), ex);
        }
    }
}
