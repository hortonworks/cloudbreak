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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.datalake.DataLakeStatusCheckerService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordTriggerService;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordValidator;
import com.sequenceiq.cloudbreak.service.salt.SaltPasswordStatusService;
import com.sequenceiq.cloudbreak.service.spot.SpotInstanceUsageCondition;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.util.NotAllowedStatusUpdate;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
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
    private StackDtoService stackDtoService;

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
    private StackApiViewService stackApiViewService;

    @Inject
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    @Inject
    private RotateSaltPasswordTriggerService rotateSaltPasswordTriggerService;

    @Inject
    private RotateSaltPasswordValidator rotateSaltPasswordValidator;

    @Inject
    private SaltPasswordStatusService saltPasswordStatusService;

    public FlowIdentifier removeInstance(StackDto stack, String instanceId, boolean forced) {
        InstanceMetaData metaData = updateNodeCountValidator.validateInstanceForDownscale(instanceId, stack.getStack());
        String instanceGroupName = metaData.getInstanceGroupName();
        int scalingAdjustment = -1;
        updateNodeCountValidator.validateServiceRoles(stack, instanceGroupName, scalingAdjustment);
        if (!forced) {
            updateNodeCountValidator.validateScalabilityOfInstanceGroup(stack, instanceGroupName, scalingAdjustment);
            updateNodeCountValidator.validateScalingAdjustment(instanceGroupName, scalingAdjustment, stack);
        }
        return flowManager.triggerStackRemoveInstance(stack.getId(), instanceGroupName, metaData.getPrivateId(), forced);
    }

    public FlowIdentifier removeInstances(StackDto stack, Collection<String> instanceIds, boolean forced) {
        Map<String, Set<Long>> instanceIdsByHostgroupMap = new HashMap<>();
        for (String instanceId : instanceIds) {
            InstanceMetaData metaData = updateNodeCountValidator.validateInstanceForDownscale(instanceId, stack.getStack());
            instanceIdsByHostgroupMap.computeIfAbsent(metaData.getInstanceGroupName(), s -> new LinkedHashSet<>()).add(metaData.getPrivateId());
        }
        updateNodeCountValidator.validateServiceRoles(stack, instanceIdsByHostgroupMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().size() * -1)));
        if (!forced) {
            for (Map.Entry<String, Set<Long>> entry : instanceIdsByHostgroupMap.entrySet()) {
                String instanceGroupName = entry.getKey();
                int scalingAdjustment = entry.getValue().size() * -1;
                updateNodeCountValidator.validateScalabilityOfInstanceGroup(stack, instanceGroupName, scalingAdjustment);
                updateNodeCountValidator.validateScalingAdjustment(instanceGroupName, scalingAdjustment, stack);
            }
        }
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_REQUESTED,
                String.format("Requested node count for downscaling: %s, instance group(s): %s",
                        instanceIds.size(), instanceIdsByHostgroupMap.keySet()));
        return flowManager.triggerStackRemoveInstances(stack.getId(), instanceIdsByHostgroupMap, forced);
    }

    public FlowIdentifier stopInstances(StackDto stackDto, Collection<String> instanceIds, boolean forced) {
        LOGGER.info("Received stop instances request for instanceIds: [{}]", instanceIds);

        if (instanceIds == null || instanceIds.isEmpty()) {
            throw new BadRequestException("Stop request cannot process an empty instanceIds collection");
        }
        StackView stack = stackDto.getStack();
        Map<String, Set<Long>> instanceIdsByHostgroupMap = new HashMap<>();
        Set<String> instanceIdsWithoutMetadata = new HashSet<>();
        for (String instanceId : instanceIds) {
            InstanceMetaData metaData = updateNodeCountValidator.validateInstanceForStop(instanceId, stack);
            if (metaData != null) {
                instanceIdsByHostgroupMap.computeIfAbsent(metaData.getInstanceGroupName(), s -> new LinkedHashSet<>()).add(metaData.getPrivateId());
            } else {
                instanceIdsWithoutMetadata.add(instanceId);
            }
        }
        if (instanceIdsByHostgroupMap.size() > 1) {
            throw new BadRequestException("Downscale via Instance Stop cannot process more than one host group");
        }
        updateNodeCountValidator.validateInstanceGroup(stackDto, instanceIdsByHostgroupMap.keySet().iterator().next());
        updateNodeCountValidator.validateStackStatusForStopStartHostGroup(stackDto, instanceIdsByHostgroupMap.keySet().iterator().next(),
                -instanceIdsByHostgroupMap.size());
        LOGGER.info("InstanceIds without metadata: [{}]", instanceIdsWithoutMetadata);
        updateNodeCountValidator.validateServiceRoles(stackDto, instanceIdsByHostgroupMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().size() * -1)));
        updateNodeCountValidator.validateInstanceGroupForStopStart(stackDto, instanceIdsByHostgroupMap.keySet().iterator().next(),
                instanceIdsByHostgroupMap.entrySet().iterator().next().getValue().size() * -1);
        LOGGER.info("Stopping the following instances: {}", instanceIdsByHostgroupMap);
        if (!forced) {
            for (Entry<String, Set<Long>> entry : instanceIdsByHostgroupMap.entrySet()) {
                String instanceGroupName = entry.getKey();
                int scalingAdjustment = entry.getValue().size() * -1;
                updateNodeCountValidator.validateScalabilityOfInstanceGroup(stackDto, instanceGroupName, scalingAdjustment);
                updateNodeCountValidator.validateScalingAdjustment(instanceGroupName, scalingAdjustment, stackDto);
            }
        }
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_BY_STOP_REQUESTED,
                "Requested node count for downscaling (stopstart): " + instanceIds.size());
        return flowManager.triggerStopStartStackDownscale(stack.getId(), instanceIdsByHostgroupMap, forced);
    }

    public FlowIdentifier updateImage(ImageChangeDto imageChangeDto) {
        return flowManager.triggerStackImageUpdate(imageChangeDto);
    }

    public FlowIdentifier updateStatus(StackDto stackDto, StatusRequest status, boolean updateCluster) {
        switch (status) {
            case SYNC:
                return sync(stackDto.getStack(), false);
            case FULL_SYNC:
                return sync(stackDto.getStack(), true);
            case REPAIR_FAILED_NODES:
                return repairFailedNodes(stackDto.getId());
            case STOPPED:
                return stop(stackDto, updateCluster);
            case STARTED:
                return start(stackDto.getStack());
            default:
                throw new BadRequestException("Cannot update the status of stack because status request not valid.");
        }
    }

    @VisibleForTesting
    FlowIdentifier triggerStackStopIfNeeded(StackDto stackDto, ClusterView cluster, boolean updateCluster) {
        if (!isStopNeeded(stackDto)) {
            return FlowIdentifier.notTriggered();
        }
        if (spotInstanceUsageCondition.isStackRunsOnSpotInstances(stackDto)) {
            throw new BadRequestException(format("Cannot update the status of stack '%s' to STOPPED, because it runs on spot instances", stackDto.getName()));
        }
        StackView stack = stackDto.getStack();
        environmentService.checkEnvironmentStatus(stack, EnvironmentStatus.stoppable());
        checkForZombieInstances(stackDto);
        if (cluster != null && !stack.isStopped() && !stack.isStopFailed()) {
            if (!updateCluster) {
                throw NotAllowedStatusUpdate
                        .stack(stack)
                        .to(STOPPED)
                        .badRequest();
            } else if (stack.isReadyForStop() || stack.isStopFailed()) {
                sendStopRequestedEvent(stack);
                return clusterOperationService.updateStatus(stackDto.getId(), StatusRequest.STOPPED);
            } else {
                throw NotAllowedStatusUpdate
                        .cluster(stack)
                        .to(STOPPED)
                        .badRequest();
            }
        } else {
            return flowManager.triggerStackStop(stackDto.getId());
        }
    }

    private void checkForZombieInstances(StackDto stack) {
        List<InstanceMetadataView> zombieInstanceMetaDataSet = stack.getZombieInstanceMetaData();
        if (!zombieInstanceMetaDataSet.isEmpty()) {
            Set<String> zombieInstanceIds = zombieInstanceMetaDataSet.stream().map(im -> im.getInstanceId()).collect(Collectors.toSet());
            LOGGER.warn("Cannot stop cluster, because there are nodes in ZOMBIE status: {}", zombieInstanceIds);
            throw new BadRequestException(format("Cannot stop cluster, because there are nodes in ZOMBIE status: %s. Please delete these nodes and try again.",
                    zombieInstanceIds));
        }
    }

    private FlowIdentifier repairFailedNodes(Long stackId) {
        LOGGER.debug("Received request to replace failed nodes: {}", stackId);
        return flowManager.triggerManualRepairFlow(stackId);
    }

    private FlowIdentifier sync(StackView stack, boolean full) {
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

    public FlowIdentifier syncComponentVersionsFromCm(StackView stack, Set<String> candidateImageUuids) {
        return flowManager.triggerSyncComponentVersionsFromCm(stack.getId(), candidateImageUuids);
    }

    private FlowIdentifier stop(StackDto stackDto, boolean updateCluster) {
        ClusterView cluster = stackDto.getCluster();
        StackView stack = stackDto.getStack();
        if (cluster != null && stack.isStopInProgress()) {
            sendStopRequestedEvent(stack);
            return FlowIdentifier.notTriggered();
        } else {
            return triggerStackStopIfNeeded(stackDto, cluster, updateCluster);
        }
    }

    public FlowIdentifier updateNodeCountStartInstances(StackDto stackDto, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson,
            boolean withClusterEvent, ScalingStrategy scalingStrategy) {

        if (instanceGroupAdjustmentJson.getScalingAdjustment() == 0) {
            throw new BadRequestException("Attempting to upscale zero instances");
        }
        if (instanceGroupAdjustmentJson.getScalingAdjustment() < 0) {
            throw new BadRequestException("Attempting to downscale via the start instances method. (File a bug)");
        }

        StackView stack = stackDto.getStack();
        environmentService.checkEnvironmentStatus(stack, EnvironmentStatus.upscalable());
        try {
            return transactionService.required(() -> {
                updateNodeCountValidator.validateServiceRoles(stackDto, instanceGroupAdjustmentJson);
                updateNodeCountValidator.validateStackStatusForStopStartHostGroup(stackDto, instanceGroupAdjustmentJson.getInstanceGroup(),
                        instanceGroupAdjustmentJson.getScalingAdjustment());
                updateNodeCountValidator.validateInstanceGroup(stackDto, instanceGroupAdjustmentJson.getInstanceGroup());
                updateNodeCountValidator.validateInstanceGroupForStopStart(stackDto,
                        instanceGroupAdjustmentJson.getInstanceGroup(), instanceGroupAdjustmentJson.getScalingAdjustment());
                updateNodeCountValidator.validateScalabilityOfInstanceGroup(stackDto, instanceGroupAdjustmentJson);
                updateNodeCountValidator.validateScalingAdjustment(instanceGroupAdjustmentJson, stackDto);
                if (withClusterEvent) {
                    updateNodeCountValidator.validateHostGroupIsPresent(instanceGroupAdjustmentJson, stackDto);
                    updateNodeCountValidator.validateCMStatus(stackDto);
                }
                stackUpdater.updateStackStatus(stackDto.getId(), DetailedStackStatus.UPSCALE_BY_START_REQUESTED,
                        "Requested node count for upscaling (stopstart): " + instanceGroupAdjustmentJson.getScalingAdjustment());
                return flowManager.triggerStopStartStackUpscale(
                        stackDto.getId(),
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

    public FlowIdentifier updateNodeCount(StackDto stackDto, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, boolean withClusterEvent) {
        StackView stack = stackDto.getStack();
        environmentService.checkEnvironmentStatus(stack, EnvironmentStatus.upscalable());
        try {
            return transactionService.required(() -> {
                boolean upscale = instanceGroupAdjustmentJson.getScalingAdjustment() > 0;
                updateNodeCountValidator.validateServiceRoles(stackDto, instanceGroupAdjustmentJson);
                updateNodeCountValidator.validateStackStatus(stack, upscale);
                updateNodeCountValidator.validateInstanceGroup(stackDto, instanceGroupAdjustmentJson.getInstanceGroup());
                updateNodeCountValidator.validateScalabilityOfInstanceGroup(stackDto, instanceGroupAdjustmentJson);
                updateNodeCountValidator.validateScalingAdjustment(instanceGroupAdjustmentJson, stackDto);
                boolean instanceStatusValidationNeeded = !upscale || !targetedUpscaleSupportService.targetedUpscaleOperationSupported(stack);
                if (instanceStatusValidationNeeded) {
                    updateNodeCountValidator.validateInstanceStatuses(stackDto, instanceGroupAdjustmentJson);
                }
                if (withClusterEvent) {
                    updateNodeCountValidator.validateClusterStatus(stack, upscale);
                    updateNodeCountValidator.validateHostGroupIsPresent(instanceGroupAdjustmentJson, stackDto);
                    if (instanceStatusValidationNeeded) {
                        updateNodeCountValidator.validataHostMetadataStatuses(stackDto, instanceGroupAdjustmentJson);
                    }
                }
                if (upscale) {
                    stackUpdater.updateStackStatus(stackDto.getId(), DetailedStackStatus.UPSCALE_REQUESTED,
                            String.format("Requested node count for upscaling: %s, instance group: %s",
                                    instanceGroupAdjustmentJson.getScalingAdjustment(), instanceGroupAdjustmentJson.getInstanceGroup()));
                    return flowManager.triggerStackUpscale(
                            stackDto.getId(),
                            instanceGroupAdjustmentJson,
                            withClusterEvent);
                } else {
                    stackUpdater.updateStackStatus(stackDto.getId(), DetailedStackStatus.DOWNSCALE_REQUESTED,
                            String.format("Requested node count for downscaling: %s, instance group: %s",
                                    abs(instanceGroupAdjustmentJson.getScalingAdjustment()), instanceGroupAdjustmentJson.getInstanceGroup()));
                    return flowManager.triggerStackDownscale(stackDto.getId(), instanceGroupAdjustmentJson);
                }
            });
        } catch (TransactionExecutionException e) {
            if (e.getCause() instanceof ConcurrencyFailureException) {
                LOGGER.info("A concurrent update arrived to this cluster.", e.getCause());
                throw new BadRequestException("A concurrent update arrived to this cluster. Please try again later.");
            }
            if (e.getCause() instanceof BadRequestException) {
                throw e.getCause();
            }
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    @VisibleForTesting
    FlowIdentifier start(StackView stack) {
        FlowIdentifier flowIdentifier = FlowIdentifier.notTriggered();
        environmentService.checkEnvironmentStatus(stack, EnvironmentStatus.startable());
        dataLakeStatusCheckerService.validateAvailableState(stack);
        if (stack.isAvailable()) {
            eventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), STACK_START_IGNORED);
        } else if (stack.isReadyForStart() || stack.isStartFailed()) {
            flowIdentifier = flowManager.triggerStackStart(stack.getId());
        } else {
            throw new BadRequestException(String.format("Can't start the cluster because it is in %s state.", Optional.ofNullable(stack.getStatus())
                    .map(Status::name)
                    .orElse("N/A")));
        }
        return flowIdentifier;
    }

    public FlowIdentifier renewCertificate(String stackName, String accountId) {
        StackView stack = stackDtoService.getStackViewByName(stackName, accountId);
        return renewCertificate(stack.getId());
    }

    public FlowIdentifier renewInternalCertificate(String stackCrn) {
        StackView stack = stackDtoService.getStackViewByCrn(stackCrn);
        return renewCertificate(stack.getId());
    }

    public FlowIdentifier renewInternalCertificate(NameOrCrn nameOrCrn, StackType stackType) {
        StackApiView stackApiView = stackApiViewService.retrieveStackByCrnAndType(nameOrCrn.getCrn(), stackType);
        return renewCertificate(stackApiView.getId());
    }

    public FlowIdentifier renewCertificate(Long stackId) {
        return flowManager.triggerClusterCertificationRenewal(stackId);
    }

    private boolean isStopNeeded(StackDto stackDto) {
        boolean result = true;
        StopRestrictionReason reason = stackStopRestrictionService.isInfrastructureStoppable(stackDto);
        StackView stack = stackDto.getStack();
        if (stack.isStopped()) {
            eventService.fireCloudbreakEvent(stack.getId(), STOPPED.name(), STACK_STOP_IGNORED);
            result = false;
        } else if (reason != StopRestrictionReason.NONE) {
            throw new BadRequestException(
                    format("Cannot stop a stack '%s'. Reason: %s", stack.getName(), reason.getReason()));
        } else if (!stack.isAvailable() && !stack.isStopFailed()
                && !stack.isAvailableWithStoppedInstances() && !stack.hasNodeFailure()) {
            throw NotAllowedStatusUpdate
                    .stack(stack)
                    .to(STOPPED)
                    .badRequest();
        }
        return result;
    }

    public boolean rangerRazEnabled(String crn) {
        StackDto stack = stackDtoService.getByCrn(crn);
        return clusterService.isRangerRazEnabledOnCluster(stack);
    }

    private void sendStopRequestedEvent(StackView stack) {
        eventService.fireCloudbreakEvent(stack.getId(), STOP_REQUESTED.name(), STACK_STOP_REQUESTED);
    }

    public FlowIdentifier reRegisterClusterProxyConfig(@NotNull NameOrCrn nameOrCrn, String accountId, String originalCrn) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        return flowManager.triggerClusterProxyConfigReRegistration(stack.getId(), originalCrn);
    }

    public FlowIdentifier rotateSaltPassword(@NotNull NameOrCrn nameOrCrn, String accountId, RotateSaltPasswordReason reason) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        rotateSaltPasswordValidator.validateRotateSaltPassword(stack);
        return rotateSaltPasswordTriggerService.triggerRotateSaltPassword(stack, reason);
    }

    public SaltPasswordStatus getSaltPasswordStatus(@NotNull NameOrCrn nameOrCrn, String accountId) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return saltPasswordStatusService.getSaltPasswordStatus(stack);
    }

    public FlowIdentifier modifyProxyConfig(NameOrCrn nameOrCrn, String accountId, String previousProxyConfigCrn) {
        StackDto stack = stackDtoService.getByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        return flowManager.triggerModifyProxyConfig(stack.getId(), previousProxyConfigCrn);
    }
}
