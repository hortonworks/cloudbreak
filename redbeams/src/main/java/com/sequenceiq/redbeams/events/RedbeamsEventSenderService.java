package com.sequenceiq.redbeams.events;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW;
import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static com.sequenceiq.redbeams.events.notification.DBEmailDto.storageAlert;
import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.notification.WebSocketNotificationService;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.events.notification.DBEmailDto;
import com.sequenceiq.redbeams.events.notification.DbStackEmailNotificationService;

/**
 * Pushes user-facing notifications from redbeams to the Management Console UI, over the websocket notification transport
 * ({@link WebSocketNotificationService}), and persists a matching {@link CDPStructuredNotificationEvent} via
 * {@link CDPDefaultStructuredEventClient} so the alert is also retained in the CDP structured-event store (audit/notification history).
 * redbeams has no logged-in user in its background pollers, so the DB stack owner CRN is used as the recipient of the UI notification.
 * When the database server config is known, an email is also sent through {@link DbStackEmailNotificationService}. Each delivery channel
 * handles its own failures, so a problem with one never prevents the others.
 */
@Service
public class RedbeamsEventSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsEventSenderService.class);

    private final WebSocketNotificationService webSocketNotificationService;

    private final CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    private final NodeConfig nodeConfig;

    private final CloudbreakMessagesService cloudbreakMessagesService;

    private final DbStackEmailNotificationService dbStackEmailNotificationService;

    private final String serviceVersion;

    public RedbeamsEventSenderService(
            WebSocketNotificationService webSocketNotificationService,
            CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient,
            NodeConfig nodeConfig,
            CloudbreakMessagesService cloudbreakMessagesService,
            DbStackEmailNotificationService dbStackEmailNotificationService,
            @Value("${info.app.version:}") String serviceVersion) {
        this.webSocketNotificationService = webSocketNotificationService;
        this.cdpDefaultStructuredEventClient = cdpDefaultStructuredEventClient;
        this.nodeConfig = nodeConfig;
        this.cloudbreakMessagesService = cloudbreakMessagesService;
        this.dbStackEmailNotificationService = dbStackEmailNotificationService;
        this.serviceVersion = serviceVersion;
    }

    public void sendStorageLowNotification(Optional<DatabaseServerConfig> databaseServerConfig, DBStack dbStack, Double freeStoragePercentage) {
        String ownerCrn = dbStack.getOwnerCrnAsString();
        String formattedPercentage = String.format("%.1f", freeStoragePercentage);
        ResourceEvent resourceEvent = REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW;
        List<String> messageArgs = List.of(dbStack.getName(), formattedPercentage);
        ExternalDatabaseStorageAlert payload = new ExternalDatabaseStorageAlert(
                dbStack.getResourceCrn(), dbStack.getName(), dbStack.getEnvironmentId(), freeStoragePercentage);
        LOGGER.info("Sending low-storage notification for external database {} ({}% free) to owner {}.",
                dbStack.getResourceCrn(), formattedPercentage, ownerCrn);
        CDPStructuredNotificationEvent structuredEvent = createStructuredNotificationEvent(dbStack, resourceEvent, payload, messageArgs);
        storeStructuredNotificationEvent(dbStack, structuredEvent);
        sendWebSocketNotification(dbStack, resourceEvent, payload, messageArgs, ownerCrn);
        databaseServerConfig.ifPresentOrElse(
                config -> sendEmailNotification(config, dbStack, storageAlert(), structuredEvent),
                () -> LOGGER.info("No database server config found for external database {}, skipping low-storage email notification.",
                        dbStack.getResourceCrn()));
    }

    private void storeStructuredNotificationEvent(DBStack dbStack, CDPStructuredNotificationEvent structuredEvent) {
        try {
            cdpDefaultStructuredEventClient.sendStructuredEvent(structuredEvent);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to store CDP structured notification event for external database {}: {}", dbStack.getResourceCrn(), e.getMessage(), e);
        }
    }

    private void sendWebSocketNotification(DBStack dbStack, ResourceEvent resourceEvent, ExternalDatabaseStorageAlert payload, List<String> messageArgs,
            String ownerCrn) {
        try {
            webSocketNotificationService.send(resourceEvent, messageArgs, payload, ownerCrn, null);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to send websocket notification for external database {}: {}", dbStack.getResourceCrn(), e.getMessage(), e);
        }
    }

    private void sendEmailNotification(DatabaseServerConfig databaseServerConfig, DBStack dbStack, DBEmailDto dbEmailDto,
            CDPStructuredNotificationEvent structuredEvent) {
        try {
            dbStackEmailNotificationService.notify(databaseServerConfig, dbStack, dbEmailDto, structuredEvent);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to send email notification for external database {}: {}", dbStack.getResourceCrn(), e.getMessage(), e);
        }
    }

    private CDPStructuredNotificationEvent createStructuredNotificationEvent(DBStack dbStack, ResourceEvent resourceEvent,
            ExternalDatabaseStorageAlert payload, List<String> messageArgs) {
        String resourceCrn = dbStack.getResourceCrn();
        String resourceType = CloudbreakEventService.REDBEAMS_RESOURCE_TYPE;
        CDPOperationDetails operationDetails = new CDPOperationDetails(
                System.currentTimeMillis(),
                NOTIFICATION,
                resourceType,
                dbStack.getId(),
                dbStack.getName(),
                nodeConfig.getId(),
                serviceVersion,
                dbStack.getAccountId(),
                resourceCrn,
                ThreadBasedUserCrnProvider.getUserCrn(),
                dbStack.getEnvironmentId(),
                resourceEvent.name()
        );
        return new CDPStructuredNotificationEvent(
                operationDetails,
                getNotificationDetails(resourceEvent, payload, resourceCrn, resourceType),
                resourceEvent.name(),
                cloudbreakMessagesService.getMessage(resourceEvent.getMessage(), messageArgs)
        );
    }

    private CDPStructuredNotificationDetails getNotificationDetails(
            ResourceEvent resourceEvent,
            ExternalDatabaseStorageAlert payload,
            String resourceCrn,
            String resourceType
    ) {
        return new CDPStructuredNotificationDetails(
                resourceEvent, resourceCrn, resourceType, serializePayload(payload, resourceEvent, resourceCrn, resourceType));
    }

    private String serializePayload(ExternalDatabaseStorageAlert payload, ResourceEvent resourceEvent, String resourceCrn, String resourceType) {
        try {
            return new Gson().toJson(payload);
        } catch (RuntimeException re) {
            String msg = format("CDPStructuredNotificationDetails' payload couldn't be serialized with ResourceEvent[%s], resource type[%s], CRN[%s]",
                    resourceEvent.name(), resourceType, resourceCrn);
            LOGGER.warn(msg, re);
            return msg;
        }
    }
}
