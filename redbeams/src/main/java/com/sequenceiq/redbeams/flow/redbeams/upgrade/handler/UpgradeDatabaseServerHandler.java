package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;


import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsUpgradeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.stack.DBResourceService;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.upgrade.DBUpgradeMigrationService;

@Component
public class UpgradeDatabaseServerHandler extends ExceptionCatcherEventHandler<UpgradeDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeDatabaseServerHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private DBResourceService dbResourceService;

    @Inject
    private DBUpgradeMigrationService upgradeMigrationService;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpgradeDatabaseServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpgradeDatabaseServerRequest> event) {
        RedbeamsUpgradeFailedEvent failure = new RedbeamsUpgradeFailedEvent(resourceId, e);
        LOGGER.warn("Error upgrading the database server:", e);
        return failure;
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpgradeDatabaseServerRequest> handlerEvent) {
        Event<UpgradeDatabaseServerRequest> event = handlerEvent.getEvent();
        LOGGER.debug("Received event: {}", event);
        UpgradeDatabaseServerRequest request = event.getData();
        DatabaseStack databaseStack = request.getDatabaseStack();
        TargetMajorVersion targetMajorVersion = request.getTargetMajorVersion();
        CloudCredential cloudCredential = request.getCloudCredential();
        CloudContext cloudContext = request.getCloudContext();
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        UpgradeDatabaseMigrationParams migrationParams = request.getUpgradeDatabaseMigrationParams();
        boolean databaseMigrationRequired = (migrationParams != null);
        Selectable response;
        DBStack dbStack = dbStackService.getById(request.getResourceId());
        try {
            if (databaseMigrationRequired) {
                LOGGER.debug("Migration is required, new database server attributes: {}", migrationParams.getAttributes());
                databaseStack = upgradeMigrationService.mergeDatabaseStacks(dbStack, migrationParams, connector, cloudCredential,
                        cloudContext.getPlatformVariant());
            } else {
                LOGGER.debug("Migration was not needed, progressing with original databaseStack..");
            }
            performDbUpgrade(request, databaseStack, targetMajorVersion, cloudCredential, cloudContext, connector);

            if (databaseMigrationRequired) {
                updateDbStack(dbStack, targetMajorVersion, databaseStack);
                DatabaseServerConfig dbServerConfig = databaseServerConfigService.getByCrn(Crn.safeFromString(dbStack.getResourceCrn()))
                        .orElseThrow(() -> new IllegalStateException("Cannot find database server " + dbStack.getResourceCrn()));
                LOGGER.debug("Updating database server connection user name after database upgrade.");
                dbServerConfig.setConnectionUserName(dbServerConfig.getConnectionUserName().split("@")[0]);
                databaseServerConfigService.update(dbServerConfig);
            } else {
                updateDbVersionOnly(dbStack, targetMajorVersion);
            }
            response = new UpgradeDatabaseServerSuccess(request.getResourceId());
            LOGGER.debug("Successfully upgraded the database server {}", databaseStack);
        } catch (Exception e) {
            response = new RedbeamsUpgradeFailedEvent(request.getResourceId(), e);
            LOGGER.warn("Error upgrading the database server {}:", databaseStack, e);
        }
        return response;
    }

    private void performDbUpgrade(UpgradeDatabaseServerRequest request, DatabaseStack databaseStack, TargetMajorVersion targetMajorVersion,
            CloudCredential cloudCredential, CloudContext cloudContext, CloudConnector connector) throws Exception {
        AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
        List<CloudResource> cloudResources = dbResourceService.getAllAsCloudResource(request.getResourceId());
        connector.resources().upgradeDatabaseServer(ac, databaseStack, persistenceNotifier, targetMajorVersion, cloudResources);
    }

    private void updateDbStack(DBStack dbStack, TargetMajorVersion targetMajorVersion, DatabaseStack mergedDatabaseStack) {
        String originalTemplate = dbStack.getTemplate();
        String templateForUpgrade = mergedDatabaseStack.getTemplate();
        if (!StringUtils.equals(originalTemplate, templateForUpgrade)) {
            LOGGER.debug("There was a difference between the upgraded template and the original one, saving it now..");
            dbStack.setTemplate(templateForUpgrade);
        }
        dbStack.setMajorVersion(targetMajorVersion.convertToMajorVersion());
        DatabaseServer migratedDbServer = mergedDatabaseStack.getDatabaseServer();
        com.sequenceiq.redbeams.domain.stack.DatabaseServer persistedDatabaseServer = dbStack.getDatabaseServer();
        // CB-22658 is filed to make this more generic
        persistedDatabaseServer.setAttributes(new Json(migratedDbServer.getParameters()));
        persistedDatabaseServer.setInstanceType(migratedDbServer.getFlavor());
        persistedDatabaseServer.setStorageSize(migratedDbServer.getStorageSize());
        dbStackService.save(dbStack);
    }

    private void updateDbVersionOnly(DBStack dbStack, TargetMajorVersion targetMajorVersion) {
        dbStack.setMajorVersion(targetMajorVersion.convertToMajorVersion());
        dbStackService.save(dbStack);
    }
}
