package com.sequenceiq.datalake.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static java.lang.String.format;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Service
public class EventSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSenderService.class);

    private final SdxClusterDtoConverter sdxClusterDtoConverter;

    private final CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    private final NodeConfig nodeConfig;

    private final String serviceVersion;

    private final CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private SdxService sdxService;

    public EventSenderService(SdxClusterDtoConverter sdxClusterDtoConverter,
            CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient, NodeConfig nodeConfig, @Value("${info.app.version:}") String serviceVersion,
            CloudbreakMessagesService cloudbreakMessagesService) {
        this.sdxClusterDtoConverter = sdxClusterDtoConverter;
        this.cdpDefaultStructuredEventClient = cdpDefaultStructuredEventClient;
        this.nodeConfig = nodeConfig;
        this.serviceVersion = serviceVersion;
        this.cloudbreakMessagesService = cloudbreakMessagesService;
    }

    public void notifyEvent(SdxContext context, ResourceEvent resourceEvent) {
        SdxCluster sdxCluster = sdxService.getById(context.getSdxId());
        if (sdxCluster != null) {
            sendEventAndNotification(sdxCluster, resourceEvent, List.of(sdxCluster.getName()));
        }
    }

    public void notifyEvent(SdxCluster sdxCluster, ResourceEvent resourceEvent) {
        if (sdxCluster != null) {
            sendEventAndNotification(sdxCluster, resourceEvent);
        }
    }

    public void sendEventAndNotification(SdxCluster sdxCluster, ResourceEvent resourceEvent) {
        sendEventAndNotification(sdxCluster, resourceEvent, new HashSet<>());
    }

    public void sendEventAndNotification(SdxCluster sdxCluster, ResourceEvent resourceEvent, Collection<?> messageArgs) {

        SdxClusterDto sdxClusterToDto = sdxClusterDtoConverter.sdxClusterToDto(sdxCluster);
        sendEventAndNotificationWithPayload(sdxClusterToDto, resourceEvent, sdxClusterToDto, messageArgs, null);
    }

    public void sendEventAndNotificationWithMessage(SdxCluster sdxCluster, ResourceEvent resourceEvent, String message) {
        SdxClusterDto sdxClusterToDto = sdxClusterDtoConverter.sdxClusterToDto(sdxCluster);
        sendEventAndNotificationWithPayload(sdxClusterToDto, resourceEvent, sdxClusterToDto, Collections.emptyList(), message);
    }

    public void sendEventAndNotificationWithPayload(AccountAwareResource resource, ResourceEvent resourceEvent, Object payload,
            Collection<?> messageArgs, String message) {
        CDPStructuredNotificationEvent cdpStructuredEvent = getStructuredEvent(resource, resourceEvent, payload, messageArgs, message);
        cdpDefaultStructuredEventClient.sendStructuredEvent(cdpStructuredEvent);
    }

    private CDPStructuredNotificationEvent getStructuredEvent(AccountAwareResource resource, ResourceEvent resourceEvent, Object payload,
            Collection<?> messageArgs, String message) {
        String resourceType = resource.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        String resourceCrn = resource.getResourceCrn();
        CDPOperationDetails operationDetails = new CDPOperationDetails(
                System.currentTimeMillis(),
                NOTIFICATION,
                resourceType,
                resource.getId(),
                resource.getName(),
                nodeConfig.getId(),
                serviceVersion,
                resource.getAccountId(),
                resourceCrn,
                ThreadBasedUserCrnProvider.getUserCrn(),
                resourceCrn,
                resourceEvent.name());
        CDPStructuredNotificationDetails notificationDetails = getNotificationDetails(resourceEvent, resourceCrn, resourceType, payload);
        String eventMessage = message;
        if (message == null) {
            eventMessage = cloudbreakMessagesService.getMessage(resourceEvent.getMessage(), messageArgs);
        }
        return new CDPStructuredNotificationEvent(operationDetails, notificationDetails, resourceEvent.name(), eventMessage);
    }

    private CDPStructuredNotificationDetails getNotificationDetails(ResourceEvent resourceEvent, String resourceCrn, String resourceType,
            Object payload) {
        String serializedPayload;
        try {
            serializedPayload = new Gson().toJson(payload);
            LOGGER.debug("CDPStructuredNotificationDetails' payload has been serialized with ResourceEvent[{}], resource type[{}], CRN[{}]",
                    resourceEvent.name(), resourceType, resourceCrn);
        } catch (RuntimeException re) {
            serializedPayload = format("CDPStructuredNotificationDetails' payload couldn't be serialized with ResourceEvent[%s], resource type[%s], CRN[%s]",
                    resourceEvent.name(), resourceType, resourceCrn);
            LOGGER.warn(serializedPayload, re);
        }
        return new CDPStructuredNotificationDetails(resourceEvent, resourceCrn, resourceType, serializedPayload);
    }
}
