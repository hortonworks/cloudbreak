package com.sequenceiq.redbeams.service.upgrade;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDBStorageCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.service.DatabaseCapabilityService;

@Component
public class DBUpgradeMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBUpgradeMigrationService.class);

    private static final int GB_TO_MB = 1024;

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @Inject
    private DatabaseCapabilityService databaseCapabilityService;

    public DatabaseStack mergeDatabaseStacks(DBStack originalDbStack, UpgradeDatabaseMigrationParams migrationParams, CloudConnector connector,
            CloudCredential cloudCredential, CloudPlatformVariant cloudPlatformVariant, ExternalDatabaseParameters providerDatabaseParameters) {
        com.sequenceiq.redbeams.domain.stack.DatabaseServer originalDatabaseServerEntity = originalDbStack.getDatabaseServer();
        originalDatabaseServerEntity.setInstanceType(getInstanceType(migrationParams.getInstanceType(), connector, cloudCredential,
                cloudPlatformVariant, originalDbStack));
        originalDatabaseServerEntity.setStorageSize(
                getStorageSizeInGB(providerDatabaseParameters, migrationParams, connector, cloudCredential, originalDbStack));
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

    private Long getStorageSizeInGB(ExternalDatabaseParameters originalDatabaseParameters, UpgradeDatabaseMigrationParams targetMigrationParams,
            CloudConnector cloudConnector, CloudCredential cloudCredential, DBStack dbStack) {
        if (isSingleServer(originalDatabaseParameters) && originalDatabaseParameters.storageSizeInMB() != null &&
                originalDatabaseParameters.storageSizeInMB() > targetMigrationParams.getStorageSize() * GB_TO_MB) {
            Optional<PlatformDBStorageCapabilities> storageCapabilities = cloudConnector.platformResources()
                    .databaseStorageCapabilities(cloudCredential, Region.region(dbStack.getRegion()));
            String logMsg = String.format("DB storage size of the flexible server will be overridden as the size of the original db is bigger than" +
                    " the default flexible size (%sGB > %sGB).", originalDatabaseParameters.storageSizeInMB() / GB_TO_MB,
                    targetMigrationParams.getStorageSize());
            if (storageCapabilities.isPresent() && !storageCapabilities.get().supportedStorageSizeInMb().isEmpty()) {
                PlatformDBStorageCapabilities dbStorageCapabilities = storageCapabilities.get();
                Optional<Long> storageCapability = getProperStorageSize(dbStorageCapabilities, originalDatabaseParameters);
                long storageSizeInGB;
                if (storageCapability.isPresent()) {
                    storageSizeInGB = storageCapability.get() / GB_TO_MB;
                    LOGGER.debug("{} New flexible size: {}GB", logMsg, storageSizeInGB);
                } else {
                    storageSizeInGB = dbStorageCapabilities.supportedStorageSizeInMb().last() / GB_TO_MB;
                    LOGGER.debug("{} New flexible size, the biggest supported one: {}GB", logMsg, storageSizeInGB);
                }
                return storageSizeInGB;
            } else {
                long storageSizeInGB = calculateStorageSize(originalDatabaseParameters);
                LOGGER.debug("{} No capability information, calculated size: {}GB", logMsg, storageSizeInGB);
                return storageSizeInGB;
            }
        } else {
            LOGGER.debug("No db storage size override is needed, will use the original setup: {}GB", targetMigrationParams.getStorageSize());
            return targetMigrationParams.getStorageSize();
        }
    }

    /**
     * Calculate the storage size for flexible server based on the original single server storage size. Flexible server only accept 2^n storage sizes like
     * 128GB, 256GB, 512GB, ...
     * The method returns the next 2^n number which is greater than or equal to the original size.
     * @param providerDatabaseParameters information about the original single server
     * @return the calculated flexible server storage size based on the original single server parameters
     */
    private long calculateStorageSize(ExternalDatabaseParameters providerDatabaseParameters) {
        Long storageSizeInMB = providerDatabaseParameters.storageSizeInMB();
        double log2Size = StrictMath.log(storageSizeInMB) / StrictMath.log(2.0);
        return (long) StrictMath.pow(2, StrictMath.ceil(log2Size)) / GB_TO_MB;
    }

    private Optional<Long> getProperStorageSize(PlatformDBStorageCapabilities dbStorageCapabilities, ExternalDatabaseParameters externalDatabaseParameters) {
        return dbStorageCapabilities.supportedStorageSizeInMb().tailSet(externalDatabaseParameters.storageSizeInMB()).stream().findFirst();
    }

    private boolean isSingleServer(ExternalDatabaseParameters databaseParameters) {
        return databaseParameters.databaseType() == AzureDatabaseType.SINGLE_SERVER;
    }
}
