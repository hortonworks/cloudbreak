package com.sequenceiq.redbeams.flow.redbeams.provision.handler;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationFailed;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationSuccess;
import com.sequenceiq.redbeams.service.UserGeneratorService;
import com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerConfigService;
import com.sequenceiq.redbeams.service.sslcertificate.DatabaseServerSslCertificateSyncService;

import reactor.bus.Event;

@Component
public class UpdateDatabaseServerRegistrationHandler extends ExceptionCatcherEventHandler<UpdateDatabaseServerRegistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDatabaseServerRegistrationHandler.class);

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private UserGeneratorService userGeneratorService;

    @Inject
    private DatabaseServerSslCertificateSyncService databaseServerSslCertificateSyncService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateDatabaseServerRegistrationRequest.class);
    }

    @Override
    protected Selectable doAccept(HandlerEvent handlerEvent) {
        Event<UpdateDatabaseServerRegistrationRequest> event = handlerEvent.getEvent();
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

            CloudContext cloudContext = request.getCloudContext();
            databaseServerSslCertificateSyncService.syncSslCertificateIfNeeded(cloudContext, request.getCloudCredential(), dbStack, request.getDatabaseStack());

            response = new UpdateDatabaseServerRegistrationSuccess(request.getResourceId());
            LOGGER.debug("Database server registration update successfully finished for {}", cloudContext);
        } catch (Exception e) {
            response = new UpdateDatabaseServerRegistrationFailed(request.getResourceId(), e);
            LOGGER.warn("Error updating the database server registration:", e);
        }
        return response;
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateDatabaseServerRegistrationRequest> event) {
        UpdateDatabaseServerRegistrationFailed failure = new UpdateDatabaseServerRegistrationFailed(resourceId, e);
        LOGGER.warn("Error updating the database server registration:", e);
        return failure;
    }

}
