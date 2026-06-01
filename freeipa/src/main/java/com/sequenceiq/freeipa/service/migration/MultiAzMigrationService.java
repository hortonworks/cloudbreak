package com.sequenceiq.freeipa.service.migration;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Response;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.operation.OperationService;

@Service
public class MultiAzMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationService.class);

    @Inject
    private OperationService operationService;

    @Inject
    private FreeIpaFlowManager flowManager;

    public FreeIpaMultiAzMigrationV1Response triggerMultiAzMigration(String accountId, Stack stack) {
        Variant targetVariant = calculateTargetVariant(stack.getPlatformvariant());
        LOGGER.info("Calculated target variant is '{}' for stack with variant '{}'.", targetVariant, stack.getPlatformvariant());

        return new FreeIpaMultiAzMigrationV1Response();
    }

    private Variant calculateTargetVariant(String currentVariant) {
        return switch (currentVariant) {
            case CloudConstants.AWS -> AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant();
            default -> Variant.variant(currentVariant);
        };
    }
}
