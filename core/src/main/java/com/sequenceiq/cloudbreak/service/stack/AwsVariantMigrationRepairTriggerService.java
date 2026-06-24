package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow2.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.migration.AwsVariantMigrationEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;

@Service
public class AwsVariantMigrationRepairTriggerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsVariantMigrationRepairTriggerService.class);

    @Inject
    private StackUpgradeService stackUpgradeService;

    public boolean shouldRunAwsVariantMigration(ClusterRepairTriggerEvent event, StackDto stackDto) {
        String triggeredVariant = event.getTriggeredStackVariant();
        Set<String> discoveryFqdnsToRepair = event.getFailedNodesMap().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
        if (!event.isUpgrade() && !stackUpgradeService.allNodesSelectedForRepair(stackDto, discoveryFqdnsToRepair)) {
            return false;
        } else {
            String originalPlatformVariant = stackDto.getPlatformVariant();
            LOGGER.debug("Upgrade flow or all the nodes selected for repair, checking that the variant migration is triggerable from " +
                    "original: '{}' to new: '{}'", originalPlatformVariant, triggeredVariant);
            return stackUpgradeService.awsVariantMigrationIsFeasible(stackDto.getStack(), triggeredVariant);
        }
    }

    public AwsVariantMigrationTriggerEvent createMigrationTriggerEvent(Long resourceId, String groupName) {
        return new AwsVariantMigrationTriggerEvent(AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT.event(), resourceId, groupName);
    }
}
