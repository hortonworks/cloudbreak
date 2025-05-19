package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_EVENT;
import static java.util.function.Predicate.not;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rebuild.RebuildRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.repair.RepairInstancesRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.converter.stack.StackToCreateFreeIpaRequestConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.repair.event.RepairEvent;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootInstanceEvent;
import com.sequenceiq.freeipa.flow.stack.termination.action.TerminationService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class RepairInstancesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepairInstancesService.class);

    private static final String DELETED_NAME_DELIMITER = "_";

    private static final Set<InstanceStatus> INVALID_REPAIR_STATUSES = Set.of(
            InstanceStatus.DECOMMISSIONED,
            InstanceStatus.TERMINATED,
            InstanceStatus.DELETED_ON_PROVIDER_SIDE,
            InstanceStatus.DELETED_BY_PROVIDER,
            InstanceStatus.STOPPED);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private FreeIpaStackHealthDetailsService healthDetailsService;

    @Inject
    private OperationService operationService;

    @Inject
    private OperationToOperationStatusConverter operationToOperationStatusConverter;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private FreeIpaCreationService freeIpaCreationService;

    @Inject
    private StackToCreateFreeIpaRequestConverter stackToCreateFreeIpaRequestConverter;

    @Inject
    private TerminationService terminationService;

    private void validate(String accountId, Stack stack, Set<InstanceMetaData> remainingGoodInstances, Set<InstanceMetaData> remainingBadInstances,
            Collection<InstanceMetaData> instancesToRepair) {
        LOGGER.debug("Validating repair for account {} and stack ID {}. Remaining good instances [{}]. Remaining bad instances [{}]. Instances to repair [{}].",
                accountId, stack.getId(), remainingGoodInstances, remainingBadInstances, instancesToRepair);
        if (instancesToRepair.isEmpty()) {
            throwNotFoundException("No unhealthy instances to repair. You can try to use the force option to enforce the repair process.");
        }
        if (remainingGoodInstances.isEmpty()) {
            throwBadRequest("At least one instance must remain running with a good status during a repair.");
        }
        if (!remainingBadInstances.isEmpty()) {
            throwBadRequest(String.format("At least one remaining non-repaired instance(s) have a bad status: [%s]. " +
                    "All remaining instances must have a good status during a repair.", remainingBadInstances));
        }
        if (stack.getInstanceGroups().isEmpty()) {
            throwBadRequest("At least one instance group must be present for a repair.");
        }
    }

    private Map<String, InstanceStatus> getInstanceHealthMap(String accountId, String environmentCrn) {
        return healthDetailsService.getHealthDetails(environmentCrn, accountId).getNodeHealthDetails().stream()
                .filter(nodeHealthDetails -> Objects.nonNull(nodeHealthDetails.getInstanceId()))
                .collect(Collectors.toMap(NodeHealthDetails::getInstanceId, NodeHealthDetails::getStatus));
    }

    private Collection<String> getValidInstanceIds(Collection<String> allInstances, Collection<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return allInstances;
        } else {
            Collection<String> validInstanceIds = instanceIds.stream()
                    .filter(allInstances::contains)
                    .collect(Collectors.toSet());
            if (validInstanceIds.size() != instanceIds.size()) {
                String badIds = instanceIds.stream()
                        .filter(not(allInstances::contains)).collect(Collectors.joining(","));
                throwBadRequest(MessageFormat.format("Invalid instanceIds in request {0}.", badIds));
            }
            return validInstanceIds;
        }
    }

    private Map<String, InstanceMetaData> getInstancesToRepair(Map<String, InstanceStatus> healthMap, Map<String, InstanceMetaData> allInstances,
            List<String> instanceIds, boolean force, boolean reboot) {
        Collection<String> validInstanceIds = getValidInstanceIds(allInstances.keySet(), instanceIds);

        Map<String, InstanceMetaData> instancesToRepair = validInstanceIds.stream()
                .filter(instanceId -> force || healthMap.get(instanceId) != null && !healthMap.get(instanceId).isAvailable())
                .collect(Collectors.toMap(Function.identity(), allInstances::get));
        if (instancesToRepair.size() != validInstanceIds.size()) {
            LOGGER.info("Not {} instances {} because force was not selected.", reboot ? "repairing" : "rebooting", validInstanceIds.stream()
                    .filter(instance -> !instancesToRepair.containsKey(instance)).collect(Collectors.joining(",")));
        }

        return instancesToRepair;
    }

    private Map<String, InstanceMetaData> getAllInstancesFromStack(Stack stack) {
        return stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getInstanceMetaData().stream())
                .filter(instanceMetaData -> Objects.nonNull(instanceMetaData.getInstanceId()))
                .collect(Collectors.toMap(InstanceMetaData::getInstanceId, Function.identity()));
    }

    private List<String> getAdditionalTerminatedInstanceIds(Collection<InstanceMetaData> instanceMetaDataCollection, List<String> requestedInstanceIds) {
        return instanceMetaDataCollection.stream()
                .filter(im -> im.isTerminated() || im.isDeletedOnProvider())
                .map(InstanceMetaData::getInstanceId)
                .filter(Objects::nonNull)
                .filter(id -> Objects.isNull(requestedInstanceIds) || !requestedInstanceIds.contains(id))
                .collect(Collectors.toList());
    }

    /**
     * If no instance passed in request, repair all bad instances (at least 1 instance must be good)
     * If instances passed in request, repair all valid passed bad instances (at least 1 instance must remain)
     * If force and instances passed in request, repair all valid passed instances (at least 1 instance must remain)
     * If force and no instances passed in request then report an error
     *
     * @param accountId - The account id for the instance to repair.
     * @param request   - A RepairInstanceRequest containing request parameters.
     */
    public OperationStatus repairInstances(String accountId, RepairInstancesRequest request) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(request.getEnvironmentCrn(), accountId);
        LOGGER.debug("Repairing freeipa instances, request: {}", request);
        validateForceRepairHasInstances(request);

        Map<String, InstanceStatus> healthMap = request.isForceRepair() ? Collections.emptyMap() : getInstanceHealthMap(accountId, request.getEnvironmentCrn());
        Map<String, InstanceMetaData> allInstancesByInstanceId = getAllInstancesFromStack(stack);
        Map<String, InstanceMetaData> instancesToRepair =
                getInstancesToRepair(healthMap, allInstancesByInstanceId, request.getInstanceIds(), request.isForceRepair(), false);
        LOGGER.debug("Freeipa repair, instances: healthmap: {}, all instances by instance id: {}, instances to repair: {}",
                healthMap, allInstancesByInstanceId, instancesToRepair);

        Set<InstanceMetaData> remainingGoodInstances =
                getRemainingGoodInstances(allInstancesByInstanceId, instancesToRepair, healthMap, request.isForceRepair());
        Set<InstanceMetaData> remainingBadInstances = getRemainingBadInstances(allInstancesByInstanceId, instancesToRepair, healthMap, request.isForceRepair());
        LOGGER.debug("Freeipa repair, remaining good instances: {}, remaining bad instances: {}", remainingGoodInstances, remainingBadInstances);
        validate(accountId, stack, remainingGoodInstances, remainingBadInstances, instancesToRepair.values());
        int nodeCount = stack.getInstanceGroups().stream().findFirst().get().getNodeCount();

        List<String> additionalTerminatedInstanceIds = getAdditionalTerminatedInstanceIds(allInstancesByInstanceId.values(), request.getInstanceIds());
        LOGGER.debug("Freeipa repair, terminated instances: {}, node count: {}", additionalTerminatedInstanceIds, nodeCount);

        Operation operation = operationService.startOperation(accountId, OperationType.REPAIR, Set.of(stack.getEnvironmentCrn()), Collections.emptySet());
        if (operation.getStatus() == OperationState.RUNNING) {
            try {
                flowManager.notify(FlowChainTriggers.REPAIR_TRIGGER_EVENT, new RepairEvent(FlowChainTriggers.REPAIR_TRIGGER_EVENT, stack.getId(),
                        operation.getOperationId(), nodeCount, new ArrayList<>(instancesToRepair.keySet()), additionalTerminatedInstanceIds));
            } catch (Exception e) {
                LOGGER.error("Couldn't start Freeipa repair flow", e);
                Operation failedOperation = operationService.failOperation(accountId, operation.getOperationId(),
                        "Couldn't start Freeipa repair flow: " + e.getMessage());
                return operationToOperationStatusConverter.convert(failedOperation);
            }
        }
        return operationToOperationStatusConverter.convert(operation);
    }

    private void validateForceRepairHasInstances(RepairInstancesRequest request) {
        if (request.isForceRepair() && CollectionUtils.isEmpty(request.getInstanceIds())) {
            String message = "Force repair requires the instance IDs to be provided.";
            LOGGER.error(message);
            throw new BadRequestException(message);
        }
    }

    private Set<InstanceMetaData> getRemainingGoodInstances(Map<String, InstanceMetaData> allInstancesByInstanceId,
            Map<String, InstanceMetaData> instancesToRepair, Map<String, InstanceStatus> healthMap, boolean forceRepair) {
        Set<InstanceMetaData> remainingGoodInstances = allInstancesByInstanceId.values().stream()
                .filter(instanceMetaData -> !instancesToRepair.containsKey(instanceMetaData.getInstanceId()))
                .filter(instanceMetaData -> {
                    if (forceRepair) {
                        return !INVALID_REPAIR_STATUSES.contains(instanceMetaData.getInstanceStatus());
                    } else {
                        return healthMap.containsKey(instanceMetaData.getInstanceId()) && healthMap.get(instanceMetaData.getInstanceId()).isAvailable();
                    }
                })
                .collect(Collectors.toSet());
        LOGGER.debug("Remaining good instances [{}]", remainingGoodInstances);
        return remainingGoodInstances;
    }

    private Set<InstanceMetaData> getRemainingBadInstances(Map<String, InstanceMetaData> allInstancesByInstanceId,
            Map<String, InstanceMetaData> instancesToRepair, Map<String, InstanceStatus> healthMap, boolean forceRepair) {
        Set<InstanceMetaData> remainingBadInstances = allInstancesByInstanceId.values().stream()
                .filter(instanceMetaData -> !instancesToRepair.containsKey(instanceMetaData.getInstanceId()))
                .filter(instanceMetaData -> !instanceMetaData.isTerminated())
                .filter(instanceMetaData -> {
                    if (forceRepair) {
                        return INVALID_REPAIR_STATUSES.contains(instanceMetaData.getInstanceStatus());
                    } else {
                        return !healthMap.containsKey(instanceMetaData.getInstanceId()) || !healthMap.get(instanceMetaData.getInstanceId()).isAvailable();
                    }
                })
                .collect(Collectors.toSet());
        LOGGER.debug("Remaining bad instances [{}]", remainingBadInstances);
        return remainingBadInstances;
    }

    /**
     * If no instance passed in request, reboot all bad instances
     * If instances passed in request, reboot all valid passed bad instances
     * If force and instances passed in request, reboot all valid passed instances
     * If force and no instances passed in request, reboot all instances
     *
     * @param accountId - The account id for the instance to reboot.
     * @param request   - A RebootInstanceRequest containing request parameters.
     */
    public OperationStatus rebootInstances(String accountId, RebootInstancesRequest request) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(request.getEnvironmentCrn(), accountId);
        Map<String, InstanceMetaData> allInstancesByInstanceId = getAllInstancesFromStack(stack);
        Map<String, InstanceStatus> healthMap = request.isForceReboot() ? Collections.emptyMap() : getInstanceHealthMap(accountId, request.getEnvironmentCrn());
        Map<String, InstanceMetaData> instancesToReboot =
                getInstancesToRepair(healthMap, allInstancesByInstanceId, request.getInstanceIds(), request.isForceReboot(), true);

        if (instancesToReboot.isEmpty()) {
            throwNotFoundException("No unhealthy instances to reboot. You can try to use the force option to enforce the repair process.");
        }

        Operation operation = operationService.startOperation(accountId, OperationType.REBOOT, Set.of(stack.getEnvironmentCrn()), Collections.emptySet());
        if (operation.getStatus() == OperationState.RUNNING) {
            try {
                flowManager.notify(REBOOT_EVENT.event(), new RebootInstanceEvent(REBOOT_EVENT.event(), stack.getId(),
                        new ArrayList<>(instancesToReboot.keySet()), operation.getOperationId()));
            } catch (Exception e) {
                LOGGER.error("Couldn't start Freeipa reboot flow", e);
                Operation failedOperation = operationService.failOperation(accountId, operation.getOperationId(),
                        "Couldn't start Freeipa reboot flow: " + e.getMessage());
                return operationToOperationStatusConverter.convert(failedOperation);
            }
        }
        return operationToOperationStatusConverter.convert(operation);
    }

    public DescribeFreeIpaResponse rebuild(String accountId, RebuildRequest rebuildRequest) {
        if (!entitlementService.isFreeIpaRebuildEnabled(accountId)) {
            throwBadRequest("The FreeIPA rebuild capability is disabled.");
        }
        Stack stack = stackService.getByCrnAndAccountIdEvenIfTerminated(rebuildRequest.getEnvironmentCrn(), accountId, rebuildRequest.getSourceCrn());
        LOGGER.debug("Freeipa rebuild request: {}", rebuildRequest);
        Optional<Stack> nonTerminatedStack = stackService.findByEnvironmentCrnAndAccountId(rebuildRequest.getEnvironmentCrn(), accountId);
        if (nonTerminatedStack.isPresent()) {
            throwBadRequest("There is a stack which hasn't been terminated.");
        }
        renameStackIfNeeded(stack);
        CreateFreeIpaRequest createFreeIpaRequest = stackToCreateFreeIpaRequestConverter.convert(stack);

        return freeIpaCreationService.launchFreeIpa(createFreeIpaRequest, accountId);
    }

    void renameStackIfNeeded(Stack stack) {
        if (!stack.isDeleteCompleted()) {
            throwBadRequest(String.format("The stack %s has not been terminated", stack.getResourceCrn()));
        }
        Long terminated = stack.getTerminated();
        String originalName = stack.getName();
        String deletionTime = StringUtils.substringAfterLast(originalName, DELETED_NAME_DELIMITER);
        if (terminated == -1 || !deletionTime.equals(terminated.toString())) {
            LOGGER.info("Updating terminated stack name from {}, prior terminated time {}", originalName, terminated);
            terminationService.finalizeTermination(stack.getId());
        }
    }

    private void throwBadRequest(String error) {
        LOGGER.error(error);
        throw new BadRequestException(error);
    }

    private void throwNotFoundException(String message) {
        LOGGER.error(message);
        throw new NotFoundException(message);
    }

}