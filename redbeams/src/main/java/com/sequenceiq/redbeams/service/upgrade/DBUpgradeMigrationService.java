package com.sequenceiq.redbeams.service.upgrade;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;

@Component
public class DBUpgradeMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBUpgradeMigrationService.class);

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    public DatabaseStack mergeDatabaseStacks(DBStack originalDbStack,
            UpgradeDatabaseMigrationParams migrationParams, CloudConnector connector) {

        com.sequenceiq.redbeams.domain.stack.DatabaseServer originalDatabaseServerEntity = originalDbStack.getDatabaseServer();
        originalDatabaseServerEntity.setInstanceType(migrationParams.getInstanceType());
        originalDatabaseServerEntity.setStorageSize(migrationParams.getStorageSize());
        originalDatabaseServerEntity.setAttributes(migrationParams.getAttributes());
        originalDbStack.setDatabaseServer(originalDatabaseServerEntity);

        DatabaseStack migratedDatabaseStack = databaseStackConverter.convert(originalDbStack);

        // DatabaseStack contains template, but it is also required (only the field AzureDatabaseType) to generate new template
        try {
            String newTemplate = connector.resources().getDBStackTemplate(migratedDatabaseStack);
            DatabaseStack mergedDatabaseStack = new DatabaseStack(
                    migratedDatabaseStack.getNetwork(),
                    migratedDatabaseStack.getDatabaseServer(),
                    migratedDatabaseStack.getTags(),
                    newTemplate);
            LOGGER.debug("Migrated database stack is {}", mergedDatabaseStack);
            return mergedDatabaseStack;
        } catch (TemplatingNotSupportedException e) {
            LOGGER.debug("Templates are not supported for platform {}, using stack without saving new template", originalDbStack.getCloudPlatform());
            return migratedDatabaseStack;
        }
    }
}
