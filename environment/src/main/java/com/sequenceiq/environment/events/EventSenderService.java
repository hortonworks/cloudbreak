package com.sequenceiq.environment.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.NotificationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResource;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;
import com.sequenceiq.flow.ha.NodeConfig;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.notification.NotificationService;

@Service
public class EventSenderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSenderService.class);

    private final NotificationService notificationService;

    private final EnvironmentResponseConverter environmentResponseConverter;

    private final CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    private final NodeConfig nodeConfig;

    private final String serviceVersion;

    public EventSenderService(NotificationService notificationService, EnvironmentResponseConverter environmentResponseConverter,
            CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient, NodeConfig nodeConfig, @Value("${info.app.version:}") String serviceVersion) {
        this.notificationService = notificationService;
        this.environmentResponseConverter = environmentResponseConverter;
        this.cdpDefaultStructuredEventClient = cdpDefaultStructuredEventClient;
        this.nodeConfig = nodeConfig;
        this.serviceVersion = serviceVersion;
    }

    public void sendEventAndNotification(EnvironmentDto environmentDto, String userCrn, ResourceEvent resourceEvent) {
        SimpleEnvironmentResponse simpleResponse = environmentResponseConverter.dtoToSimpleResponse(environmentDto);
        sendEventAndNotificationWithPayload(environmentDto, userCrn, resourceEvent, simpleResponse);
    }

    public void sendEventAndNotificationWithPayload(AccountAwareResource resource, String userCrn, ResourceEvent resourceEvent, Object payload) {
        CDPStructuredNotificationEvent cdpStructuredEvent = createStructureEvent(resource, resourceEvent, payload);
        cdpDefaultStructuredEventClient.sendStructuredEvent(cdpStructuredEvent);
        notificationService.send(resourceEvent, payload, userCrn);
    }

    public void sendEventAndNotificationForMissingEnv(BaseNamedFlowEvent payload, ResourceEvent resourceEvent, String userCrn) {
        CDPStructuredNotificationEvent cdpStructuredEvent = createStructureEventForMissingEnvironment(payload, resourceEvent, userCrn);
        cdpDefaultStructuredEventClient.sendStructuredEvent(cdpStructuredEvent);
        notificationService.send(resourceEvent, payload, userCrn);
    }

    @NotNull
    private CDPStructuredNotificationEvent createStructureEvent(AccountAwareResource resource, ResourceEvent resourceEvent, Object payload) {
        CDPOperationDetails operationDetails = new CDPOperationDetails(
                System.currentTimeMillis(),
                NOTIFICATION,
                CDPStructuredNotificationEvent.class.getSimpleName(),
                resource.getId(),
                resource.getName(),
                nodeConfig.getId(),
                serviceVersion,
                resource.getAccountId(),
                resource.getResourceCrn(),
                resource.getCreator(),
                resource.getResourceCrn(),
                null);
        NotificationDetails notificationDetails = createNotificationDetails(resourceEvent);

        return new CDPStructuredNotificationEvent(operationDetails, notificationDetails);
    }

    @NotNull
    private CDPStructuredNotificationEvent createStructureEventForMissingEnvironment(BaseNamedFlowEvent payload, ResourceEvent resourceEvent, String userCrn) {
        CDPOperationDetails operationDetails = new CDPOperationDetails(
                System.currentTimeMillis(),
                NOTIFICATION,
                CDPStructuredNotificationEvent.class.getSimpleName(),
                payload.getResourceId(),
                payload.getResourceName(),
                nodeConfig.getId(),
                serviceVersion,
                null,
                payload.getResourceCrn(),
                userCrn,
                payload.getResourceCrn(),
                null);

        //TODO create generalized NotificationDetails for new CDP domain that is generic enough
        NotificationDetails notificationDetails = createNotificationDetails(resourceEvent);

        return new CDPStructuredNotificationEvent(operationDetails, notificationDetails);
    }

    private NotificationDetails createNotificationDetails(ResourceEvent resourceEvent) {
        //TODO create generalized NotificationDetails for new CDP domain that is generic enough
        NotificationDetails notificationDetails = new NotificationDetails();
        notificationDetails.setNotificationType(resourceEvent.name());
        notificationDetails.setNotification(resourceEvent.getMessage());
        return notificationDetails;
    }
}
