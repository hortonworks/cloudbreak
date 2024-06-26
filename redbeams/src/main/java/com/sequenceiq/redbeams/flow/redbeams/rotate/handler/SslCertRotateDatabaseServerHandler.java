package com.sequenceiq.redbeams.flow.redbeams.rotate.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateRedbeamsEvent;
import com.sequenceiq.redbeams.service.rotate.CloudProviderCertRotator;
import com.sequenceiq.redbeams.service.stack.DBStackUpdater;

@Component
public class SslCertRotateDatabaseServerHandler implements EventHandler<SslCertRotateDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslCertRotateDatabaseServerHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private CloudProviderCertRotator cloudProviderCertRotator;

    @Inject
    private DBStackUpdater dbStackUpdater;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SslCertRotateDatabaseServerRequest.class);
    }

    @Override
    public void accept(Event<SslCertRotateDatabaseServerRequest> event) {
        LOGGER.debug("Received event: {}", event);
        SslCertRotateDatabaseServerRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        DatabaseStack databaseStack = request.getDatabaseStack();
        Long resourceId = request.getResourceId();
        try {
            cloudProviderCertRotator.rotate(resourceId, cloudContext, cloudCredential, databaseStack, request.isOnlyCertificateUpdate());
            dbStackUpdater.updateSslConfig(
                    resourceId,
                    cloudContext,
                    cloudCredential,
                    databaseStack);
            SslCertRotateRedbeamsEvent success = new SslCertRotateDatabaseServerSuccess(
                    resourceId,
                    request.isOnlyCertificateUpdate());
            eventBus.notify(success.selector(), new Event<>(event.getHeaders(), success));
            LOGGER.debug("Rotating cert the database server successfully finished for {}", cloudContext);
        } catch (Exception e) {
            SslCertRotateDatabaseServerFailed failure =
                    new SslCertRotateDatabaseServerFailed(resourceId, e, request.isOnlyCertificateUpdate());
            LOGGER.warn("Error rotating cert the database server:", e);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }

}
