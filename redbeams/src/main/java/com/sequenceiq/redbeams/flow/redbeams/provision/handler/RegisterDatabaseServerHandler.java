package com.sequenceiq.redbeams.flow.redbeams.provision.handler;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.RegisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.RegisterDatabaseServerSuccess;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;
import com.sequenceiq.redbeams.service.crn.CrnService;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RegisterDatabaseServerHandler implements EventHandler<RegisterDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterDatabaseServerHandler.class);

    private static final long DEFAULT_WORKSPACE = 0L;

    @Inject
    private EventBus eventBus;

    @Inject
    private CrnService crnService;

    @Inject
    private DatabaseServerConfigRepository databaseServerConfigRepository;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RegisterDatabaseServerRequest.class);
    }

    @Override
    public void accept(Event<RegisterDatabaseServerRequest> event) {
        RegisterDatabaseServerRequest request = event.getData();
        Selectable response = new RegisterDatabaseServerSuccess(request.getResourceId());
        DBStack dbStack = request.getDBStack();
        List<CloudResource> dbResources = request.getDbResources();

        DatabaseServer databaseServer = dbStack.getDatabaseServer();
        DatabaseServerConfig dbServerConfig = new DatabaseServerConfig();

        // TODO: Adjust workspace to something non-default when and if necessary
        dbServerConfig.setWorkspaceId(DEFAULT_WORKSPACE);
        dbServerConfig.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        dbServerConfig.setAccountId(databaseServer.getAccountId());
        dbServerConfig.setName(dbStack.getName());
        dbServerConfig.setEnvironmentId(dbStack.getEnvironmentId());
        // TODO: Since the postgres driver is built in, we don't have to worry about setting the driver info.
        //       However, we'll need to address this when adding in new types of database servers.
        //dbServerConfig.setConnectionDriver("POSTGRESQL");
        //dbServerConfig.setConnectorJarUrl("");
        dbServerConfig.setConnectionUserName(dbStack.getDatabaseServer().getRootUserName());
        dbServerConfig.setConnectionPassword(dbStack.getDatabaseServer().getRootPassword());
        dbServerConfig.setDatabaseVendor(dbStack.getDatabaseServer().getDatabaseVendor());
        dbServerConfig.setPort(databaseServer.getPort());

        Optional<CloudResource> dbHostname = CloudResource.getResourceTypeFromList(ResourceType.RDS_HOSTNAME, dbResources);
        if (dbHostname.isEmpty()) {
            throw new IllegalStateException("DB hostname not found for allocated database.");
        }

        Optional<CloudResource> dbPort = CloudResource.getResourceTypeFromList(ResourceType.RDS_PORT, dbResources);
        if (dbPort.isEmpty()) {
            throw new IllegalStateException("DB port not found for allocated database.");
        }

        dbServerConfig.setHost(dbHostname.get().getName());
        dbServerConfig.setPort(Integer.parseInt(dbPort.get().getName()));
        dbServerConfig.setResourceCrn(crnService.createCrn(dbServerConfig));

        databaseServerConfigRepository.save(dbServerConfig);

        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
