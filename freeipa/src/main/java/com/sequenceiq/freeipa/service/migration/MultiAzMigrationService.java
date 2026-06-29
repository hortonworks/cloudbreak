package com.sequenceiq.freeipa.service.migration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.chain.FlowChainTriggers;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class MultiAzMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationService.class);

    @Inject
    private OperationService operationService;

    @Inject
    private FreeIpaFlowManager flowManager;

    public FreeIpaMultiAzMigrationV1Response triggerMultiAzMigration(String environmentCrn, String accountId, Stack stack) {
        Variant sourceVariant = Variant.variant(stack.getPlatformvariant());
        Variant targetVariant = calculateTargetVariant(sourceVariant);
        Set<InstanceMetaData> allInstances = stack.getNotDeletedInstanceMetaDataSet();
        Set<String> allInstanceIds = getAllInstanceIds(allInstances);
        String primaryGwInstanceId = getPrimaryGwInstanceId(allInstances);

        Operation operation = operationService.startOperation(accountId, OperationType.MIGRATE_TO_MULTI_AZ, List.of(environmentCrn), List.of());
        if (operation.getStatus() != OperationState.RUNNING) {
            throw new BadRequestException("Failed to start the multi-AZ migration operation for the FreeIPA: " + operation.getError());
        }
        try {
            MultiAzMigrationEvent event = new MultiAzMigrationEvent(
                    FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT,
                    stack.getId(),
                    operation.getOperationId(),
                    sourceVariant,
                    targetVariant,
                    new HashSet<>(allInstanceIds),
                    primaryGwInstanceId
            );
            LOGGER.info("Triggering multi-AZ migration operation for the FreeIPA: {}", event);
            FlowIdentifier flowIdentifier = flowManager.notify(FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT, event);
            FreeIpaMultiAzMigrationV1Response response = new FreeIpaMultiAzMigrationV1Response();
            response.setFlowIdentifier(flowIdentifier);
            response.setOperationId(operation.getOperationId());
            return response;
        } catch (Exception e) {
            try {
                operationService.failOperation(accountId, operation.getOperationId(), "Could not start FreeIPA multi-AZ migration flow: " + e.getMessage());
            } catch (Exception failOperationException) {
                LOGGER.error("Failed to mark operation {} as failed after trigger failure.", operation.getOperationId(), failOperationException);
                failOperationException.addSuppressed(e);
                throw failOperationException;
            }
            throw e;
        }
    }

    private static Variant calculateTargetVariant(Variant sourceVariant) {
        return switch (sourceVariant.getValue()) {
            case CloudConstants.AWS -> AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant();
            default -> sourceVariant;
        };
    }

    private static Set<String> getAllInstanceIds(Set<InstanceMetaData> allInstances) {
        return allInstances.stream()
                .map(InstanceMetaData::getInstanceId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static String getPrimaryGwInstanceId(Set<InstanceMetaData> allInstances) {
        return allInstances.stream()
                .filter(i -> InstanceMetadataType.GATEWAY_PRIMARY.equals(i.getInstanceMetadataType()))
                .map(InstanceMetaData::getInstanceId)
                .findFirst()
                .orElseThrow(() -> new CloudbreakRuntimeException("Primary gateway could not be determined for the stack."));
    }
}
