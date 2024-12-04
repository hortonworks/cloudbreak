package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.common.model.AzureDatabaseType;

@Component
public class RdsUpgradeValidationResultHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RdsUpgradeValidationResultHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DatabaseService databaseService;

    public void handleUpgradeValidationWarning(Long stackId, String validationReason) {
        updateDBServerTypeIfAutomigrationHappened(stackId, validationReason);
    }

    private void updateDBServerTypeIfAutomigrationHappened(Long stackId, String validationReason) {
        if (validationReason != null && validationReason.contains(AzureDatabaseType.AZURE_AUTOMIGRATION_ERROR_PREFIX)) {
            LOGGER.info("Automigration happened on Azure side from Single to Flexible server for stack with id {}", stackId);
            Optional<Database> database = stackDtoService.getDatabaseByStackId(stackId);
            database.ifPresentOrElse(db -> {
                LOGGER.info("Database type will be updated to FLEXIBLE_SERVER");
                Map<String, Object> attributes = db.getAttributesMap();
                attributes.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER);
                db.setAttributes(new Json(attributes));
                databaseService.save(db);
            }, () -> LOGGER.warn("No database found for stack with id {}", stackId));
        }
    }
}
