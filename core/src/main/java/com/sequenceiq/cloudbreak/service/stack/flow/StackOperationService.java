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
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.datalake.DataLakeStatusCheckerService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.spot.SpotInstanceUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.NotAllowedStatusUpdate;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class StackOperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackOperationService.class);

    @Inject
    private ReactorFlowManager flowManager;

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
    private ClusterOperationService clusterOperationService;

    @Inject
    private UpdateNodeCountValidator updateNodeCountValidator;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private DataLakeStatusCheckerService dataLakeStatusCheckerService;

    @Inject
    private SpotInstanceUsageCondition spotInstanceUsageCondition;

    @Inject
    private StackStopRestrictionService stackStopRestrictionService;

    public FlowIdentifier removeInstance(Stack stack, Long workspaceId, String instanceId, boolean forced, User user) {
        InstanceMetaData metaData = updateNodeCountValidator.validateInstanceForDownscale(instanceId, stack);
        return flowManager.triggerStackRemoveInstance(stack.getId(), metaData.getInstanceGroupName(), metaData.getPrivateId(), forced);
    }

    public FlowIdentifier removeInstances(Stack stack, Long workspaceId, Collection<String> instanceIds, boolean forced, User user) {
        Map<String, Set<Long>> instanceIdsByHostgroupMap = new HashMap<>();
        for (String instanceId : instanceIds) {
            InstanceMetaData metaData = updateNodeCountValidator.validateInstanceForDownscale(instanceId, stack);
            instanceIdsByHostgroupMap.computeIfAbsent(metaData.getInstanceGroupName(), s -> new LinkedHashSet<>()).add(metaData.getPrivateId());
        }
        return flowManager.triggerStackRemoveInstances(stack.getId(), instanceIdsByHostgroupMap, forced);
    }

    public FlowIdentifier updateImage(ImageChangeDto imageChangeDto) {
        return flowManager.triggerStackImageUpdate(imageChangeDto);
    }

    public FlowIdentifier updateStatus(Long stackId, StatusRequest status, boolean updateCluster, User user) {
        Stack stack = stackService.getByIdWithLists(stackId);
        Cluster cluster = null;
        if (stack.getCluster() != null) {
            cluster = clusterService.findOneWithLists(stack.getCluster().getId()).orElse(null);
        }
        switch (status) {
            case SYNC:
                return sync(stack, false, user);
            case FULL_SYNC:
                return sync(stack, true, user);
            case REPAIR_FAILED_NODES:
                return repairFailedNodes(stack, user);
            case STOPPED:
                return stop(stack, cluster, updateCluster, user);
            case STARTED:
                return start(stack, cluster, updateCluster);
            default:
                throw new BadRequestException("Cannot update the status of stack because status request not valid.");
        }
    }

    @VisibleForTesting
    FlowIdentifier triggerStackStopIfNeeded(Stack stack, Cluster cluster, boolean updateCluster) {
        if (!isStopNeeded(stack)) {
            return FlowIdentifier.notTriggered();
        }
        if (spotInstanceUsageCondition.isStackRunsOnSpotInstances(stack)) {
            throw new BadRequestException(format("Cannot update the status of stack '%s' to STOPPED, because it runs on spot instances", stack.getName()));
        }
        environmentService.checkEnvironmentStatus(stack, EnvironmentStatus.stoppable());
        if (cluster != null && !cluster.isStopped() && !stack.isStopFailed()) {
            if (!updateCluster) {
                throw NotAllowedStatusUpdate
                        .stack(stack)
                        .to(STOPPED)
                        .badRequest();
            } else if (cluster.isClusterReadyForStop() || cluster.isStopFailed()) {
                setStackStatusToStopRequested(stack);
                return clusterOperationService.updateStatus(stack.getId(), StatusRequest.STOPPED);
            } else {
                throw NotAllowedStatusUpdate
                        .cluster(stack)
                        .to(STOPPED)
                        .badRequest();
            }
        } else {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOP_REQUESTED);
            return flowManager.triggerStackStop(stack.getId());
        }
    }

    private FlowIdentifier repairFailedNodes(Stack stack, User user) {
        LOGGER.debug("Received request to replace failed nodes: {}", stack.getId());
        return flowManager.triggerManualRepairFlow(stack.getId());
    }

    private FlowIdentifier sync(Stack stack, boolean full, User user) {
        // TODO: is it a good condition?
        if (!stack.isDeleteInProgress() && !stack.isStackInDeletionPhase() && !stack.isModificationInProgress()) {
            if (full) {
                return flowManager.triggerFullSync(stack.getId());
            } else {
                return flowManager.triggerStackSync(stack.getId());
            }
        } else {
            LOGGER.debug("Stack could not be synchronized in {} state!", stack.getStatus());
            return FlowIdentifier.notTriggered();
        }
    }

    private FlowIdentifier stop(Stack stack, Cluster cluster, boolean updateCluster, User user) {
        if (cluster != null && cluster.isStopInProgress()) {
            setStackStatusToStopRequested(stack);
            return FlowIdentifier.notTriggered();
        } else {
            return triggerStackStopIfNeeded(stack, cluster, updateCluster);
        }
    }

    public FlowIdentifier updateNodeCount(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, boolean withClusterEvent) {
        environmentService.checkEnvironmentStatus(stack, EnvironmentStatus.upscalable());
        try {
            return transactionService.required(() -> {
                Stack stackWithLists = stackService.getByIdWithLists(stack.getId());
                updateNodeCountValidator.validateServiceRoles(stackWithLists, instanceGroupAdjustmentJson);
                updateNodeCountValidator.validateStackStatus(stackWithLists);
                updateNodeCountValidator.validateInstanceGroup(stackWithLists, instanceGroupAdjustmentJson.getInstanceGroup());
                updateNodeCountValidator.validateScalabilityOfInstanceGroup(stackWithLists, instanceGroupAdjustmentJson);
                updateNodeCountValidator.validateScalingAdjustment(instanceGroupAdjustmentJson, stackWithLists);
                updateNodeCountValidator.validateInstanceStatuses(stackWithLists, instanceGroupAdjustmentJson);
                if (withClusterEvent) {
                    updateNodeCountValidator.validateClusterStatus(stackWithLists);
                    updateNodeCountValidator.validateHostGroupAdjustment(
                            instanceGroupAdjustmentJson,
                            stackWithLists,
                            instanceGroupAdjustmentJson.getScalingAdjustment());
                    updateNodeCountValidator.validataHostMetadataStatuses(stackWithLists, instanceGroupAdjustmentJson);
                }
                if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
                    stackUpdater.updateStackStatus(stackWithLists.getId(), DetailedStackStatus.UPSCALE_REQUESTED);
                    return flowManager.triggerStackUpscale(
                            stackWithLists.getId(),
                            instanceGroupAdjustmentJson,
                            withClusterEvent);
                } else {
                    stackUpdater.updateStackStatus(stackWithLists.getId(), DetailedStackStatus.DOWNSCALE_REQUESTED);
                    return flowManager.triggerStackDownscale(stackWithLists.getId(), instanceGroupAdjustmentJson);
                }
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof BadRequestException) {
                throw e.getCause();
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @VisibleForTesting
    FlowIdentifier start(Stack stack, Cluster cluster, boolean updateCluster) {
        FlowIdentifier flowIdentifier = FlowIdentifier.notTriggered();
        environmentService.checkEnvironmentStatus(stack, EnvironmentStatus.startable());
        dataLakeStatusCheckerService.validateRunningState(stack);
        if (stack.isAvailable() && (cluster == null || cluster.isAvailable())) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), STACK_START_IGNORED);
        } else if (isStackStartable(stack) || isClusterStartable(cluster)) {
            Stack startStack = stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.START_REQUESTED);
            flowIdentifier = flowManager.triggerStackStart(stack.getId());
            if (updateCluster && cluster != null) {
                clusterOperationService.updateStatus(startStack, StatusRequest.STARTED);
            }
        } else {
            throw NotAllowedStatusUpdate
                    .stack(stack)
                    .to(DetailedStackStatus.START_REQUESTED)
                    .badRequest();
        }
        return flowIdentifier;
    }

    public FlowIdentifier renewCertificate(String stackName) {
        Workspace workspace = workspaceService.getForCurrentUser();
        Stack stack = stackService.getByNameInWorkspace(stackName, workspace.getId());
        return renewCertificate(stack);
    }

    public FlowIdentifier renewCertificate(Stack stack) {
        return flowManager.triggerClusterCertificationRenewal(stack.getId());
    }

    private boolean isStopNeeded(Stack stack) {
        boolean result = true;
        StopRestrictionReason reason = stackStopRestrictionService.isInfrastructureStoppable(stack.getCloudPlatform(), stack.getInstanceGroups());
        if (stack.isStopped()) {
            eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(), STACK_STOP_IGNORED);
            result = false;
        } else if (reason != StopRestrictionReason.NONE) {
            throw new BadRequestException(
                    format("Cannot stop a stack '%s'. Reason: %s", stack.getName(), reason.getReason()));
        } else if (!stack.isAvailable() && !stack.isStopFailed()) {
            throw NotAllowedStatusUpdate
                    .stack(stack)
                    .to(STOPPED)
                    .badRequest();
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
