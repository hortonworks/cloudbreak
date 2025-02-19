package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.redbeams.api.model.common.Status.START_IN_PROGRESS;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.CollectMetadataResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class StackStartActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartActions.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private StackStartStopService stackStartStopService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private ResourceService resourceService;

    @Inject
    private DatabaseSslService databaseSslService;

    @Inject
    private CloudbreakEventService eventService;

    @Bean(name = "START_STATE")
    public Action<?, ?> stackStartAction() {
        return new AbstractStackStartAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StackEvent payload, Map<Object, Object> variables) {
                databaseSslService.getDbSslDetailsForRotationAndUpdateInCluster(payload.getResourceId());
                stackStartStopService.startStackStart(context);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                StackDto stackDto = context.getStack();
                StackView stack = stackDto.getStack();
                LOGGER.debug("Assembling start request for stack: {}", stack.getId());
                List<CloudInstance> cloudInstances = instanceMetaDataToCloudInstanceConverter.convert(stackDto.getInstanceGroupDtos(), stack);
                List<CloudResource> resources = resourceService.getAllByStackId(stack.getId()).stream()
                        .map(s -> resourceToCloudResourceConverter.convert(s))
                        .collect(Collectors.toList());
                cloudInstances.forEach(instance -> stackDto.getParameters().forEach(instance::putParameter));
                sendPreStartNotification(context, resources, cloudInstances);
                return new StartInstancesRequest(context.getCloudContext(), context.getCloudCredential(), resources, cloudInstances);
            }

            private void sendPreStartNotification(StackStartStopContext context, List<CloudResource> resources, List<CloudInstance> cloudInstances) {
                try {
                    CloudConnector connector = cloudPlatformConnectors.get(context.getCloudContext().getPlatformVariant());
                    AuthenticatedContext authenticatedContext = connector.authentication().authenticate(context.getCloudContext(), context.getCloudCredential());
                    connector.notifications().flatMap(nc -> nc.preStartNotificiationEvent(authenticatedContext, resources, cloudInstances))
                            .ifPresent(notificationEvent ->
                                    eventService.fireCloudbreakEvent(context.getStack().getId(), START_IN_PROGRESS.name(), notificationEvent));
                } catch (Exception e) {
                    LOGGER.error("Failed to send notification before starting instances on provider, skipping. Reason:", e);
                }
            }
        };
    }

    @Bean(name = "COLLECTING_METADATA")
    public Action<?, ?> collectingMetadataAction() {
        return new AbstractStackStartAction<>(StartInstancesResult.class) {
            @Override
            protected void doExecute(StackStartStopContext context, StartInstancesResult payload, Map<Object, Object> variables) {
                stackStartStopService.validateStackStartResult(context, payload);
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                StackDto stackDto = context.getStack();
                List<CloudInstance> cloudInstances = cloudStackConverter.buildInstances(stackDto);
                List<CloudResource> cloudResources = resourceService.getAllByStackId(stackDto.getId()).stream()
                        .map(s -> resourceToCloudResourceConverter.convert(s))
                        .collect(Collectors.toList());
                return new CollectMetadataRequest(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances, cloudInstances);
            }
        };
    }

    @Bean(name = "START_FINISHED_STATE")
    public Action<?, ?> startFinishedAction() {
        return new AbstractStackStartAction<>(CollectMetadataResult.class) {
            @Override
            protected void doExecute(StackStartStopContext context, CollectMetadataResult payload, Map<Object, Object> variables) {
                stackStartStopService.finishStackStart(context, payload.getResults());
                getMetricService().incrementMetricCounter(MetricType.STACK_START_SUCCESSFUL, context.getStack().getStack());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackStartStopContext context) {
                return new StackEvent(StackStartEvent.START_FINALIZED_EVENT.event(), context.getStack().getId());
            }
        };
    }

    @Bean(name = "START_FAILED_STATE")
    public Action<?, ?> stackStartFailedAction() {
        return new AbstractStackFailureAction<StackStartState, StackStartEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                stackStartStopService.handleStackStartError(context.getStack(), payload);
                getMetricService().incrementMetricCounter(MetricType.STACK_START_FAILED, context.getStack(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackStartEvent.START_FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

    private abstract static class AbstractStackStartAction<P extends Payload>
            extends AbstractStackAction<StackStartState, StackStartEvent, StackStartStopContext, P> {
        private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStackStartAction.class);

        @Inject
        private StackDtoService stackDtoService;

        @Inject
        private StackUtil stackUtil;

        protected AbstractStackStartAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected StackStartStopContext createFlowContext(FlowParameters flowParameters, StateContext<StackStartState, StackStartEvent> stateContext,
            P payload) {
            Long stackId = payload.getResourceId();
            StackDto stackDto = stackDtoService.getById(stackId);
            StackView stack = stackDto.getStack();
            MDCBuilder.buildMdcContext(stack);
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            CloudContext cloudContext = CloudContext.Builder.builder()
                    .withId(stack.getId())
                    .withName(stack.getName())
                    .withCrn(stack.getResourceCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withVariant(stack.getPlatformVariant())
                    .withLocation(location)
                    .withWorkspaceId(stack.getWorkspaceId())
                    .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                    .withTenantId(stackDto.getTenant().getId())
                    .build();
            CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
            return new StackStartStopContext(flowParameters, stackDto, stackDto.getInstanceGroupDtos(), cloudContext, cloudCredential);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<StackStartStopContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getResourceId(), ex);
        }
    }
}
