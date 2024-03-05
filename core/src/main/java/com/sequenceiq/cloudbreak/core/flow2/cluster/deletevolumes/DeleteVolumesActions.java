package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_CM_CONFIG_START;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_FINISHED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_START;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_UNMOUNT_START;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DELETE_VOLUMES_VALIDATION_START;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.event.DeleteVolumesTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFinishedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesHandlerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesRequest;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.flow.event.EventSelectorUtil;

@Configuration
public class DeleteVolumesActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteVolumesActions.class);

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;

    @Bean(name = "DELETE_VOLUMES_VALIDATION_STATE")
    public Action<?, ?> deleteVolumesValidationAction() {
        return new AbstractClusterAction<>(DeleteVolumesTriggerEvent.class) {

            @Override
            protected void doExecute(ClusterViewContext context, DeleteVolumesTriggerEvent payload, Map<Object, Object> variables) {
                StackDeleteVolumesRequest stackDeleteVolumesRequest = payload.getStackDeleteVolumesRequest();
                Long stackId = stackDeleteVolumesRequest.getStackId();
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.DELETE_BLOCK_STORAGES, String.format("Validating delete volumes  request " +
                                "on the host group %s ", stackDeleteVolumesRequest.getGroup()));
                flowMessageService.fireEventAndLog(stackDeleteVolumesRequest.getStackId(), Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_DELETE_VOLUMES_VALIDATION_START, stackDeleteVolumesRequest.getGroup());
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
                stackUpdater.updateStackStatus(stackDeleteVolumesRequest.getStackId(), DetailedStackStatus.DELETE_BLOCK_STORAGES,
                        String.format("Unmounting Volumes on the host group %s ", stackDeleteVolumesRequest.getGroup()));
                flowMessageService.fireEventAndLog(stackDeleteVolumesRequest.getStackId(), Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_DELETE_VOLUMES_UNMOUNT_START, stackDeleteVolumesRequest.getGroup());
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
                stackUpdater.updateStackStatus(stackDeleteVolumesRequest.getStackId(), DetailedStackStatus.DELETE_BLOCK_STORAGES,
                        String.format("Deleting block storages on the host group %s ", stackDeleteVolumesRequest.getGroup()));
                flowMessageService.fireEventAndLog(stackDeleteVolumesRequest.getStackId(), Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_DELETE_VOLUMES_START, stackDeleteVolumesRequest.getGroup());
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
                stackUpdater.updateStackStatus(stackDeleteVolumesRequest.getStackId(), DetailedStackStatus.DELETE_BLOCK_STORAGES,
                        String.format("Configuring CM after deleting block storages on the host group %s ", stackDeleteVolumesRequest.getGroup()));
                flowMessageService.fireEventAndLog(stackDeleteVolumesRequest.getStackId(), Status.UPDATE_IN_PROGRESS.name(),
                        CLUSTER_DELETE_VOLUMES_CM_CONFIG_START, stackDeleteVolumesRequest.getGroup());
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
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE,
                        String.format("Finished Deleting Volumes and CM config update on the host group %s", payload.getRequestGroup()));
                flowMessageService.fireEventAndLog(stackId, Status.AVAILABLE.name(), CLUSTER_DELETE_VOLUMES_FINISHED,
                        payload.getRequestGroup());
                DeleteVolumesFinalizedEvent deleteVolumesHandlerRequest = new DeleteVolumesFinalizedEvent(stackId);
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
                Long stackId = payload.getResourceId();
                stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Failed Deleting Volumes");
                flowMessageService.fireEventAndLog(stackId,
                        DELETE_FAILED.name(),
                        CLUSTER_DELETE_VOLUMES_FAILED,
                        payload.getException().getMessage());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(DELETE_VOLUMES_FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }
}
