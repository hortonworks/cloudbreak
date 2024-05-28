package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.event.StackSyncTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.StackParameters;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;
import com.sequenceiq.cloudbreak.service.stack.flow.SyncConfig;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class StackSyncActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackSyncActions.class);

    @Inject
    private InstanceMetaDataToCloudInstanceConverter cloudInstanceConverter;

    @Inject
    private StackSyncService stackSyncService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackDtoService stackDtoService;

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
                StackView stack = context.getStack();
                List<CloudInstance> cloudInstances = cloudInstanceConverter.convert(context.getInstanceMetaData(), stack);
                List<StackParameters> stackParameters = stackDtoService.getStackParameters(stack.getId());
                cloudInstances.forEach(instance -> stackParameters.forEach(p -> instance.putParameter(p.getKey(), p.getValue())));
                return new GetInstancesStateRequest<>(context.getCloudContext(), context.getCloudCredential(), cloudInstances);
            }
        };
    }

    @Bean(name = "SYNC_FINISHED_STATE")
    public Action<?, ?> stackSyncFinishedAction() {
        return new AbstractStackSyncAction<>(GetInstancesStateResult.class) {
            @Override
            protected void doExecute(StackSyncContext context, GetInstancesStateResult payload, Map<Object, Object> variables) {
                SyncConfig syncConfig = new SyncConfig(context.isStatusUpdateEnabled(), true, false);
                stackSyncService.updateInstances(context.getStack(), context.getInstanceMetaData(), payload.getStatuses(), syncConfig);
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
                flowMessageService.fireEventAndLog(context.getStackId(), UPDATE_FAILED.name(), STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE,
                        payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StackSyncEvent.SYNC_FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

    private abstract static class AbstractStackSyncAction<P extends Payload> extends AbstractStackAction<StackSyncState, StackSyncEvent, StackSyncContext, P> {
        static final String STATUS_UPDATE_ENABLED = "STATUS_UPDATE_ENABLED";

        @Inject
        private StackDtoService stackDtoService;

        @Inject
        private InstanceMetaDataService instanceMetaDataService;

        @Inject
        private StackUtil stackUtil;

        protected AbstractStackSyncAction(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected StackSyncContext createFlowContext(FlowParameters flowParameters, StateContext<StackSyncState, StackSyncEvent> stateContext,
            P payload) {
            Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
            Long stackId = payload.getResourceId();
            StackView stack = stackDtoService.getStackViewById(stackId);
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
                    .withTenantId(stack.getTenantId())
                    .build();
            CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
            List<InstanceMetadataView> allAvailableInstanceMetadata = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stackId);
            return new StackSyncContext(flowParameters, stack, allAvailableInstanceMetadata, cloudContext, cloudCredential,
                    isStatusUpdateEnabled(variables));
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<StackSyncContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getResourceId(), ex);
        }

        private Boolean isStatusUpdateEnabled(Map<Object, Object> variables) {
            return (Boolean) variables.get(STATUS_UPDATE_ENABLED);
        }
    }
}
