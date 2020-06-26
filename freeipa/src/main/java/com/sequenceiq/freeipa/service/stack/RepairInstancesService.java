package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_EVENT;
import static com.sequenceiq.freeipa.flow.instance.reboot.RebootEvent.REBOOT_EVENT;
import static java.util.function.Predicate.not;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.repair.RepairInstancesRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.instance.InstanceEvent;
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
    private FreeIpaHealthDetailsService healthDetailsService;

    @Inject
    private OperationService operationService;

    @Inject
    private OperationToOperationStatusConverter operationToOperationStatusConverter;

    private Map<String, InstanceStatus> getInstanceHealthMap(String accountId, String environmentCrn) {
        return healthDetailsService.getHealthDetails(environmentCrn, accountId).getNodeHealthDetails().stream()
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
                .collect(Collectors.toMap(InstanceMetaData::getInstanceId, Function.identity()));
    }

    /**
     * If no instance passed in request, repair all bad instances (at least 1 instance must be good)
     * If instances passed in request, repair all valid passed bad instances (at least 1 instance must remain)
     * If force and instances passed in request, repair all valid passed instances (at least 1 instance must remain)
     * If force and no instances passed in request then report an error
     * @param accountId - The account id for the instance to repair.
     * @param request - A RepairInstanceRequest containing request parameters.
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

        if (instancesToRepair.keySet().isEmpty()) {
            throw new NotFoundException("No unhealthy instances to repair.  Maybe use the force option.");
        }
        Set<InstanceMetaData> remainingInstances = allInstancesByInstanceId.values().stream()
                .filter(instanceMetaData -> !instancesToRepair.containsKey(instanceMetaData.getInstanceId()))
                .filter(instanceMetaData -> {
                    if (request.isForceRepair()) {
                        return !INVALID_REPAIR_STATUSES.contains(instanceMetaData.getInstanceStatus());
                    } else {
                        return healthMap.containsKey(instanceMetaData.getInstanceId()) && healthMap.get(instanceMetaData.getInstanceId()).isAvailable();
                    }
                })
                .collect(Collectors.toSet());
        if (remainingInstances.isEmpty()) {
            throw new BadRequestException("At least one instance must remain running during a repair.");
        }

        Operation operation =
                operationService.startOperation(accountId, OperationType.REPAIR, Set.of(stack.getEnvironmentCrn()), Collections.emptySet());
        flowManager.notify(DOWNSCALE_EVENT.event(), new DownscaleEvent(DOWNSCALE_EVENT.event(), stack.getId(),
                instancesToRepair.keySet().stream().collect(Collectors.toList()), operation.getOperationId()));
        return operationToOperationStatusConverter.convert(operation);
    }

    /**
     * If no instance passed in request, reboot all bad instances
     * If instances passed in request, reboot all valid passed bad instances
     * If force and instances passed in request, reboot all valid passed instances
     * If force and no instances passed in request, reboot all instances
     * @param accountId - The account id for the instance to reboot.
     * @param request - A RebootInstanceRequest containing request parameters.
     */
    public void rebootInstances(String accountId, RebootInstancesRequest request) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(request.getEnvironmentCrn(), accountId);
        MDCBuilder.buildMdcContext(stack);
        Map<String, InstanceMetaData> allInstancesByInstanceId = getAllInstancesFromStack(stack);
        Map<String, InstanceStatus> healthMap = request.isForceReboot() ? Collections.emptyMap() : getInstanceHealthMap(accountId, request.getEnvironmentCrn());
        Map<String, InstanceMetaData> instancesToReboot =
                getInstancesToRepair(healthMap, allInstancesByInstanceId, request.getInstanceIds(), request.isForceReboot(), true);

        if (instancesToReboot.keySet().isEmpty()) {
            throw new NotFoundException("No unhealthy instances to reboot.  Maybe use the force option.");
        }

        flowManager.notify(REBOOT_EVENT.event(), new InstanceEvent(REBOOT_EVENT.event(), stack.getId(),
                instancesToReboot.keySet().stream().collect(Collectors.toList())));
    }
}
