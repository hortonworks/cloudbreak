package com.sequenceiq.redbeams.service.upgrade;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.service.DatabaseCapabilityService;

@Component
public class DBUpgradeMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBUpgradeMigrationService.class);

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @Inject
    private DatabaseCapabilityService databaseCapabilityService;

    public DatabaseStack mergeDatabaseStacks(DBStack originalDbStack, UpgradeDatabaseMigrationParams migrationParams, CloudConnector connector,
            CloudCredential cloudCredential, CloudPlatformVariant cloudPlatformVariant) {

        com.sequenceiq.redbeams.domain.stack.DatabaseServer originalDatabaseServerEntity = originalDbStack.getDatabaseServer();
        originalDatabaseServerEntity.setInstanceType(getInstanceType(migrationParams.getInstanceType(), connector, cloudCredential,
                cloudPlatformVariant, originalDbStack));
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

    private String getInstanceType(String instanceType, CloudConnector connector, CloudCredential cloudCredential, CloudPlatformVariant cloudPlatformVariant,
            DBStack dbStack) {
        if (StringUtils.isEmpty(instanceType)) {
            String defaultInstanceType = databaseCapabilityService.getDefaultInstanceType(connector, cloudCredential, cloudPlatformVariant,
                    Region.region(dbStack.getRegion()));
            LOGGER.debug("Database instancetype is missing, will use {} in {} region on {}", defaultInstanceType, dbStack.getRegion(), cloudPlatformVariant);
            return defaultInstanceType;
        } else {
            return instanceType;
        }
    }
}
