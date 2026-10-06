package com.sequenceiq.redbeams.sync.provider;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerStorageMetrics;
import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.events.RedbeamsEventSenderService;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.sync.DBStackConnector;
import com.sequenceiq.redbeams.sync.DBStackConnector.ConnectedDatabaseStack;

/**
 * Reconciles the instance type and DB engine version stored on the CB side with the actual values reported by the cloud provider.
 * Both instance type and version drift are persisted back to Redbeams (each gated by its own config flag) so the stored state
 * reflects the provider reality after an out-of-band change.
 */
@Component
public class RdsProviderSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsProviderSyncService.class);

    @Inject
    private DBStackConnector dbStackConnector;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private RdsProviderSyncConfig config;

    @Inject
    private RedbeamsEventSenderService eventSenderService;

    public void syncInstanceTypeAndVersion(DBStack dbStack) {
        ConnectedDatabaseStack connected;
        try {
            connected = dbStackConnector.connect(dbStack);
            ExternalDatabaseParameters parameters = connected.connector().resources()
                    .getDatabaseServerParameters(connected.authenticatedContext(), connected.databaseStack());
            if (parameters == null) {
                LOGGER.warn(":::RDS provider sync::: No provider parameters returned for DB stack {}, skipping.", dbStack.getResourceCrn());
            } else {
                boolean instanceChanged = syncInstanceType(dbStack, parameters.instanceType());
                boolean versionChanged = syncVersion(dbStack, parameters.engineVersion());
                boolean storageAlertChanged = checkStorageAndNotify(dbStack, connected);
                if (instanceChanged || versionChanged || storageAlertChanged) {
                    dbStackService.save(dbStack);
                }
            }
        } catch (Exception e) {
            LOGGER.warn(":::RDS provider sync::: Failed to sync provider metadata for DB stack {}: {}", dbStack.getResourceCrn(), e.getMessage(), e);
            return;
        }
    }

    private boolean checkStorageAndNotify(DBStack dbStack, ConnectedDatabaseStack connected) {
        if (!config.isStorageMonitoringEnabled()) {
            return false;
        }
        try {
            Optional<DatabaseServerStorageMetrics> metrics = connected.connector().resources()
                    .getDatabaseServerStorageMetrics(connected.authenticatedContext(), connected.databaseStack());
            if (metrics.isEmpty() || metrics.get().freeStoragePercentage() == null) {
                LOGGER.debug(":::RDS provider sync::: No storage metrics reported for DB stack {}, skipping storage check.", dbStack.getResourceCrn());
                return false;
            }
            double freePercentage = metrics.get().freeStoragePercentage();
            boolean low = freePercentage < config.getStorageLowThresholdPercentage();
            if (low && !dbStack.isLowStorage()) {
                LOGGER.info(":::RDS provider sync::: DB stack {} is low on storage ({}% free, threshold {}%), notifying UI.",
                        dbStack.getResourceCrn(), freePercentage, config.getStorageLowThresholdPercentage());
                eventSenderService.sendStorageLowNotification(dbStack, freePercentage);
                dbStack.setLowStorage(true);
                return true;
            } else if (!low && dbStack.isLowStorage()) {
                LOGGER.info(":::RDS provider sync::: DB stack {} storage recovered ({}% free), re-arming low-storage alert.",
                        dbStack.getResourceCrn(), freePercentage);
                dbStack.setLowStorage(false);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn(":::RDS provider sync::: Failed to check storage for DB stack {}: {}", dbStack.getResourceCrn(), e.getMessage(), e);
            return false;
        }
    }

    private boolean syncInstanceType(DBStack dbStack, String providerInstanceType) {
        if (StringUtils.isBlank(providerInstanceType)) {
            LOGGER.debug(":::RDS provider sync::: Provider did not report an instance type for DB stack {}, skipping.", dbStack.getResourceCrn());
            return false;
        }
        DatabaseServer databaseServer = dbStack.getDatabaseServer();
        if (databaseServer == null) {
            LOGGER.debug(":::RDS provider sync::: DB stack {} has no database server, skipping instance type sync.", dbStack.getResourceCrn());
            return false;
        }
        String storedInstanceType = databaseServer.getInstanceType();
        if (providerInstanceType.equals(storedInstanceType)) {
            LOGGER.debug(":::RDS provider sync::: Instance type for DB stack {} is up to date: {}", dbStack.getResourceCrn(), storedInstanceType);
            return false;
        }
        if (!config.isUpdateInstanceType()) {
            LOGGER.info(":::RDS provider sync::: Instance type drift detected for DB stack {} (CB: '{}', provider: '{}'), but update is disabled.",
                    dbStack.getResourceCrn(), storedInstanceType, providerInstanceType);
            return false;
        }
        LOGGER.info(":::RDS provider sync::: Updating instance type for DB stack {} from '{}' to provider value '{}'.",
                dbStack.getResourceCrn(), storedInstanceType, providerInstanceType);
        databaseServer.setInstanceType(providerInstanceType);
        return true;
    }

    private boolean syncVersion(DBStack dbStack, String providerEngineVersion) {
        if (StringUtils.isBlank(providerEngineVersion)) {
            LOGGER.debug(":::RDS provider sync::: Provider did not report an engine version for DB stack {}, skipping.", dbStack.getResourceCrn());
            return false;
        }
        MajorVersion storedMajorVersion = dbStack.getMajorVersion();
        Optional<MajorVersion> providerMajorVersion = MajorVersion.get(providerEngineVersion);
        if (providerMajorVersion.isEmpty()) {
            LOGGER.warn(":::RDS provider sync::: Provider reported unrecognized engine version '{}' for DB stack {} (CB major version: {}).",
                    providerEngineVersion, dbStack.getResourceCrn(), storedMajorVersion);
            return false;
        }
        if (providerMajorVersion.get() == storedMajorVersion) {
            LOGGER.debug(":::RDS provider sync::: DB engine version for DB stack {} is up to date: {}", dbStack.getResourceCrn(), storedMajorVersion);
            return false;
        }
        if (!config.isUpdateVersion()) {
            LOGGER.info(":::RDS provider sync::: DB engine version drift detected for DB stack {} (CB: {}, provider: {} ('{}')), but update is disabled.",
                    dbStack.getResourceCrn(), storedMajorVersion, providerMajorVersion.get(), providerEngineVersion);
            return false;
        }
        LOGGER.info(":::RDS provider sync::: Updating DB engine version for DB stack {} from {} to provider value {} ('{}').",
                dbStack.getResourceCrn(), storedMajorVersion, providerMajorVersion.get(), providerEngineVersion);
        dbStack.setMajorVersion(providerMajorVersion.get());
        return true;
    }
}
