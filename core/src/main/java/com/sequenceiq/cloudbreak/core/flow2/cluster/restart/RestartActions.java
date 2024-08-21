package com.sequenceiq.cloudbreak.core.flow2.cluster.restart;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

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

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.RestartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.RestartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.event.RestartInstancesEvent;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class RestartActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartActions.class);

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private RestartService restartService;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Bean(name = "RESTART_STATE")
    public Action<?, ?> restartAction() {
        return new AbstractRestartActions<>(RestartInstancesEvent.class) {

            @Override
            protected void prepareExecution(RestartInstancesEvent payload, Map<Object, Object> variables) {
                variables.put(HOSTS_TO_RESTART, payload.getInstanceIds());
            }

            @Override
            protected void doExecute(RestartContext context, RestartInstancesEvent payload, Map<Object, Object> variables) {
                restartService.startInstanceRestart(context);
                LOGGER.info("toRestartInstances: count={}, InstanceIds=[{}]", context.getInstanceIds().size(), context.getInstanceIds());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RestartContext context) {
                StackDtoDelegate stack = context.getStack();

                List<InstanceMetadataView> instances = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(context.getStack().getId()).stream()
                        .filter(instanceMetaData -> context.getInstanceIds().contains(instanceMetaData.getInstanceId())).collect(Collectors.toList());

                List<CloudInstance> cloudInstances = instanceMetaDataToCloudInstanceConverter.convert(instances, stack.getStack());
                List<CloudResource> cloudResources = getCloudResources(context.getStack().getId());
                return new RestartInstancesRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudResources, cloudInstances);
            }

            private List<CloudResource> getCloudResources(Long stackId) {
                List<Resource> resources = (List<Resource>) resourceService.getAllByStackId(stackId);
                return resources.stream()
                        .map(r -> resourceToCloudResourceConverter.convert(r))
                        .collect(Collectors.toList());
            }
        };
    }

    @Bean(name = "RESTART_FINISHED_STATE")
    public Action<?, ?> restartFinishedAction() {
        return new AbstractRestartActions<>(RestartInstancesResult.class) {

            @Override
            protected void doExecute(RestartContext context, RestartInstancesResult payload,
                    Map<Object, Object> variables) throws Exception {
                List<String> successOnRestartInstanceIds = payload.getResults().getResults()
                        .stream().map(CloudVmInstanceStatus::getCloudInstance).collect(Collectors.toList())
                        .stream().map(CloudInstance::getInstanceId).collect(Collectors.toList());

                List<String> failedToRestartInstanceIds = payload.getInstanceIds().stream()
                        .filter(i -> !successOnRestartInstanceIds.contains(i)).collect(Collectors.toList());

                restartService.instanceRestartFinished(context, failedToRestartInstanceIds, successOnRestartInstanceIds);
                LOGGER.info("RESTART_FINISHED_STATE - finishing instance restart via stop and start operations.");
                sendEvent(context, RestartEvent.RESTART_FINALIZED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "RESTART_FAILED_STATE")
    public Action<?, ?> restartFailureAction() {
        return new AbstractRestartActions<>(StackFailureEvent.class) {
            @Override
            protected void doExecute(RestartContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                restartService.allInstanceRestartFailed(context, payload.getException());
                String message = String.format("Restarting failed for %s.", context.getInstanceIds().stream().collect(Collectors.joining(",")));
                LOGGER.error(message);
                sendEvent(context, new StackEvent(RestartEvent.RESTART_FAIL_HANDLED_EVENT.event(), context.getStack().getId()));
            }
        };
    }

    @VisibleForTesting
    abstract static class AbstractRestartActions<P extends Payload> extends AbstractStackAction<RestartState, RestartEvent, RestartContext, P> {

        static final String HOSTS_TO_RESTART = "HOSTS_TO_RESTART";

        @Inject
        private StackService stackService;

        @Inject
        private StackUtil stackUtil;

        AbstractRestartActions(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<RestartContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getResourceId(), ex);
        }

        @Override
        protected RestartContext createFlowContext(FlowParameters flowParameters, StateContext<RestartState, RestartEvent> stateContext, P payload) {
            Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
            Long stackId = payload.getResourceId();
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            MDCBuilder.buildMdcContext(stack);
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
            List<String> instanceIds = (List<String>) variables.get(HOSTS_TO_RESTART);


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

            return new RestartContext(flowParameters, stack, instanceIds, cloudContext, cloudCredential);
        }
    }
}
