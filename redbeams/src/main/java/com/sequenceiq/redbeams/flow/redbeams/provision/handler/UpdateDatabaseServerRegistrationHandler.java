package com.sequenceiq.redbeams.flow.redbeams.provision.handler;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationFailed;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationSuccess;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UpdateDatabaseServerRegistrationHandler implements EventHandler<UpdateDatabaseServerRegistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDatabaseServerRegistrationHandler.class);

    private static final long DEFAULT_WORKSPACE = 0L;

    @Inject
    private EventBus eventBus;

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private UserGeneratorService userGeneratorService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateDatabaseServerRegistrationRequest.class);
    }

    @Override
    public void accept(Event<UpdateDatabaseServerRegistrationRequest> event) {
        LOGGER.debug("Received event: {}", event);
        UpdateDatabaseServerRegistrationRequest request = event.getData();
        DBStack dbStack = request.getDBStack();
        List<CloudResource> dbResources = request.getDbResources();

        Selectable response;
        try {
            DatabaseServerConfig dbServerConfig = databaseServerConfigService.getByCrn(dbStack.getResourceCrn())
                    .orElseThrow(() -> new IllegalStateException("Cannot find database server " + dbStack.getResourceCrn()));
            CloudResource dbHostname = cloudResourceHelper.getResourceTypeFromList(ResourceType.RDS_HOSTNAME, dbResources)
                    .orElseThrow(() -> new IllegalStateException("DB hostname not found for allocated database."));
            CloudResource dbPort = cloudResourceHelper.getResourceTypeFromList(ResourceType.RDS_PORT, dbResources)
                    .orElseThrow(() -> new IllegalStateException("DB port not found for allocated database."));
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(dbStack.getCloudPlatform());

            String dbHostnameString = dbHostname.getName();
            dbServerConfig.setHost(dbHostnameString);
            dbServerConfig.setPort(Integer.parseInt(dbPort.getName()));
            String updatedUserName = userGeneratorService.updateUserName(dbServerConfig.getConnectionUserName(), Optional.of(cloudPlatform), dbHostnameString);
            dbServerConfig.setConnectionUserName(updatedUserName);

            databaseServerConfigService.update(dbServerConfig);
            response = new UpdateDatabaseServerRegistrationSuccess(request.getResourceId());
        } catch (Exception e) {
            response = new UpdateDatabaseServerRegistrationFailed(request.getResourceId(), e);
            LOGGER.warn("Error updating the database server registration:", e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
