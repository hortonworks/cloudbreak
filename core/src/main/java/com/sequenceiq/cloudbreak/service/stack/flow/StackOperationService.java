package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_START_IGNORED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_STOP_IGNORED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_STOP_REQUESTED;
import static java.lang.Math.abs;
import static java.lang.String.format;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
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
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.service.FlowCancelService;

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

    @Inject
    private FlowCancelService flowCancelService;

    public FlowIdentifier removeInstance(Stack stack, String instanceId, boolean forced) {
        InstanceMetaData metaData = updateNodeCountValidator.validateInstanceForDownscale(instanceId, stack);
        String instanceGroupName = metaData.getInstanceGroupName();
        int scalingAdjustment = -1;
        updateNodeCountValidator.validateServiceRoles(stack, instanceGroupName, scalingAdjustment);
        if (!forced) {
            updateNodeCountValidator.validateScalabilityOfInstanceGroup(stack, instanceGroupName, scalingAdjustment);
            updateNodeCountValidator.validateScalingAdjustment(instanceGroupName, scalingAdjustment, stack);
        }
        return flowManager.triggerStackRemoveInstance(stack.getId(), instanceGroupName, metaData.getPrivateId(), forced);
    }

    public FlowIdentifier removeInstances(Stack stack, Collection<String> instanceIds, boolean forced, ScalingStrategy scalingStrategy) {
        LOGGER.info("Received removeInstances request with scaling strategy for instanceIds: {}, [{}]", scalingStrategy, instanceIds);

        // TODO CB-14929: How is a null instanceId set / an empty-instanceid set handled by the downstream handlers
        if (instanceIds == null) {
            throw new BadRequestException("Downscale request cannot process a 'null' instanceIds collection");
        }

        Map<String, Set<Long>> instanceIdsByHostgroupMap = new HashMap<>();
        for (String instanceId : instanceIds) {
            // TODO CB-14929: CB-15154 An invalid instanceId should not necessarily fail a downscale-stop operation?
            InstanceMetaData metaData = updateNodeCountValidator.validateInstanceForDownscale(instanceId, stack);
            instanceIdsByHostgroupMap.computeIfAbsent(metaData.getInstanceGroupName(), s -> new LinkedHashSet<>()).add(metaData.getPrivateId());
        }
        updateNodeCountValidator.validateServiceRoles(stack, instanceIdsByHostgroupMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().size() * -1)));
        LOGGER.info("ZZZ: removeInstances: instanceIds/hostnames received: {}", instanceIds);
        LOGGER.info("ZZZ: removeInstances: computedInstanceIds: {}", instanceIdsByHostgroupMap);
        // TODO CB-14929: Validations for start-stop
        if (!forced) {
            for (Map.Entry<String, Set<Long>> entry : instanceIdsByHostgroupMap.entrySet()) {
                String instanceGroupName = entry.getKey();
                int scalingAdjustment = entry.getValue().size() * -1;
                updateNodeCountValidator.validateScalabilityOfInstanceGroup(stack, instanceGroupName, scalingAdjustment);
                updateNodeCountValidator.validateScalingAdjustment(instanceGroupName, scalingAdjustment, stack);
            }
        }
        if (scalingStrategy != null) {
            // TODO: CB-14929: CB-15154 stopstart can only support a single hostGroup being specified.
            // TODO: CB-14929: Should this not be putting the stack into a UpdateInProgress state?, and maybe run inside a transaction? (similar to the upscale)
            if (instanceIdsByHostgroupMap.size() > 1) {
                throw new BadRequestException("Downscale via Instance Stop cannot process more than one host group");
            }
            return flowManager.triggerStopStartStackDownscale(stack.getId(), instanceIdsByHostgroupMap, forced);
        } else {
            return flowManager.triggerStackRemoveInstances(stack.getId(), instanceIdsByHostgroupMap, forced);
        }
    }

    public FlowIdentifier updateImage(ImageChangeDto imageChangeDto) {
        return flowManager.triggerStackImageUpdate(imageChangeDto);
    }

    public FlowIdentifier updateStatus(Long stackId, StatusRequest status, boolean updateCluster) {
        Stack stack = stackService.getByIdWithLists(stackId);
        Cluster cluster = null;
        if (stack.getCluster() != null) {
            cluster = clusterService.findOneWithLists(stack.getCluster().getId()).orElse(null);
        }
        switch (status) {
            case SYNC:
                return sync(stack, false);
            case FULL_SYNC:
                return sync(stack, true);
            case REPAIR_FAILED_NODES:
                return repairFailedNodes(stack);
            case STOPPED:
                return stop(stack, cluster, updateCluster);
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
        if (cluster != null && !stack.isStopped() && !stack.isStopFailed()) {
            if (!updateCluster) {
                throw NotAllowedStatusUpdate
                        .stack(stack)
                        .to(STOPPED)
                        .badRequest();
            } else if (stack.isReadyForStop() || stack.isStopFailed()) {
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

    private FlowIdentifier repairFailedNodes(Stack stack) {
        LOGGER.debug("Received request to replace failed nodes: {}", stack.getId());
        return flowManager.triggerManualRepairFlow(stack.getId());
    }

    private FlowIdentifier sync(Stack stack, boolean full) {
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

    public FlowIdentifier syncComponentVersionsFromCm(Stack stack, Set<String> candidateImageUuids) {
        return flowManager.triggerSyncComponentVersionsFromCm(stack.getId(), candidateImageUuids);
    }

    private FlowIdentifier stop(Stack stack, Cluster cluster, boolean updateCluster) {
        if (cluster != null && stack.isStopInProgress()) {
            setStackStatusToStopRequested(stack);
            return FlowIdentifier.notTriggered();
        } else {
            return triggerStackStopIfNeeded(stack, cluster, updateCluster);
        }
    }

    public FlowIdentifier updateNodeCountStartInstances(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson,
            boolean withClusterEvent, ScalingStrategy scalingStrategy) {

        // TODO CB-14929: Post CB-15162 Remove this
        if (instanceGroupAdjustmentJson.getScalingAdjustment() == 0) {
            LOGGER.info("ZZZ: Scaling Adjustment 0. Cancalling existing flows... and continuing");
            flowCancelService.cancelRunningFlows(stack.getId());
        }
        if (instanceGroupAdjustmentJson.getScalingAdjustment() < 0) {
            throw new BadRequestException(String.format("Attempting to downscale via the start instances method. (File a bug)"));
        }

        environmentService.checkEnvironmentStatus(stack, EnvironmentStatus.upscalable());
        try {
            return transactionService.required(() -> {
                Stack stackWithLists = stackService.getByIdWithLists(stack.getId());

                // TODO CB-14929: Post CB-15162 Remove this
                if (!stack.isAvailable()) {
                    LOGGER.info("ZZZ: Stack status is not AVAILABLE. Resetting, and cancelling existing flows... it is at: {}", stackWithLists.getStatus());
                    flowCancelService.cancelRunningFlows(stack.getId());
                    stackWithLists.getStackStatus().setStatus(AVAILABLE);
                    clusterService.updateClusterStatusByStackId(stackWithLists.getId(), AVAILABLE, "fake update");
                    stackUpdater.updateStackStatus(stackWithLists.getId(), DetailedStackStatus.PROVISIONED,
                            String.format("fake update"));
                }

                // TODO CB-14929: validateServiceRoles needs adjusting - it counts NMs, and could be the place where we allow this only for NMs/gateways.
                updateNodeCountValidator.validateServiceRoles(stackWithLists, instanceGroupAdjustmentJson);
                updateNodeCountValidator.validateStackStatus(stackWithLists);
                updateNodeCountValidator.validateInstanceGroup(stackWithLists, instanceGroupAdjustmentJson.getInstanceGroup());
                // TODO CB-14929: The next 2 validation also needs adjustment. Counts instances - which stop/start will not modify.
                updateNodeCountValidator.validateScalabilityOfInstanceGroup(stackWithLists, instanceGroupAdjustmentJson);
                updateNodeCountValidator.validateScalingAdjustment(instanceGroupAdjustmentJson, stackWithLists);
                // TODO CB-14929: Instead of validating not in RUNNIG state, this validation needs to change to disallow everything
                //  except if instances are in STOPPED state.
                // TODO CB-14929: Overall cleanup of code changes made in this class
                //updateNodeCountValidator.validateInstanceStatuses(stackWithLists, instanceGroupAdjustmentJson);
                LOGGER.info("ZZZ: cluster.isAvailable: {}, clusterStatus: {}", stackWithLists.getCluster().isAvailable(),
                        stackWithLists.getCluster().getStatus());
                if (withClusterEvent) {
                    // TODO CB-14929: Figure out valid states for start/stop operations.
//                    updateNodeCountValidator.validateClusterStatus(stackWithLists);
                    updateNodeCountValidator.validateHostGroupAdjustment(
                            instanceGroupAdjustmentJson,
                            stackWithLists,
                            instanceGroupAdjustmentJson.getScalingAdjustment());
                    // TODO CB-14929: This validations needs to be removed or adjusted. Checks for UNHEALTHY service status.
                    //  Needs to change to check unhealthy CM / unhealthy RM eventually.
                    updateNodeCountValidator.validataHostMetadataStatuses(stackWithLists, instanceGroupAdjustmentJson);
                }
                // TODO CB-14929: Post CB-15162 and add an additional check to ignore upscale requests of size 0 in stackCommonService

                // TODO CB-14929: When should DB status be updated. Should the DetailStackStatus be something other than UPSCALE_REQUESTED.
                //  i.e. differentiate between an actual upscale and a 'VM pool / start-stop'
                stackUpdater.updateStackStatus(stackWithLists.getId(), DetailedStackStatus.UPSCALE_REQUESTED,
                        "Requested node count for upscaling (stopstart): " + instanceGroupAdjustmentJson.getScalingAdjustment());
                return flowManager.triggerStopStartStackUpscale(
                        stackWithLists.getId(),
                        instanceGroupAdjustmentJson,
                        withClusterEvent);
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof BadRequestException) {
                throw e.getCause();
            }
            throw new TransactionRuntimeExecutionException(e);
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
                    updateNodeCountValidator.validateHostGroupIsPresent(instanceGroupAdjustmentJson, stackWithLists);
                    updateNodeCountValidator.validataHostMetadataStatuses(stackWithLists, instanceGroupAdjustmentJson);
                }
                if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
                    stackUpdater.updateStackStatus(stackWithLists.getId(), DetailedStackStatus.UPSCALE_REQUESTED,
                            "Requested node count for upscaling: " + instanceGroupAdjustmentJson.getScalingAdjustment());
                    return flowManager.triggerStackUpscale(
                            stackWithLists.getId(),
                            instanceGroupAdjustmentJson,
                            withClusterEvent);
                } else {
                    stackUpdater.updateStackStatus(stackWithLists.getId(), DetailedStackStatus.DOWNSCALE_REQUESTED,
                            "Requested node count for downscaling: " + abs(instanceGroupAdjustmentJson.getScalingAdjustment()));
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
        if (stack.isAvailable()) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), STACK_START_IGNORED);
        } else if (stack.isReadyForStart() || stack.isStartFailed()) {
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
        StopRestrictionReason reason = stackStopRestrictionService.isInfrastructureStoppable(stack);
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

    public boolean rangerRazEnabled(Long workspaceId, String crn) {
        Stack stack = stackService.getByCrnInWorkspace(crn, workspaceId);
        return clusterService.isRangerRazEnabledOnCluster(stack);
    }

    private void setStackStatusToStopRequested(Stack stack) {
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOP_REQUESTED, "Stopping of cluster infrastructure has been requested.");
        eventService.fireCloudbreakEvent(stack.getId(), STOP_REQUESTED.name(), STACK_STOP_REQUESTED);
    }
}
