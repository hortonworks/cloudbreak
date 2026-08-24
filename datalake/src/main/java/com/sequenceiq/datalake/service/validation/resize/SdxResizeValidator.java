package com.sequenceiq.datalake.service.validation.resize;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.service.sdx.database.AzureDatabaseAttributesService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Component
public class SdxResizeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxResizeValidator.class);

    @Inject
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    @Inject
    private EntitlementService entitlementService;

    public void validateDatabaseTypeForResize(SdxDatabase sdxDatabase, CloudPlatform cloudPlatform) {
        if (CloudPlatform.AZURE.equals(cloudPlatform) && hasExternalDatabase(sdxDatabase) && isSingleServer(sdxDatabase)) {
            String message = "Resizing a DataLake cluster is not possible when using Azure Single Server database type. "
                    + "To proceed with the resizing operation, you will first need to upgrade your cluster’s database type to Azure Flexible Server. "
                    + "Once this change is made, you can retry the resize operation.";
            LOGGER.warn(message);
            throw new BadRequestException(message);
        }
    }

    public void validateResizeShapeTransition(SdxClusterShape currentShape, SdxClusterShape targetShape, boolean singleToMultiAzTransition) {
        if (SdxClusterShape.LIGHT_DUTY_PRO.equals(targetShape)) {
            String message = String.format("Resizing to %s shape is not supported.", SdxClusterShape.LIGHT_DUTY_PRO);
            LOGGER.warn(message);
            throw new BadRequestException(message);
        }
        boolean multiAzResizeOnSameShape = currentShape.equals(targetShape) && singleToMultiAzTransition;
        if (SdxClusterShape.ENTERPRISE_PRO.equals(targetShape) && !SdxClusterShape.LIGHT_DUTY_PRO.equals(currentShape)
                && !multiAzResizeOnSameShape) {
            String message = String.format("Resizing to %s shape is only supported from %s shape, but the current shape is %s.",
                    SdxClusterShape.ENTERPRISE_PRO, SdxClusterShape.LIGHT_DUTY_PRO, currentShape);
            LOGGER.warn(message);
            throw new BadRequestException(message);
        }
        if (SdxClusterShape.ENTERPRISE.equals(targetShape) && currentShape.isWithoutHbase()) {
            String message = String.format("Resizing to %s shape is not supported from %s shape.", SdxClusterShape.ENTERPRISE, currentShape);
            LOGGER.warn(message);
            throw new BadRequestException(message);
        }
    }

    private boolean hasExternalDatabase(SdxDatabase sdxDatabase) {
        return SdxDatabaseAvailabilityType.hasExternalDatabase(sdxDatabase.getDatabaseAvailabilityType());
    }

    private boolean isSingleServer(SdxDatabase sdxDatabase) {
        return AzureDatabaseType.SINGLE_SERVER.equals(azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase));
    }
}
