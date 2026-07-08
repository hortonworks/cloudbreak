package com.sequenceiq.redbeams.sync.provider;

import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.database.ExternalDatabaseParameters;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.sync.DBStackConnector;
import com.sequenceiq.redbeams.sync.DBStackConnector.ConnectedDatabaseStack;

/**
 * Reconciles the instance type and DB engine version stored on the CB side with the actual values reported by the cloud provider.
 * Instance type drift is persisted; version drift is logged only (a version change requires the manual upgrade flow to run).
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

    public void syncInstanceTypeAndVersion(DBStack dbStack) {
        try {
            ConnectedDatabaseStack connected = dbStackConnector.connect(dbStack);
            ExternalDatabaseParameters parameters = connected.connector().resources()
                    .getDatabaseServerParameters(connected.authenticatedContext(), connected.databaseStack());
            if (parameters == null) {
                LOGGER.warn(":::RDS provider sync::: No provider parameters returned for DB stack {}, skipping.", dbStack.getResourceCrn());
                return;
            }
            syncInstanceType(dbStack, parameters.instanceType());
            logVersionDrift(dbStack, parameters.engineVersion());
        } catch (Exception e) {
            LOGGER.warn(":::RDS provider sync::: Failed to sync provider metadata for DB stack {}: {}", dbStack.getResourceCrn(), e.getMessage(), e);
        }
    }

    private void syncInstanceType(DBStack dbStack, String providerInstanceType) {
        if (StringUtils.isBlank(providerInstanceType)) {
            LOGGER.debug(":::RDS provider sync::: Provider did not report an instance type for DB stack {}, skipping.", dbStack.getResourceCrn());
            return;
        }
        String storedInstanceType = dbStack.getDatabaseServer() == null ? null : dbStack.getDatabaseServer().getInstanceType();
        if (providerInstanceType.equals(storedInstanceType)) {
            LOGGER.debug(":::RDS provider sync::: Instance type for DB stack {} is up to date: {}", dbStack.getResourceCrn(), storedInstanceType);
            return;
        }
        if (!config.isUpdateInstanceType()) {
            LOGGER.info(":::RDS provider sync::: Instance type drift detected for DB stack {} (CB: '{}', provider: '{}'), but update is disabled.",
                    dbStack.getResourceCrn(), storedInstanceType, providerInstanceType);
            return;
        }
        LOGGER.info(":::RDS provider sync::: Updating instance type for DB stack {} from '{}' to provider value '{}'.",
                dbStack.getResourceCrn(), storedInstanceType, providerInstanceType);
        dbStack.getDatabaseServer().setInstanceType(providerInstanceType);
        dbStackService.save(dbStack);
    }

    private void logVersionDrift(DBStack dbStack, String providerEngineVersion) {
        if (StringUtils.isBlank(providerEngineVersion)) {
            LOGGER.debug(":::RDS provider sync::: Provider did not report an engine version for DB stack {}, skipping.", dbStack.getResourceCrn());
            return;
        }
        MajorVersion storedMajorVersion = dbStack.getMajorVersion();
        Optional<MajorVersion> providerMajorVersion = MajorVersion.get(providerEngineVersion);
        if (providerMajorVersion.isEmpty()) {
            LOGGER.warn(":::RDS provider sync::: Provider reported unrecognized engine version '{}' for DB stack {} (CB major version: {}).",
                    providerEngineVersion, dbStack.getResourceCrn(), storedMajorVersion);
            return;
        }
        if (providerMajorVersion.get() != storedMajorVersion) {
            LOGGER.warn(":::RDS provider sync::: DB engine version drift detected for DB stack {}: CB major version is {} but provider reports {} ('{}'). "
                    + "Not updating automatically; a manual database upgrade flow is required to reconcile the version.",
                    dbStack.getResourceCrn(), storedMajorVersion, providerMajorVersion.get(), providerEngineVersion);
        } else {
            LOGGER.debug(":::RDS provider sync::: DB engine version for DB stack {} is up to date: {}", dbStack.getResourceCrn(), storedMajorVersion);
        }
    }
}
