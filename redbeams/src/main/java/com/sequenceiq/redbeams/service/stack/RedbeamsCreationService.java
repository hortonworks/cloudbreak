package com.sequenceiq.redbeams.service.stack;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

@Service
public class RedbeamsCreationService {

    // TODO: Adjust workspace to something non-default when and if necessary
    @VisibleForTesting
    static final long DEFAULT_WORKSPACE = 0L;

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsCreationService.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Inject
    private RedbeamsFlowManager flowManager;

    public DBStack launchDatabaseServer(DBStack dbStack, String clusterCrn) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create called with: {}", dbStack);
        }

        if (dbStackService.findByNameAndEnvironmentCrn(dbStack.getName(), dbStack.getEnvironmentId()).isPresent()) {
            throw new BadRequestException("A stack for this database server already exists in the environment");
        }

        Optional<DatabaseServerConfig> optionalDBServerConfig =
                databaseServerConfigService.findByEnvironmentCrnAndClusterCrn(dbStack.getEnvironmentId(), clusterCrn);

        DBStack savedDbStack;
        boolean startFlow = false;
        if (optionalDBServerConfig.isEmpty()) {
            LOGGER.debug("DataBaseServerConfig is not available by cluster crn '{}'", clusterCrn);
            savedDbStack = saveDbStackAndRegisterDatabaseServerConfig(dbStack, clusterCrn);
            startFlow = true;
        } else {
            DatabaseServerConfig dbServerConfig = optionalDBServerConfig.get();
            if (dbServerConfig.getDbStack().isEmpty()) {
                LOGGER.debug("DBStack is not available in DatabaseServerConfig '{}'", dbServerConfig.getResourceCrn());
                savedDbStack = saveDbStackInDatabaseServerConfig(dbServerConfig, dbStack);
                startFlow = true;
            } else {
                savedDbStack = dbServerConfig.getDbStack().get();
            }
        }

        if (startFlow) {
            flowManager.notify(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.selector(),
                    new RedbeamsEvent(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.selector(), savedDbStack.getId()));
        }

        return savedDbStack;
    }

    private DBStack saveDbStackAndRegisterDatabaseServerConfig(DBStack dbStack, String clusterCrn) {
        DBStack savedStack = saveDbStack(dbStack);
        registerDatabaseServerConfig(savedStack, clusterCrn);

        return savedStack;
    }

    private DBStack saveDbStackInDatabaseServerConfig(DatabaseServerConfig databaseServerConfig,  DBStack dbStack) {
        DBStack savedDbStack = saveDbStack(dbStack);
        databaseServerConfig.setDbStack(savedDbStack);
        databaseServerConfigService.update(databaseServerConfig);

        return savedDbStack;
    }

    private DBStack saveDbStack(DBStack dbStack) {
        // possible future change is to use a flow here (GetPlatformTemplateRequest, modified for database server)
        // for now, just get it synchronously / within this thread, it ought to be quick
        CloudPlatformVariant platformVariant = new CloudPlatformVariant(dbStack.getCloudPlatform(), dbStack.getPlatformVariant());
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.get(platformVariant);
            if (connector == null) {
                throw new RedbeamsException("Failed to find cloud connector for platform variant " + platformVariant);
            }
            String template = connector.resources().getDBStackTemplate();
            if (template == null) {
                throw new RedbeamsException("No database stack template is available for platform variant " + platformVariant);
            }
            dbStack.setTemplate(template);
        } catch (TemplatingNotSupportedException e) {
            throw new RedbeamsException("Failed to retrieve database stack template for cloud platform", e);
        }

        DatabaseServer databaseServer = dbStack.getDatabaseServer();
        if (databaseServer.getConnectionDriver() == null) {
            String connectionDriver = databaseServer.getDatabaseVendor().connectionDriver();
            databaseServer.setConnectionDriver(connectionDriver);
            LOGGER.info("Database server allocation request lacked a connection driver; defaulting to {}", connectionDriver);
        }

        return dbStackService.save(dbStack);
    }

    private void registerDatabaseServerConfig(DBStack dbStack, String clusterCrn) {
        DatabaseServer databaseServer = dbStack.getDatabaseServer();
        DatabaseServerConfig dbServerConfig = new DatabaseServerConfig();

        dbServerConfig.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        dbServerConfig.setAccountId(databaseServer.getAccountId());
        dbServerConfig.setName(dbStack.getName());
        dbServerConfig.setDescription(dbStack.getDescription());
        dbServerConfig.setEnvironmentId(dbStack.getEnvironmentId());
        dbServerConfig.setConnectionDriver(databaseServer.getConnectionDriver());
        // username and password are set during conversion to DBStack
        dbServerConfig.setConnectionUserName(databaseServer.getRootUserName());
        dbServerConfig.setConnectionPassword(databaseServer.getRootPassword());
        dbServerConfig.setDatabaseVendor(databaseServer.getDatabaseVendor());
        dbServerConfig.setDbStack(dbStack);
        // host and port are set after allocation is complete, so leave as null
        dbServerConfig.setResourceCrn(dbStack.getResourceCrn());
        dbServerConfig.setClusterCrn(clusterCrn);

        databaseServerConfigService.create(dbServerConfig, DEFAULT_WORKSPACE, false);
    }

}
