package com.sequenceiq.environment.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static java.lang.String.format;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

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
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.notification.WebSocketNotificationService;

@Service
public class EventSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventSenderService.class);

    private final WebSocketNotificationService webSocketNotificationService;

    private final EnvironmentResponseConverter environmentResponseConverter;

    private final CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    private final NodeConfig nodeConfig;

    private final String serviceVersion;

    private final CloudbreakMessagesService cloudbreakMessagesService;

    public EventSenderService(WebSocketNotificationService webSocketNotificationService, EnvironmentResponseConverter environmentResponseConverter,
            CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient, NodeConfig nodeConfig, @Value("${info.app.version:}") String serviceVersion,
            CloudbreakMessagesService cloudbreakMessagesService) {
        this.webSocketNotificationService = webSocketNotificationService;
        this.environmentResponseConverter = environmentResponseConverter;
        this.cdpDefaultStructuredEventClient = cdpDefaultStructuredEventClient;
        this.nodeConfig = nodeConfig;
        this.serviceVersion = serviceVersion;
        this.cloudbreakMessagesService = cloudbreakMessagesService;
    }

    public void sendEventAndNotification(EnvironmentDto environmentDto, String userCrn, ResourceEvent resourceEvent) {
        sendEventAndNotification(environmentDto, userCrn, resourceEvent, new HashSet<>());
    }

    public void sendEventAndNotification(EnvironmentDto environmentDto, String userCrn, ResourceEvent resourceEvent, Collection<?> messageArgs) {
        SimpleEnvironmentResponse simpleResponse = environmentResponseConverter.dtoToSimpleResponse(environmentDto, true, true);
        sendEventAndNotificationWithPayload(environmentDto, userCrn, resourceEvent, simpleResponse, messageArgs);
    }

    public void sendEventAndNotificationWithPayload(AccountAwareResource resource, String userCrn, ResourceEvent resourceEvent,
            Object payload) {
        sendEventAndNotificationWithPayload(resource, userCrn, resourceEvent, payload, new HashSet<>());
    }

    public void sendEventAndNotificationWithPayload(AccountAwareResource resource, String userCrn, ResourceEvent resourceEvent, Object payload,
            Collection<?> messageArgs) {
        CDPStructuredNotificationEvent cdpStructuredEvent = getStructuredEvent(resource, resourceEvent, payload, messageArgs);
        cdpDefaultStructuredEventClient.sendStructuredEvent(cdpStructuredEvent);
        webSocketNotificationService.send(resourceEvent, messageArgs, payload, userCrn, null);
    }

    public void sendEventAndNotificationForMissingEnv(BaseNamedFlowEvent payload, ResourceEvent resourceEvent, String userCrn) {
        CDPStructuredNotificationEvent cdpStructuredEvent = createStructureEventForMissingEnvironment(payload, resourceEvent, userCrn);
        cdpDefaultStructuredEventClient.sendStructuredEvent(cdpStructuredEvent);
        webSocketNotificationService.send(resourceEvent, payload, userCrn);
    }

    private CDPStructuredNotificationEvent getStructuredEvent(AccountAwareResource resource, ResourceEvent resourceEvent, Object payload,
            Collection<?> messageArgs) {
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
        String message = cloudbreakMessagesService.getMessage(resourceEvent.getMessage(), messageArgs);
        return new CDPStructuredNotificationEvent(operationDetails, notificationDetails, resourceEvent.name(), message);
    }

    private CDPStructuredNotificationEvent createStructureEventForMissingEnvironment(BaseNamedFlowEvent payload, ResourceEvent resourceEvent, String userCrn) {
        String resourceType = payload.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        String resourceCrn = payload.getResourceCrn();
        CDPOperationDetails operationDetails = new CDPOperationDetails(
                System.currentTimeMillis(),
                NOTIFICATION,
                resourceType,
                payload.getResourceId(),
                payload.getResourceName(),
                nodeConfig.getId(),
                serviceVersion,
                null,
                resourceCrn,
                userCrn,
                resourceCrn,
                resourceEvent.name());

        CDPStructuredNotificationDetails notificationDetails = getNotificationDetails(resourceEvent, resourceCrn, resourceType, payload);
        String message = cloudbreakMessagesService.getMessage(resourceEvent.getMessage());
        return new CDPStructuredNotificationEvent(operationDetails, notificationDetails, resourceEvent.name(), message);
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
