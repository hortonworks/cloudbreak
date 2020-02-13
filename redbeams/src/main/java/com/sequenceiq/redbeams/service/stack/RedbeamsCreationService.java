package com.sequenceiq.redbeams.service.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.common.cost.CostTagging;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.exception.BadRequestException;
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

    @Inject
    private CostTagging costTagging;

    public DBStack launchDatabaseServer(DBStack dbStack) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create called with: {}", dbStack);
        }

        if (dbStackService.findByNameAndEnvironmentCrn(dbStack.getName(), dbStack.getEnvironmentId()).isPresent()) {
            throw new BadRequestException("A stack for this database server already exists in the environment");
        }

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
        } catch (TemplatingDoesNotSupportedException e) {
            throw new RedbeamsException("Failed to retrieve database stack template for cloud platform", e);
        }

        if (dbStack.getDatabaseServer().getConnectionDriver() == null) {
            String connectionDriver = dbStack.getDatabaseServer().getDatabaseVendor().connectionDriver();
            dbStack.getDatabaseServer().setConnectionDriver(connectionDriver);
            LOGGER.info("Database server allocation request lacked a connection driver; defaulting to {}", connectionDriver);
        }

        DBStack savedStack = dbStackService.save(dbStack);

        registerDatabaseServerConfig(savedStack);

        flowManager.notify(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.selector(),
                new RedbeamsEvent(RedbeamsProvisionEvent.REDBEAMS_PROVISION_EVENT.selector(), dbStack.getId()));
        return savedStack;
    }

    private void registerDatabaseServerConfig(DBStack dbStack) {
        DatabaseServer databaseServer = dbStack.getDatabaseServer();
        DatabaseServerConfig dbServerConfig = new DatabaseServerConfig();

        dbServerConfig.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        dbServerConfig.setAccountId(databaseServer.getAccountId());
        dbServerConfig.setName(dbStack.getName());
        dbServerConfig.setDescription(dbStack.getDescription());
        dbServerConfig.setEnvironmentId(dbStack.getEnvironmentId());
        dbServerConfig.setConnectionDriver(dbStack.getDatabaseServer().getConnectionDriver());
        // username and password are set during conversion to DBStack
        dbServerConfig.setConnectionUserName(dbStack.getDatabaseServer().getRootUserName());
        dbServerConfig.setConnectionPassword(dbStack.getDatabaseServer().getRootPassword());
        dbServerConfig.setDatabaseVendor(dbStack.getDatabaseServer().getDatabaseVendor());
        dbServerConfig.setDbStack(dbStack);
        // host and port are set after allocation is complete, so leave as null
        dbServerConfig.setResourceCrn(dbStack.getResourceCrn());

        databaseServerConfigService.create(dbServerConfig, DEFAULT_WORKSPACE, false);
    }
}
