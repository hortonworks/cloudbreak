package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_START_IGNORED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_STOP_IGNORED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_STOP_REQUESTED;
import static java.lang.String.format;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterFlowService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.workspace.authorization.PermissionCheckingUtils;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class StackFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackFlowService.class);

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackService stackService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterFlowService clusterFlowService;

    @Inject
    private UpdateNodeCountValidator updateNodeCountValidator;

    public void removeInstance(Stack stack, Long workspaceId, String instanceId, boolean forced, User user) {
        InstanceMetaData metaData = updateNodeCountValidator.validateInstanceForDownscale(instanceId, stack, workspaceId, user);
        flowManager.triggerStackRemoveInstance(stack.getId(), metaData.getInstanceGroupName(), metaData.getPrivateId(), forced);
    }

    public void removeInstances(Stack stack, Long workspaceId, Collection<String> instanceIds, boolean forced, User user) {
        Map<String, Set<Long>> instanceIdsByHostgroupMap = new HashMap<>();
        for (String instanceId : instanceIds) {
            InstanceMetaData metaData = updateNodeCountValidator.validateInstanceForDownscale(instanceId, stack, workspaceId, user);
            instanceIdsByHostgroupMap.computeIfAbsent(metaData.getInstanceGroupName(), s -> new LinkedHashSet<>()).add(metaData.getPrivateId());
        }
        flowManager.triggerStackRemoveInstances(stack.getId(), instanceIdsByHostgroupMap, forced);
    }

    public void updateImage(Long stackId, Long workspaceId, String imageId, String imageCatalogName, String imageCatalogUrl, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        flowManager.triggerStackImageUpdate(stackId, imageId, imageCatalogName, imageCatalogUrl);
    }

    public void updateStatus(Long stackId, StatusRequest status, boolean updateCluster, User user) {
        Stack stack = stackService.getByIdWithLists(stackId);
        Cluster cluster = null;
        if (stack.getCluster() != null) {
            cluster = clusterService.findOneWithLists(stack.getCluster().getId()).orElse(null);
        }
        switch (status) {
            case SYNC:
                sync(stack, false, user);
                break;
            case FULL_SYNC:
                sync(stack, true, user);
                break;
            case REPAIR_FAILED_NODES:
                repairFailedNodes(stack, user);
                break;
            case STOPPED:
                stop(stack, cluster, updateCluster, user);
                break;
            case STARTED:
                start(stack, cluster, updateCluster, user);
                break;
            default:
                throw new BadRequestException("Cannot update the status of stack because status request not valid.");
        }
    }

    private void triggerStackStopIfNeeded(Stack stack, Cluster cluster, boolean updateCluster, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        if (!isStopNeeded(stack)) {
            return;
        }
        if (cluster != null && !cluster.isStopped() && !stack.isStopFailed()) {
            if (!updateCluster) {
                throw new BadRequestException(format("Cannot update the status of stack '%s' to STOPPED, because the cluster's state is %s.",
                        stack.getName(), cluster.getStatus().name()));
            } else if (cluster.isClusterReadyForStop() || cluster.isStopFailed()) {
                setStackStatusToStopRequested(stack);
                clusterFlowService.updateStatus(stack.getId(), StatusRequest.STOPPED);
            } else {
                throw new BadRequestException(format("Cannot update the status of cluster '%s' to STOPPED, because the cluster's state is %s.",
                        cluster.getName(), cluster.getStatus()));
            }
        } else {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOP_REQUESTED);
            flowManager.triggerStackStop(stack.getId());
        }
    }

    private void repairFailedNodes(Stack stack, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        LOGGER.debug("Received request to replace failed nodes: " + stack.getId());
        flowManager.triggerManualRepairFlow(stack.getId());
    }

    private void sync(Stack stack, boolean full, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        // TODO: is it a good condition?
        if (!stack.isDeleteInProgress() && !stack.isStackInDeletionPhase() && !stack.isModificationInProgress()) {
            if (full) {
                flowManager.triggerFullSync(stack.getId());
            } else {
                flowManager.triggerStackSync(stack.getId());
            }
        } else {
            LOGGER.debug("Stack could not be synchronized in {} state!", stack.getStatus());
        }
    }

    private void stop(Stack stack, Cluster cluster, boolean updateCluster, User user) {
        if (cluster != null && cluster.isStopInProgress()) {
            setStackStatusToStopRequested(stack);
        } else {
            triggerStackStopIfNeeded(stack, cluster, updateCluster, user);
        }
    }

    public void updateNodeCount(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, boolean withClusterEvent, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        try {
            transactionService.required(() -> {
                Stack stackWithLists = stackService.getByIdWithLists(stack.getId());
                updateNodeCountValidator.validateStackStatus(stackWithLists);
                updateNodeCountValidator.validateInstanceGroup(stackWithLists, instanceGroupAdjustmentJson.getInstanceGroup());
                updateNodeCountValidator.validateScalingAdjustment(instanceGroupAdjustmentJson, stackWithLists);
                updateNodeCountValidator.validateInstanceStatuses(stackWithLists, instanceGroupAdjustmentJson);
                if (withClusterEvent) {
                    updateNodeCountValidator.validateClusterStatus(stackWithLists);
                    updateNodeCountValidator.validateHostGroupAdjustment(instanceGroupAdjustmentJson, stackWithLists,
                            instanceGroupAdjustmentJson.getScalingAdjustment());
                    updateNodeCountValidator.validataHostMetadataStatuses(stackWithLists, instanceGroupAdjustmentJson);
                }
                if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
                    stackUpdater.updateStackStatus(stackWithLists.getId(), DetailedStackStatus.UPSCALE_REQUESTED);
                    flowManager.triggerStackUpscale(stackWithLists.getId(), instanceGroupAdjustmentJson, withClusterEvent);
                } else {
                    stackUpdater.updateStackStatus(stackWithLists.getId(), DetailedStackStatus.DOWNSCALE_REQUESTED);
                    flowManager.triggerStackDownscale(stackWithLists.getId(), instanceGroupAdjustmentJson);
                }
                return null;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            if (e.getCause() instanceof BadRequestException) {
                throw e.getCause();
            }
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    @VisibleForTesting
    void start(Stack stack, Cluster cluster, boolean updateCluster, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResource.DATAHUB, ResourceAction.WRITE, user.getUserCrn());
        if (stack.isAvailable() && (cluster == null || cluster.isAvailable())) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), STACK_START_IGNORED);
        } else if (isStackStartable(stack) || isClusterStartable(cluster)) {
            Stack startStack = stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.START_REQUESTED);
            flowManager.triggerStackStart(stack.getId());
            if (updateCluster && cluster != null) {
                clusterFlowService.updateStatus(startStack, StatusRequest.STARTED);
            }
        } else {
            throw new BadRequestException(format("Cannot update the status of stack '%s' to STARTED, because it is in %s state",
                    stack.getName(), stack.getStatus().name()));
        }
    }

    public void renewCertificate(String stackName) {
        Workspace workspace = workspaceService.getForCurrentUser();
        Stack stack = stackService.getByNameInWorkspace(stackName, workspace.getId());
        flowManager.triggerClusterCertificationRenewal(stack.getId());
    }

    private boolean isStopNeeded(Stack stack) {
        boolean result = true;
        StopRestrictionReason reason = stack.isInfrastructureStoppable();
        if (stack.isStopped()) {
            eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(), STACK_STOP_IGNORED);
            result = false;
        } else if (reason != StopRestrictionReason.NONE) {
            throw new BadRequestException(
                    format("Cannot stop a stack '%s'. Reason: %s", stack.getName(), reason.getReason()));
        } else if (!stack.isAvailable() && !stack.isStopFailed()) {
            throw new BadRequestException(
                    format("Cannot update the status of stack '%s' to STOPPED, because it isn't in AVAILABLE state.", stack.getName()));
        }
        return result;
    }

    private boolean isStackStartable(Stack stack) {
        return stack.isStopped() || stack.isStartFailed();
    }

    private boolean isClusterStartable(Cluster cluster) {
        return cluster != null && (cluster.isStopped() || cluster.isStartFailed());
    }

    private void setStackStatusToStopRequested(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOP_REQUESTED, "Stopping of cluster infrastructure has been requested.");
        eventService.fireCloudbreakEvent(stack.getId(), STOP_REQUESTED.name(), STACK_STOP_REQUESTED);
    }
}
