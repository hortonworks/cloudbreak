package com.sequenceiq.redbeams.flow.redbeams.rotate.handler;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerFailed;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.rotate.event.SslCertRotateRedbeamsEvent;
import com.sequenceiq.redbeams.service.rotate.CloudProviderCertRotator;

@Component
public class SslCertRotateDatabaseServerHandler implements EventHandler<SslCertRotateDatabaseServerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslCertRotateDatabaseServerHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private PollTaskFactory statusCheckFactory;

    @Inject
    private SyncPollingScheduler<ExternalDatabaseStatus> externalDatabaseStatusPollingScheduler;

    @Inject
    private CloudProviderCertRotator cloudProviderCertRotator;

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
        try {
            cloudProviderCertRotator.rotate(request.getResourceId(), cloudContext, cloudCredential, databaseStack, request.isOnlyCertificateUpdate());
            SslCertRotateRedbeamsEvent success = new SslCertRotateDatabaseServerSuccess(
                    request.getResourceId(),
                    request.isOnlyCertificateUpdate());
            eventBus.notify(success.selector(), new Event<>(event.getHeaders(), success));
            LOGGER.debug("Rotating cert the database server successfully finished for {}", cloudContext);
        } catch (Exception e) {
            SslCertRotateDatabaseServerFailed failure =
                    new SslCertRotateDatabaseServerFailed(request.getResourceId(), e, request.isOnlyCertificateUpdate());
            LOGGER.warn("Error rotating cert the database server:", e);
            eventBus.notify(failure.selector(), new Event<>(event.getHeaders(), failure));
        }
    }

}
