package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_EVENT;
import static java.util.function.Predicate.not;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.repair.RepairInstancesRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.repair.event.RepairEvent;
import com.sequenceiq.freeipa.flow.instance.reboot.RebootInstanceEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class RepairInstancesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepairInstancesService.class);

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
    private StackUpdater stackUpdater;

    private void validate(String accountId, Stack stack, Set<InstanceMetaData> remainingGoodInstances, Set<InstanceMetaData> remainingBadInstances,
            Collection<InstanceMetaData> instancesToRepair) {
        LOGGER.debug("Validating repair for account {} and stack ID {}. Remaining good instances [{}]. Remaining bad instances [{}]. Instances to repair [{}].",
                accountId, stack.getId(), remainingGoodInstances, remainingBadInstances, instancesToRepair);
        if (!entitlementService.freeIpaHaRepairEnabled(accountId)) {
            throw new BadRequestException("The FreeIPA HA Repair capability is disabled.");
        }
        if (instancesToRepair.isEmpty()) {
            throw new NotFoundException("No unhealthy instances to repair.  Maybe use the force option.");
        }
        if (remainingGoodInstances.isEmpty()) {
            throw new BadRequestException("At least one instance must remain running with a good status during a repair.");
        }
        if (!remainingBadInstances.isEmpty()) {
            String errorMsg = "At least one remaining non-repaired instance has a bad status. All remaining instances must have a good status during a repair.";
            LOGGER.error("{}. The following instances have a bad status: [{}]", errorMsg, remainingBadInstances);
            throw new BadRequestException(errorMsg);
        }
        if (stack.getInstanceGroups().isEmpty()) {
            throw new BadRequestException("At least one instace group must be present for a repair.");
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
                String msg = MessageFormat.format("Invalid instanceIds in request {0}.", badIds);
                LOGGER.error(msg);
                throw new BadRequestException(msg);
            }
            return validInstanceIds;
        }
    }

    private Map<String, InstanceMetaData> getInstancesToRepair(Map<String, InstanceStatus> healthMap, Map<String, InstanceMetaData> allInstances,
            List<String> instanceIds, boolean force, boolean reboot) {
        Collection<String> validInstanceIds = getValidInstanceIds(allInstances.keySet(), instanceIds);

        Map<String, InstanceMetaData> instancesToRepair = validInstanceIds.stream()
                .filter(instanceId -> force || (healthMap.get(instanceId) != null && !healthMap.get(instanceId).isAvailable()))
                .collect(Collectors.toMap(Function.identity(), instanceId -> allInstances.get(instanceId)));
        if (instancesToRepair.keySet().size() != validInstanceIds.size()) {
            LOGGER.info("Not {} instances {} because force was not selected.", reboot ? "repairing" : "rebooting", validInstanceIds.stream()
                    .filter(instance -> !instancesToRepair.keySet().contains(instance)).collect(Collectors.joining(",")));
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
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);

        if (request.isForceRepair() && CollectionUtils.isEmpty(request.getInstanceIds())) {
            throw new UnsupportedOperationException("Force repair requires the instance IDs to be provided.");
        }

        Map<String, InstanceStatus> healthMap = request.isForceRepair() ? Collections.emptyMap() : getInstanceHealthMap(accountId, request.getEnvironmentCrn());
        Map<String, InstanceMetaData> allInstancesByInstanceId = getAllInstancesFromStack(stack);
        Map<String, InstanceMetaData> instancesToRepair =
                getInstancesToRepair(healthMap, allInstancesByInstanceId, request.getInstanceIds(), request.isForceRepair(), false);

        Set<InstanceMetaData> remainingGoodInstances =
                getRemainingGoodInstances(allInstancesByInstanceId, instancesToRepair, healthMap, request.isForceRepair());
        Set<InstanceMetaData> remainingBadInstances = getRemainingBadInstances(allInstancesByInstanceId, instancesToRepair, healthMap, request.isForceRepair());
        validate(accountId, stack, remainingGoodInstances, remainingBadInstances, instancesToRepair.values());
        int nodeCount = stack.getInstanceGroups().stream().findFirst().get().getNodeCount();

        List<String> additionalTerminatedInstanceIds = getAdditionalTerminatedInstanceIds(allInstancesByInstanceId.values(), request.getInstanceIds());

        Operation operation = operationService.startOperation(accountId, OperationType.REPAIR, Set.of(stack.getEnvironmentCrn()), Collections.emptySet());
        if (operation.getStatus() == OperationState.RUNNING) {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.REPAIR_REQUESTED, "Repair requested");
            flowManager.notify(FlowChainTriggers.REPAIR_TRIGGER_EVENT, new RepairEvent(FlowChainTriggers.REPAIR_TRIGGER_EVENT, stack.getId(),
                    operation.getOperationId(), nodeCount, instancesToRepair.keySet().stream().collect(Collectors.toList()), additionalTerminatedInstanceIds));
        }
        return operationToOperationStatusConverter.convert(operation);
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
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        Map<String, InstanceMetaData> allInstancesByInstanceId = getAllInstancesFromStack(stack);
        Map<String, InstanceStatus> healthMap = request.isForceReboot() ? Collections.emptyMap() : getInstanceHealthMap(accountId, request.getEnvironmentCrn());
        Map<String, InstanceMetaData> instancesToReboot =
                getInstancesToRepair(healthMap, allInstancesByInstanceId, request.getInstanceIds(), request.isForceReboot(), true);

        if (instancesToReboot.keySet().isEmpty()) {
            throw new NotFoundException("No unhealthy instances to reboot.  Maybe use the force option.");
        }


        Operation operation = operationService.startOperation(accountId, OperationType.REBOOT, Set.of(stack.getEnvironmentCrn()), Collections.emptySet());
        if (operation.getStatus() == OperationState.RUNNING) {
            stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.REPAIR_REQUESTED, "Reboot requested");
            flowManager.notify(REBOOT_EVENT.event(), new RebootInstanceEvent(REBOOT_EVENT.event(), stack.getId(),
                    instancesToReboot.keySet().stream().collect(Collectors.toList()), operation.getOperationId()));
        }
        return operationToOperationStatusConverter.convert(operation);
    }
}
