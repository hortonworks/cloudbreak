package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_CM_CONFIG_START;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_START;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_UNMOUNT_START;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_VALIDATION_START;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.DeleteVolumesTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesHandlerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesRequest;
import com.sequenceiq.flow.event.EventSelectorUtil;

@Configuration
public class DeleteVolumesActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteVolumesActions.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Bean(name = "DELETE_VOLUMES_VALIDATION_STATE")
    public Action<?, ?> deleteVolumesValidationAction() {
        return new AbstractClusterAction<>(DeleteVolumesTriggerEvent.class) {

            @Override
            protected void doExecute(ClusterViewContext context, DeleteVolumesTriggerEvent payload, Map<Object, Object> variables) {
                StackDeleteVolumesRequest stackDeleteVolumesRequest = payload.getStackDeleteVolumesRequest();
                flowMessageService.fireEventAndLog(stackDeleteVolumesRequest.getStackId(), Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_DELETE_VOLUMES_VALIDATION_START, stackDeleteVolumesRequest.getGroup(), stackDeleteVolumesRequest.getStackId().toString());
                String selector = EventSelectorUtil.selector(DeleteVolumesValidationRequest.class);
                DeleteVolumesValidationRequest deleteVolumesValidationRequest = DeleteVolumesValidationRequest.Builder.builder()
                        .withSelector(selector)
                        .withStackId(payload.getResourceId())
                        .withStackDeleteVolumesRequest(stackDeleteVolumesRequest)
                        .build();
                sendEvent(context, selector, deleteVolumesValidationRequest);
            }
        };
    }

    @Bean(name = "DELETE_VOLUMES_UNMOUNT_STATE")
    public Action<?, ?> deleteVolumesUnmountAction() {
        return new AbstractClusterAction<>(DeleteVolumesRequest.class) {

            @Override
            protected void doExecute(ClusterViewContext context, DeleteVolumesRequest payload, Map<Object, Object> variables) {
                StackDeleteVolumesRequest stackDeleteVolumesRequest = payload.getStackDeleteVolumesRequest();
                flowMessageService.fireEventAndLog(stackDeleteVolumesRequest.getStackId(), Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_DELETE_VOLUMES_UNMOUNT_START, stackDeleteVolumesRequest.getGroup(), stackDeleteVolumesRequest.getStackId().toString());
                DeleteVolumesUnmountEvent deleteVolumesUnmountEvent = new DeleteVolumesUnmountEvent(payload.getResourceId(),
                        stackDeleteVolumesRequest.getGroup(), payload.getResourcesToBeDeleted(), stackDeleteVolumesRequest, payload.getCloudPlatform(),
                        payload.getHostTemplateServiceComponents());
                sendEvent(context, deleteVolumesUnmountEvent);
            }
        };
    }

    @Bean(name = "DELETE_VOLUMES_STATE")
    public Action<?, ?> deleteVolumesAction() {
        return new AbstractClusterAction<>(DeleteVolumesUnmountFinishedEvent.class) {

            @Override
            protected void doExecute(ClusterViewContext context, DeleteVolumesUnmountFinishedEvent payload, Map<Object, Object> variables) {
                StackDeleteVolumesRequest stackDeleteVolumesRequest = payload.getStackDeleteVolumesRequest();
                flowMessageService.fireEventAndLog(stackDeleteVolumesRequest.getStackId(), Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_DELETE_VOLUMES_START, stackDeleteVolumesRequest.getGroup(), stackDeleteVolumesRequest.getStackId().toString());
                DeleteVolumesHandlerRequest deleteVolumesHandlerRequest = new DeleteVolumesHandlerRequest(payload.getResourcesToBeDeleted(),
                        payload.getStackDeleteVolumesRequest(), payload.getCloudPlatform(), payload.getHostTemplateServiceComponents());
                sendEvent(context, EventSelectorUtil.selector(DeleteVolumesHandlerRequest.class), deleteVolumesHandlerRequest);
            }
        };
    }

    @Bean(name = "DELETE_VOLUMES_CM_CONFIG_STATE")
    public Action<?, ?> deleteVolumesCMConfigAction() {
        return new AbstractClusterAction<>(DeleteVolumesFinishedEvent.class) {

            @Override
            protected void doExecute(ClusterViewContext context, DeleteVolumesFinishedEvent payload, Map<Object, Object> variables) {
                StackDeleteVolumesRequest stackDeleteVolumesRequest = payload.getStackDeleteVolumesRequest();
                flowMessageService.fireEventAndLog(stackDeleteVolumesRequest.getStackId(), Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_DELETE_VOLUMES_CM_CONFIG_START, stackDeleteVolumesRequest.getGroup(), stackDeleteVolumesRequest.getStackId().toString());
                DeleteVolumesCMConfigEvent deleteVolumesCMConfigEvent = new DeleteVolumesCMConfigEvent(payload.getResourceId(),
                        stackDeleteVolumesRequest.getGroup());
                sendEvent(context, deleteVolumesCMConfigEvent);
            }
        };
    }

    @Bean(name = "DELETE_VOLUMES_CM_CONFIG_FINISHED_STATE")
    public Action<?, ?> deleteVolumesCMConfigFinishedAction() {
        return new AbstractClusterAction<>(DeleteVolumesCMConfigFinishedEvent.class) {

            @Override
            protected void doExecute(ClusterViewContext context, DeleteVolumesCMConfigFinishedEvent payload, Map<Object, Object> variables) {
                flowMessageService.fireEventAndLog(payload.getResourceId(), Status.AVAILABLE.name(), CLUSTER_DELETE_VOLUMES_FINISHED,
                        payload.getRequestGroup(), payload.getResourceId().toString());
                DeleteVolumesFinalizedEvent deleteVolumesHandlerRequest = new DeleteVolumesFinalizedEvent(payload.getResourceId());
                sendEvent(context, deleteVolumesHandlerRequest);
            }
        };
    }

    @Bean(name = "DELETE_VOLUMES_FAILED_STATE")
    public Action<?, ?> deleteVolumesFailedAction() {
        return new AbstractStackFailureAction<DeleteVolumesState, DeleteVolumesEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Exception during vertical scaling!: {}", payload.getException().getMessage());
                flowMessageService.fireEventAndLog(payload.getResourceId(),
                        DELETE_FAILED.name(),
                        CLUSTER_DELETE_VOLUMES_FAILED,
                        payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(CoreVerticalScaleEvent.FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
