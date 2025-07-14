package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_CM_CONFIGURATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_ORCHESTRATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_VALIDATE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ATTACH_VOLUMES_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FINALIZED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.ADDING_VOLUMES_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_ADDED_VOLUMES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_ADDING_VOLUMES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_ATTACHING_VOLUMES;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CONFIG_CHANGE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MOUNTING_VOLUMES;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.converter.AddVolumesRequestToAddVolumesValidationFinishedEventConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesCMConfigFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesCMConfigHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFinalizedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.PayloadConverter;

@Configuration
public class AddVolumesActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesActions.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "ADD_VOLUMES_VALIDATE_STATE")
    public Action<?, ?> addVolumesValidateAction() {
        return new AbstractAddVolumesAction<>(AddVolumesRequest.class) {
            @Override
            protected void doExecute(CommonContext ctx, AddVolumesRequest payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                String instanceGroup = payload.getInstanceGroup();
                AddVolumesValidateEvent handlerRequest = new AddVolumesValidateEvent(stackId, payload.getNumberOfDisks(), payload.getType(),
                        payload.getSize(), payload.getCloudVolumeUsageType(), payload.getInstanceGroup());
                sendEvent(ctx, ADD_VOLUMES_VALIDATE_HANDLER_EVENT.event(), handlerRequest);
            }
        };
    }

    @Bean(name = "ADD_VOLUMES_STATE")
    public Action<?, ?> addVolumesAction() {
        return new AbstractAddVolumesAction<>(AddVolumesValidationFinishedEvent.class) {
            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<AddVolumesValidationFinishedEvent>> payloadConverters) {
                payloadConverters.add(new AddVolumesRequestToAddVolumesValidationFinishedEventConverter());
            }

            @Override
            protected void doExecute(CommonContext ctx, AddVolumesValidationFinishedEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                LOGGER.debug("Starting to add volumes for stack: {}", stackId);
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.ADD_ADDITIONAL_BLOCK_STORAGES,
                        "Adding additional block storages in stack " + stackId);
                flowMessageService.fireEventAndLog(stackId,
                        CLUSTER_ADDING_VOLUMES.name(),
                        CLUSTER_ADDING_VOLUMES,
                        payload.getNumberOfDisks().toString(),
                        payload.getInstanceGroup(),
                        String.valueOf(payload.getResourceId()));
                AddVolumesHandlerEvent handlerRequest = new AddVolumesHandlerEvent(stackId, payload.getNumberOfDisks(), payload.getType(),
                        payload.getSize(), payload.getCloudVolumeUsageType(), payload.getInstanceGroup());
                sendEvent(ctx, ADD_VOLUMES_HANDLER_EVENT.event(), handlerRequest);
            }
        };
    }

    @Bean(name = "ATTACH_VOLUMES_STATE")
    public Action<?, ?> attachVolumesAction() {
        return new AbstractAddVolumesAction<>(AddVolumesFinishedEvent.class) {
            @Override
            protected void doExecute(CommonContext ctx, AddVolumesFinishedEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting to add volumes for stack: {}", payload.getResourceId());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        CLUSTER_ATTACHING_VOLUMES.name(),
                        CLUSTER_ATTACHING_VOLUMES,
                        payload.getNumberOfDisks().toString(),
                        payload.getInstanceGroup(),
                        String.valueOf(payload.getResourceId()));
                AttachVolumesHandlerEvent handlerRequest = new AttachVolumesHandlerEvent(payload.getResourceId(), payload.getNumberOfDisks(), payload.getType(),
                        payload.getSize(), payload.getCloudVolumeUsageType(), payload.getInstanceGroup());
                sendEvent(ctx, ATTACH_VOLUMES_HANDLER_EVENT.event(), handlerRequest);
            }
        };
    }

    @Bean(name = "ADD_VOLUMES_ORCHESTRATION_STATE")
    public Action<?, ?> addVolumesOrchestrationAction() {
        return new AbstractAddVolumesAction<>(AttachVolumesFinishedEvent.class) {
            @Override
            protected void doExecute(CommonContext ctx, AttachVolumesFinishedEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting to mount added volumes for stack: {}", payload.getResourceId());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        CLUSTER_MOUNTING_VOLUMES.name(),
                        CLUSTER_MOUNTING_VOLUMES,
                        payload.getNumberOfDisks().toString(),
                        payload.getInstanceGroup(),
                        String.valueOf(payload.getResourceId()));
                AddVolumesOrchestrationHandlerEvent handlerRequest = new AddVolumesOrchestrationHandlerEvent(payload.getResourceId(),
                        payload.getNumberOfDisks(), payload.getType(), payload.getSize(), payload.getCloudVolumeUsageType(), payload.getInstanceGroup());
                sendEvent(ctx, ADD_VOLUMES_ORCHESTRATION_HANDLER_EVENT.event(), handlerRequest);
            }
        };
    }

    @Bean(name = "ADD_VOLUMES_CM_CONFIGURATION_STATE")
    public Action<?, ?> addVolumesCMConfigAction() {
        return new AbstractAddVolumesAction<>(AddVolumesOrchestrationFinishedEvent.class) {
            @Override
            protected void doExecute(CommonContext ctx, AddVolumesOrchestrationFinishedEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Starting to configure volumes in CM for stack: {}", payload.getResourceId());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        CLUSTER_CM_CONFIG_CHANGE.name(),
                        CLUSTER_CM_CONFIG_CHANGE,
                        payload.getInstanceGroup(),
                        String.valueOf(payload.getResourceId()));
                AddVolumesCMConfigHandlerEvent handlerRequest = new AddVolumesCMConfigHandlerEvent(payload.getResourceId(), payload.getInstanceGroup(),
                        payload.getNumberOfDisks(), payload.getType(), payload.getSize(), payload.getCloudVolumeUsageType());
                sendEvent(ctx, ADD_VOLUMES_CM_CONFIGURATION_HANDLER_EVENT.event(), handlerRequest);
            }
        };
    }

    @Bean(name = "ADD_VOLUMES_FINISHED_STATE")
    public Action<?, ?> addVolumesFinishedAction() {
        return new AbstractAddVolumesAction<>(AddVolumesCMConfigFinishedEvent.class) {
            @Override
            protected void doExecute(CommonContext ctx, AddVolumesCMConfigFinishedEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                LOGGER.debug("Adding volumes for stack {} successfully done!", stackId);
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE,
                        "Finished adding additional block storages in stack " + stackId);
                flowMessageService.fireEventAndLog(stackId,
                        AVAILABLE.name(),
                        CLUSTER_ADDED_VOLUMES,
                        payload.getNumberOfDisks().toString(),
                        payload.getInstanceGroup(),
                        String.valueOf(stackId));
                AddVolumesFinalizedEvent finalizedEvent = new AddVolumesFinalizedEvent(stackId);
                sendEvent(ctx, FINALIZED_EVENT.event(), finalizedEvent);
            }
        };
    }

    @Bean(name = "ADD_VOLUMES_FAILED_STATE")
    public Action<?, ?> addVolumesFailedAction() {
        return new AbstractAddVolumesAction<>(AddVolumesFailedEvent.class) {
            @Override
            protected void doExecute(CommonContext context, AddVolumesFailedEvent payload, Map<Object, Object> variables) {
                Long stackId = payload.getResourceId();
                String exceptionMessage = payload.getException().getMessage();
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE,
                        format("Failed to add additional block storages in stack %s. Exception: %s", stackId, exceptionMessage));
                LOGGER.info("Exception during vertical scaling!: {}", exceptionMessage);
                flowMessageService.fireEventAndLog(stackId,
                        ADDING_VOLUMES_FAILED.name(),
                        ADDING_VOLUMES_FAILED,
                        exceptionMessage);
                sendEvent(context, ADD_VOLUMES_FAILURE_HANDLED_EVENT.selector(), payload);
            }
        };
    }
}