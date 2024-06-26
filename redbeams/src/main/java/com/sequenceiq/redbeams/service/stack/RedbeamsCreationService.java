package com.sequenceiq.redbeams.service.stack;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParametersBase;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.exception.RedbeamsException;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.TriggerRedbeamsProvisionEvent;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

@Service
public class RedbeamsCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsCreationService.class);

    private static final long DEFAULT_WORKSPACE = 0L;

    private static final int MAXIMUM_DB_SERVERS_FOR_CLUSTER = 2;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Inject
    private RedbeamsFlowManager flowManager;

    @Inject
    private DBStackToDatabaseStackConverter databaseStackConverter;

    public DBStack launchDatabaseServer(DBStack dbStack, String clusterCrn, ProviderParametersBase networkParameters) {
        MDCBuilder.buildMdcContext(dbStack);
        validateDbDoesNotExist(dbStack);
        Optional<DatabaseServerConfig> optionalDBServerConfig = databaseServerConfigService.findByClusterCrn(dbStack.getEnvironmentId(), clusterCrn);
        if (optionalDBServerConfig.isEmpty()) {
            LOGGER.debug("DataBaseServerConfig is not available by cluster crn '{}'", clusterCrn);
            DBStack savedDbStack = saveDbStackAndRegisterDatabaseServerConfig(dbStack, clusterCrn);
            triggerProvisionFlow(savedDbStack, networkParameters);
            return savedDbStack;
        } else {
            DatabaseServerConfig dbServerConfig = optionalDBServerConfig.get();
            if (dbServerConfig.getDbStack().isEmpty()) {
                LOGGER.debug("DBStack is not available in DatabaseServerConfig '{}'", dbServerConfig.getResourceCrn());
                DBStack savedDbStack = saveDbStackInDatabaseServerConfig(dbServerConfig, dbStack);
                triggerProvisionFlow(savedDbStack, networkParameters);
                return savedDbStack;
            } else {
                LOGGER.debug("No DB provision has been launched");
                return dbServerConfig.getDbStack().get();
            }
        }
    }

    public DBStack launchNonUniqueDatabaseServer(DBStack dbStack, String clusterCrn, ProviderParametersBase networkParameters) {
        MDCBuilder.buildMdcContext(dbStack);
        validateDbDoesNotExist(dbStack);
        List<DatabaseServerConfig> configs = databaseServerConfigService.findByEnvironmentCrnAndClusterCrn(dbStack.getEnvironmentId(), clusterCrn);
        if (configs.size() < MAXIMUM_DB_SERVERS_FOR_CLUSTER) {
            DBStack savedDbStack = saveDbStackAndRegisterDatabaseServerConfig(dbStack, clusterCrn);
            triggerProvisionFlow(savedDbStack, networkParameters);
            return savedDbStack;
        } else {
            throw new BadRequestException(String.format("You cannot create additional database servers for this cluster because you currently have %d servers,"
                    + " exceeding the maximum limit of %d.", configs.size(), MAXIMUM_DB_SERVERS_FOR_CLUSTER));
        }
    }

    private void triggerProvisionFlow(DBStack savedDbStack, ProviderParametersBase networkParameters) {
        LOGGER.info("Trigger DB provision for {}", savedDbStack);
        flowManager.notify(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.selector(),
                new TriggerRedbeamsProvisionEvent(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.selector(), savedDbStack.getId(), networkParameters));
    }

    private void validateDbDoesNotExist(DBStack dbStack) {
        if (dbStackService.findByNameAndEnvironmentCrn(dbStack.getName(), dbStack.getEnvironmentId()).isPresent()) {
            throw new BadRequestException("A stack for this database server already exists in the environment");
        }
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
        CloudPlatformVariant platformVariant = new CloudPlatformVariant(dbStack.getCloudPlatform(), dbStack.getPlatformVariant());
        try {
            CloudConnector connector = cloudPlatformConnectors.get(platformVariant);
            DatabaseStack databaseStack = databaseStackConverter.convert(dbStack);
            if (connector == null) {
                throw new RedbeamsException("Failed to find cloud connector for platform variant " + platformVariant);
            }
            String template = connector.resources().getDBStackTemplate(databaseStack);
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
        dbServerConfig.setResourceCrn(Crn.safeFromString(dbStack.getResourceCrn()));
        dbServerConfig.setClusterCrn(clusterCrn);

        databaseServerConfigService.create(dbServerConfig, DEFAULT_WORKSPACE);
    }

}
