package com.sequenceiq.redbeams.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static java.lang.String.format;

import java.util.List;

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
import com.sequenceiq.redbeams.domain.stack.DBStack;

/**
 * Pushes user-facing notifications from redbeams to the Management Console UI, over the websocket notification transport
 * ({@link WebSocketNotificationService}), and persists a matching {@link CDPStructuredNotificationEvent} via
 * {@link CDPDefaultStructuredEventClient} so the alert is also retained in the CDP structured-event store (audit/notification history).
 * redbeams has no logged-in user in its background pollers, so the DB stack owner CRN is used as the recipient of the UI notification.
 */
@Service
public class RedbeamsEventSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsEventSenderService.class);

    private final WebSocketNotificationService webSocketNotificationService;

    private final CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    private final NodeConfig nodeConfig;

    private final CloudbreakMessagesService cloudbreakMessagesService;

    private final String serviceVersion;

    public RedbeamsEventSenderService(
            WebSocketNotificationService webSocketNotificationService,
            CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient,
            NodeConfig nodeConfig,
            CloudbreakMessagesService cloudbreakMessagesService,
            @Value("${info.app.version:}") String serviceVersion) {
        this.webSocketNotificationService = webSocketNotificationService;
        this.cdpDefaultStructuredEventClient = cdpDefaultStructuredEventClient;
        this.nodeConfig = nodeConfig;
        this.cloudbreakMessagesService = cloudbreakMessagesService;
        this.serviceVersion = serviceVersion;
    }

    public void sendStorageLowNotification(DBStack dbStack, Double freeStoragePercentage) {
        String ownerCrn = dbStack.getOwnerCrn() == null ? null : dbStack.getOwnerCrn().toString();
        String formattedPercentage = String.format("%.1f", freeStoragePercentage);
        ResourceEvent resourceEvent = ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW;
        List<String> messageArgs = List.of(dbStack.getName(), formattedPercentage);
        ExternalDatabaseStorageAlert payload = new ExternalDatabaseStorageAlert(
                dbStack.getResourceCrn(), dbStack.getName(), dbStack.getEnvironmentId(), freeStoragePercentage);
        LOGGER.info("Sending low-storage notification for external database {} ({}% free) to owner {}.",
                dbStack.getResourceCrn(), formattedPercentage, ownerCrn);
        storeStructuredNotificationEvent(dbStack, resourceEvent, payload, messageArgs);
        webSocketNotificationService.send(resourceEvent, messageArgs, payload, ownerCrn, null);
    }

    private void storeStructuredNotificationEvent(DBStack dbStack, ResourceEvent resourceEvent, ExternalDatabaseStorageAlert payload,
            List<String> messageArgs) {
        try {
            CDPStructuredNotificationEvent structuredEvent = createStructuredNotificationEvent(dbStack, resourceEvent, payload, messageArgs);
            cdpDefaultStructuredEventClient.sendStructuredEvent(structuredEvent);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to store CDP structured notification event for external database {}: {}", dbStack.getResourceCrn(), e.getMessage(), e);
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
                resourceEvent.name());
        CDPStructuredNotificationDetails notificationDetails = new CDPStructuredNotificationDetails(
                resourceEvent, resourceCrn, resourceType, serializePayload(payload, resourceEvent, resourceCrn, resourceType));
        String message = cloudbreakMessagesService.getMessage(resourceEvent.getMessage(), messageArgs);
        return new CDPStructuredNotificationEvent(operationDetails, notificationDetails, resourceEvent.name(), message);
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
