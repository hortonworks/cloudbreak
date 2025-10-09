package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.HashMap;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.event.RollingVerticalScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleInstancesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStartInstancesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStopInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.RollingVerticalScaleStopInstancesResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class RollingVerticalScaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RollingVerticalScaleActions.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private StackToCloudStackConverter cloudStackConverter;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Inject
    private RollingVerticalScaleService rollingVerticalScaleService;

    @Bean(name = "ROLLING_VERTICALSCALE_STOP_INSTANCES_STATE")
    public Action<?, ?> stopInstancesAction() {
        return new AbstractRollingVerticalScaleActions<>(RollingVerticalScaleTriggerEvent.class) {
            @Override
            protected void prepareExecution(RollingVerticalScaleTriggerEvent payload, Map<Object, Object> variables) {
                StackVerticalScaleV4Request request = payload.getStackVerticalScaleV4Request();
                Optional<InstanceGroupView> optionalGroup =
                        instanceGroupService.findInstanceGroupViewByStackIdAndGroupName(payload.getResourceId(), request.getGroup());
                String previousInstanceType = optionalGroup.map(InstanceGroupView::getTemplate).map(Template::getInstanceType).orElse("unknown");
                variables.put(PREVIOUS_INSTANCE_TYPE, previousInstanceType);
                variables.put(TARGET_INSTANCE_TYPE, previousInstanceType);
                if (request.getTemplate().getInstanceType() != null) {
                    variables.put(TARGET_INSTANCE_TYPE, request.getTemplate().getInstanceType());
                } else if ("unknown".equals(previousInstanceType)) {
                    throw new CloudbreakRuntimeException(String.format("Cannot determine instance type for vertical scale. " +
                            "Input request %s template does not contain target instance type", request));
                }
                variables.put(GROUP_BEING_SCALED, request.getGroup());
                variables.put(TARGET_INSTANCES, payload.getInstanceIds());
                variables.put(STACK_VERTICALSCALE_V4_REQUEST, payload.getStackVerticalScaleV4Request());
            }

            @Override
            protected void doExecute(RollingVerticalScaleContext context, RollingVerticalScaleTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Stopping instances to vertical scale: count={}, instanceIds=[{}]", context.getInstanceIds().size(), context.getInstanceIds());

                StackDtoDelegate stack = context.getStack();
                String targetInstanceType = context.getTargetInstanceType();
                List<String> instanceIds = payload.getInstanceIds();
                String targetGroup = payload.getStackVerticalScaleV4Request().getGroup();
                RollingVerticalScaleResult result = new RollingVerticalScaleResult(instanceIds, targetGroup);

                List<InstanceMetadataView> instances = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(context.getStack().getId()).stream()
                        .filter(instanceMetaData -> context.getInstanceIds().contains(instanceMetaData.getInstanceId())).collect(Collectors.toList());
                List<CloudInstance> cloudInstances = instanceMetaDataToCloudInstanceConverter.convert(instances, stack.getStack());
                List<CloudResource> cloudResources = getCloudResources(context.getStack().getId());

                RollingVerticalScaleStopInstancesRequest request = new RollingVerticalScaleStopInstancesRequest(payload.getResourceId(),
                        context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances, targetInstanceType, result);
                sendEvent(context, request);
            }

            private List<CloudResource> getCloudResources(Long stackId) {
                List<Resource> resources = (List<Resource>) resourceService.getAllByStackId(stackId);
                return resources.stream()
                        .map(r -> resourceToCloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
            }
        };
    }

    @Bean(name = "ROLLING_VERTICALSCALE_SCALE_INSTANCES_STATE")
    public Action<?, ?> scaleInstancesActions() {
        return new AbstractRollingVerticalScaleActions<>(RollingVerticalScaleStopInstancesResult.class) {

            @Override
            protected void doExecute(RollingVerticalScaleContext context, RollingVerticalScaleStopInstancesResult payload,
                    Map<Object, Object> variables) throws Exception {
                StackVerticalScaleV4Request stackVerticalScaleV4Request = context.getStackVerticalScaleV4Request();
                RollingVerticalScaleResult rollingVerticalScaleResult = payload.getRollingVerticalScaleResult();
                List<String> stoppedInstanceIds = getStoppedInstanceIds(rollingVerticalScaleResult);

                LOGGER.info("Vertical scaling the stopped instances: count={}, instanceIds=[{}]", stoppedInstanceIds.size(), stoppedInstanceIds);
                rollingVerticalScaleService.verticalScaleInstances(payload.getResourceId(), stoppedInstanceIds, stackVerticalScaleV4Request);
                StackDto stack = stackDtoService.getById(payload.getResourceId());
                List<CloudResource> cloudResources = getCloudResources(stack, stoppedInstanceIds);
                CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
                CloudStack cloudStack = cloudStackConverter.convert(stack);
                cloudStack = cloudStackConverter.updateWithVerticalScaleRequest(cloudStack, stackVerticalScaleV4Request);
                CloudContext cloudContext = context.getCloudContext();

                RollingVerticalScaleInstancesRequest request = new RollingVerticalScaleInstancesRequest(
                        payload.getResourceId(),
                        cloudContext,
                        cloudCredential,
                        cloudStack,
                        cloudResources,
                        stackVerticalScaleV4Request,
                        rollingVerticalScaleResult);
                sendEvent(context, request);
            }

            private List<String> getStoppedInstanceIds(RollingVerticalScaleResult result) {
                return result.getInstanceIds().stream()
                        .filter(i -> result.getStatus(i).getStatus().equals(RollingVerticalScaleStatus.STOPPED)).toList();
            }

            private List<CloudResource> getCloudResources(StackDto stack, List<String> stoppedInstanceIds) {
                return stack.getResources().stream()
                        .filter(i -> stoppedInstanceIds.contains(i.getInstanceId()))
                        .map(i -> resourceToCloudResourceConverter.convert(i)).toList();
            }
        };
    }

    @Bean(name = "ROLLING_VERTICALSCALE_START_INSTANCES_STATE")
    public Action<?, ?> startInstancesAction() {
        return new AbstractRollingVerticalScaleActions<>(RollingVerticalScaleInstancesResult.class) {

            @Override
            protected void doExecute(RollingVerticalScaleContext context, RollingVerticalScaleInstancesResult payload, Map<Object, Object> variables) {
                StackDtoDelegate stack = context.getStack();
                List<String> instancesToRestart = getInstancesToRestart(payload.getRollingVerticalScaleResult());

                LOGGER.info("Restarting instances after vertical scale: count={}, instanceIds=[{}]", instancesToRestart.size(), instancesToRestart);
                rollingVerticalScaleService.startInstances(payload.getResourceId(), instancesToRestart, context.getStackVerticalScaleV4Request().getGroup());
                List<InstanceMetadataView> instances = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(context.getStack().getId()).stream()
                        .filter(instanceMetaData -> context.getInstanceIds().contains(instanceMetaData.getInstanceId())).collect(Collectors.toList());

                List<CloudInstance> cloudInstances = instanceMetaDataToCloudInstanceConverter.convert(instances, stack.getStack());
                List<CloudResource> cloudResources = getCloudResources(context.getStack().getId());
                RollingVerticalScaleStartInstancesRequest request = new RollingVerticalScaleStartInstancesRequest(payload.getResourceId(),
                        context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances, payload.getRollingVerticalScaleResult());
                sendEvent(context, request);
            }

            private List<String> getInstancesToRestart(RollingVerticalScaleResult rollingVerticalScaleResult) {
                return rollingVerticalScaleResult.getInstanceIds();
            }

            private List<CloudResource> getCloudResources(Long stackId) {
                List<Resource> resources = (List<Resource>) resourceService.getAllByStackId(stackId);
                return resources.stream()
                        .map(r -> resourceToCloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
            }
        };
    }

    @Bean(name = "ROLLING_VERTICALSCALE_FINISHED_STATE")
    public Action<?, ?> verticalScaleFinishedAction() {
        return new AbstractRollingVerticalScaleActions<>(RollingVerticalScaleStartInstancesResult.class) {
            @Override
            protected void doExecute(RollingVerticalScaleContext context, RollingVerticalScaleStartInstancesResult payload,
                    Map<Object, Object> variables) throws Exception {
                LOGGER.info("ROLLING_VERTICALSCALE_FINISHED_STATE - finishing rolling vertical scale.");
                RollingVerticalScaleResult result = payload.getRollingVerticalScaleResult();
                List<String> successfulInstanceIds = result.getInstanceIds().stream()
                        .filter(i -> result.getStatus(i).getStatus().equals(RollingVerticalScaleStatus.SUCCESS)).toList();
                Map<String, String> failedInstancesWithErrorMessage = getFailedInstances(result);
                LOGGER.info("Rolling Vertical scale instances. Results: Successfully vertical scaled:[{}]. Failed to vertical scale:[{}]",
                        successfulInstanceIds, failedInstancesWithErrorMessage);

                if (!failedInstancesWithErrorMessage.isEmpty()) {
                    String errorMessage = String.format("Vertical scale instances failed for instances [%s] with errors [%s]",
                            failedInstancesWithErrorMessage.keySet(), failedInstancesWithErrorMessage.values());
                    LOGGER.error(errorMessage);
                    sendEvent(context, new StackFailureEvent(RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FAILURE_EVENT.event(),
                            payload.getResourceId(), new CloudbreakException(errorMessage)));
                } else {
                    sendEvent(context, RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FINALIZED_EVENT.event(), payload);
                }
            }

            private Map<String, String> getFailedInstances(RollingVerticalScaleResult result) {
                Map<String, String> failedInstances = new HashMap<>();
                for (String instanceId : result.getInstanceIds()) {
                    if (!result.getStatus(instanceId).getStatus().equals(RollingVerticalScaleStatus.SUCCESS)) {
                        String errorMessage = String.format("Failed to vertical scale instance %s during (%s) stages with errors: %s",
                                instanceId,
                                result.getStatus(instanceId).getStatus().getMessage(),
                                result.getStatus(instanceId).getMessage());
                        failedInstances.put(instanceId, errorMessage);
                    }
                }
                return failedInstances;
            }

        };
    }

    @Bean(name = "ROLLING_VERTICALSCALE_FAILED_STATE")
    public Action<?, ?> verticalScaleFailedAction() {
        return new AbstractStackFailureAction<RollingVerticalScaleState, RollingVerticalScaleEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Handling a failure from vertical scaling instances", payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

    abstract static class AbstractRollingVerticalScaleActions<P extends Payload>
            extends AbstractStackAction<RollingVerticalScaleState, RollingVerticalScaleEvent, RollingVerticalScaleContext, P> {

        static final String TARGET_INSTANCES = "TARGET_INSTANCES";

        static final String STACK_VERTICALSCALE_V4_REQUEST = "STACK_VERTICALSCALE_V4_REQUEST";

        static final String TARGET_INSTANCE_TYPE = "TARGET_INSTANCE_TYPE";

        static final String PREVIOUS_INSTANCE_TYPE = "PREVIOUS_INSTANCE_TYPE";

        static final String GROUP_BEING_SCALED = "GROUP_BEING_SCALED";

        @Inject
        private StackService stackService;

        @Inject
        private StackUtil stackUtil;

        protected AbstractRollingVerticalScaleActions(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected RollingVerticalScaleContext createFlowContext(FlowParameters flowParameters, StateContext<RollingVerticalScaleState,
                RollingVerticalScaleEvent> stateContext, P payload) {
            Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
            Long stackId = payload.getResourceId();
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            MDCBuilder.buildMdcContext(stack);
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            StackVerticalScaleV4Request stackVerticalScaleV4Request = (StackVerticalScaleV4Request) variables.get(STACK_VERTICALSCALE_V4_REQUEST);
            List<String> instanceIds = (List<String>) variables.get(TARGET_INSTANCES);
            String targetInstanceType = (String) variables.get(TARGET_INSTANCE_TYPE);

            CloudContext cloudContext = CloudContext.Builder.builder()
                    .withId(stack.getId())
                    .withName(stack.getName())
                    .withCrn(stack.getResourceCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withVariant(stack.getPlatformVariant())
                    .withLocation(location)
                    .withWorkspaceId(stack.getWorkspaceId())
                    .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                    .withTenantId(stack.getTenant().getId())
                    .build();
            CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());

            return new RollingVerticalScaleContext(flowParameters, stack, instanceIds,
                    stackVerticalScaleV4Request, cloudContext, cloudCredential, targetInstanceType);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<RollingVerticalScaleContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getResourceId(), ex);
        }
    }
}
