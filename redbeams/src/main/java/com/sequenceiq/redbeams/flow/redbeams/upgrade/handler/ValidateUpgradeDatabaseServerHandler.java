package com.sequenceiq.redbeams.flow.redbeams.upgrade.handler;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.RdsAutoMigrationException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.DeploymentType;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.dto.UpgradeDatabaseMigrationParams;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsValidateUpgradeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerSuccess;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.upgrade.DatabaseAutoMigrationUpdater;
import com.sequenceiq.redbeams.service.validation.DatabaseEncryptionValidator;

@Component
public class ValidateUpgradeDatabaseServerHandler extends ExceptionCatcherEventHandler<ValidateUpgradeDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateUpgradeDatabaseServerHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private DatabaseEncryptionValidator databaseEncryptionValidator;

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @Inject
    private DatabaseAutoMigrationUpdater databaseAutoMigrationUpdater;

    @Inject
    private ResourceNameGenerator nameGenerator;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ValidateUpgradeDatabaseServerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ValidateUpgradeDatabaseServerRequest> event) {
        RedbeamsValidateUpgradeFailedEvent failure = new RedbeamsValidateUpgradeFailedEvent(resourceId, e);
        LOGGER.warn("Error during database server upgrade validation:", e);
        return failure;
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ValidateUpgradeDatabaseServerRequest> handlerEvent) {
        Event<ValidateUpgradeDatabaseServerRequest> event = handlerEvent.getEvent();
        LOGGER.debug("Received event: {}", event);
        ValidateUpgradeDatabaseServerRequest request = event.getData();
        DatabaseStack databaseStack = request.getDatabaseStack();
        UpgradeDatabaseMigrationParams migrationParams = request.getMigrationParams();
        TargetMajorVersion targetMajorVersion = request.getTargetMajorVersion();
        CloudCredential cloudCredential = request.getCloudCredential();
        CloudContext cloudContext = request.getCloudContext();
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());

        Selectable response;
        DBStack dbStack = dbStackService.getById(request.getResourceId());

        try {
            // Validating encryption
            databaseEncryptionValidator.validateEncryptionDuringUpgrade(dbStack.getCloudPlatform(), dbStack.getEnvironmentId(),
                    databaseStack.getDatabaseServer(), targetMajorVersion);

            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            ResourceConnector resourceConnector = connector.resources();
            // General validations
            resourceConnector.validateUpgradeDatabaseServer(ac, databaseStack, targetMajorVersion);

            if (Objects.nonNull(migrationParams)) {
                DatabaseStack migratedDbStack = getMigratedDbStack(migrationParams, dbStack, connector, targetMajorVersion);
                // Canary deployment validations
                List<CloudResourceStatus> resourceList = resourceConnector.launchValidateUpgradeDatabaseServerResources(
                        ac, databaseStack, targetMajorVersion, migratedDbStack, persistenceNotifier);
                response = new ValidateUpgradeDatabaseServerSuccess(request.getResourceId(), resourceList);
            } else {
                LOGGER.info("No migration required, proceeding without launching canary database server.");
                response = new ValidateUpgradeDatabaseServerSuccess(request.getResourceId(), List.of());
            }

        } catch (RdsAutoMigrationException autoMigrationException) {
            databaseAutoMigrationUpdater.updateDatabaseIfAutoMigrationHappened(dbStack, autoMigrationException);
            response = new ValidateUpgradeDatabaseServerSuccess(request.getResourceId(), List.of(), autoMigrationException.getMessage());
        } catch (Exception ex) {
            LOGGER.warn("RDS upgrade validation failed on provider side", ex);
            response = new RedbeamsValidateUpgradeFailedEvent(request.getResourceId(), ex);
        }
        return response;
    }

    private DatabaseStack getMigratedDbStack(UpgradeDatabaseMigrationParams migrationParams, DBStack dbStack, CloudConnector connector,
            TargetMajorVersion targetMajorVersion) throws TemplatingNotSupportedException {
        DBStack migratedDbStack = dbStack.copy();
        DatabaseServer migratedDatabaseServer = configureMigratedDatabaseServer(migrationParams, dbStack, targetMajorVersion, migratedDbStack);
        migratedDbStack.setDatabaseServer(migratedDatabaseServer);
        DatabaseStack databaseStack = databaseStackConverter.convert(migratedDbStack);
        // DatabaseStack contains template, but it is also required (only the field AzureDatabaseType) to generate new template
        String newTemplate = connector.resources().getDBStackTemplate(databaseStack);

        DatabaseStack mergedDatabaseStack = new DatabaseStack(
                databaseStack.getNetwork(),
                databaseStack.getDatabaseServer(),
                databaseStack.getTags(),
                newTemplate);
        mergedDatabaseStack.setDeploymentType(DeploymentType.CANARY_TEST_DEPLOYMENT);
        return mergedDatabaseStack;
    }

    private DatabaseServer configureMigratedDatabaseServer(UpgradeDatabaseMigrationParams migrationParams, DBStack dbStack,
            TargetMajorVersion targetMajorVersion, DBStack migratedDbStack) {
        DatabaseServer migratedDatabaseServer = migratedDbStack.getDatabaseServer();
        migratedDatabaseServer.setName(nameGenerator.generateHashBasedName(APIResourceType.DATABASE_SERVER,
                Optional.of(dbStack.getResourceCrn() + targetMajorVersion.getMajorVersion())));
        migratedDatabaseServer.setAttributes(migrationParams.getAttributes());
        migratedDatabaseServer.setInstanceType(migrationParams.getInstanceType());
        migratedDatabaseServer.setRootUserName(migrationParams.getRootUserName());
        migratedDatabaseServer.setStorageSize(migrationParams.getStorageSize());
        return migratedDatabaseServer;
    }
}