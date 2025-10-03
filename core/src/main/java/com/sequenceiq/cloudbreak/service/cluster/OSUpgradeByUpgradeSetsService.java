package com.sequenceiq.cloudbreak.service.cluster;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class OSUpgradeByUpgradeSetsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OSUpgradeByUpgradeSetsService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private ClusterRepairService clusterRepairService;

    @Inject
    private StackUpgradeService stackUpgradeService;

    public FlowIdentifier osUpgradeByUpgradeSets(StackView stackView, ImageChangeDto imageChangeDto, List<OrderedOSUpgradeSet> upgradeSets) {
        LOGGER.info("OS upgrade by upgrade sets: {}", upgradeSets);
        validateOrderNumbers(upgradeSets);
        validateInstanceIdsAndStack(stackView.getId(), upgradeSets);
        String upgradeVariant = stackUpgradeService.calculateUpgradeVariant(stackView, ThreadBasedUserCrnProvider.getUserCrn(), false);
        return flowManager.triggerOsUpgradeByUpgradeSetsFlow(stackView.getId(), upgradeVariant, imageChangeDto, upgradeSets);
    }

    private void validateOrderNumbers(List<OrderedOSUpgradeSet> upgradeSets) {
        List<Integer> orderNumbers = upgradeSets.stream().map(OrderedOSUpgradeSet::getOrder).collect(toList());
        Map<Integer, Integer> duplicatedOrderNumbers = orderNumbers.stream().filter(i -> Collections.frequency(orderNumbers, i) > 1)
                .collect(toMap(s -> s, s -> Collections.frequency(orderNumbers, s), (r1, r2) -> r1));
        if (!duplicatedOrderNumbers.isEmpty()) {
            LOGGER.info("Duplicated order number(s): {}", duplicatedOrderNumbers);
            throw new BadRequestException("There are duplicated order number(s): " + duplicatedOrderNumbers.entrySet().stream().map(entry ->
                    entry.getKey() + " appears " + entry.getValue() + " times").collect(Collectors.joining(", ")));
        }
    }

    private void validateInstanceIdsAndStack(Long stackId, List<OrderedOSUpgradeSet> upgradeSets) {
        List<String> upgradeInstanceIdList = upgradeSets.stream()
                .flatMap(numberedOsUpgradeSet -> numberedOsUpgradeSet.getInstanceIds().stream()).collect(toList());
        checkDuplicatedInstanceIds(upgradeInstanceIdList);
        StackDto stack = stackDtoService.getById(stackId);
        List<String> notTerminatedInstanceIds = stack.getNotTerminatedInstanceMetaData().stream().map(InstanceMetadataView::getInstanceId).collect(toList());
        validateAllInstanceSelectedFromStack(upgradeInstanceIdList, notTerminatedInstanceIds);
        validateSelectedInstancesExistsInStack(upgradeInstanceIdList, notTerminatedInstanceIds);
        clusterRepairService.validateRepairConditions(ManualClusterRepairMode.ALL, stack, Set.of()).ifPresent(repairValidation -> {
            LOGGER.info("There are repair validation errors: {}", repairValidation.getValidationErrors());
            throw new BadRequestException(String.join(", ", repairValidation.getValidationErrors()));
        });
    }

    private void checkDuplicatedInstanceIds(List<String> upgradeInstanceIdList) {
        Map<String, Integer> duplicatedElements = upgradeInstanceIdList.stream().filter(i -> Collections.frequency(upgradeInstanceIdList, i) > 1)
                .collect(toMap(s -> s, s -> Collections.frequency(upgradeInstanceIdList, s), (r1, r2) -> r1));
        if (!duplicatedElements.isEmpty()) {
            LOGGER.info("Duplicated elements in upgrade set requests: {}", duplicatedElements);
            throw new BadRequestException("There are duplicated element(s): " + duplicatedElements.entrySet().stream().map(entry ->
                    entry.getKey() + " appears " + entry.getValue() + " times").collect(Collectors.joining(", ")));
        }
    }

    private void validateSelectedInstancesExistsInStack(List<String> upgradeInstanceIdList, List<String> notTerminatedInstanceIds) {
        LOGGER.info("Validate if every selected instance are exists, instance ids for stack: {}", notTerminatedInstanceIds);
        List<String> nonExistingInstanceIds =
                upgradeInstanceIdList.stream().filter(upgradeInstanceId -> !notTerminatedInstanceIds.contains(upgradeInstanceId)).collect(toList());
        if (!nonExistingInstanceIds.isEmpty()) {
            throw new BadRequestException("These instances does not exists: " + nonExistingInstanceIds);
        }
    }

    private void validateAllInstanceSelectedFromStack(List<String> upgradeInstanceIdList, List<String> notTerminatedInstanceIds) {
        LOGGER.info("Validate if all instance are selected from the stack, instance ids for stack: {}", notTerminatedInstanceIds);
        List<String> instancesNotSelectedFromAvailableInstances = notTerminatedInstanceIds.stream()
                .filter(notTerminatedInstanceId -> !upgradeInstanceIdList.contains(notTerminatedInstanceId)).collect(toList());
        if (!instancesNotSelectedFromAvailableInstances.isEmpty()) {
            throw new BadRequestException("All instance must be selected, following instances are not selected for upgrade: " +
                    instancesNotSelectedFromAvailableInstances);
        }
    }

}
