package com.sequenceiq.datalake.service.upgrade.database;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Component
public class SdxRdsUpgradeValidationErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRdsUpgradeValidationErrorHandler.class);

    @Inject
    private SdxService sdxService;

    public SdxCluster handleUpgradeValidationError(SdxCluster sdxCluster, Exception exception) {
        return updateDBServerTypeIfAutomigrationHappened(sdxCluster, exception);
    }

    private SdxCluster updateDBServerTypeIfAutomigrationHappened(SdxCluster sdxCluster, Exception exception) {
        if (exception.getMessage() != null && exception.getMessage().contains(AzureDatabaseType.AZURE_AUTOMIGRATION_ERROR_PREFIX)) {
            LOGGER.info("Automigration happened on Azure side from Single to Flexible server for datalake with id {}," +
                    " database type will be updated to FLEXIBLE_SERVER", sdxCluster.getId());
            SdxDatabase sdxDatabase = sdxCluster.getSdxDatabase();
            Map<String, Object> attributes = sdxDatabase.getAttributesMap();
            attributes.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER);
            sdxDatabase.setAttributes(new Json(attributes));
            return sdxService.save(sdxCluster);
        } else {
            return sdxCluster;
        }
    }
}
