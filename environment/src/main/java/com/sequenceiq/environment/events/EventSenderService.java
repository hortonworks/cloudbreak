package com.sequenceiq.environment.events;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.NOTIFICATION;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationDetails;
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
        CDPStructuredNotificationEvent cdpStructuredEvent = getStructuredEvent(resource, resourceEvent, payload);
        cdpDefaultStructuredEventClient.sendStructuredEvent(cdpStructuredEvent);
        notificationService.send(resourceEvent, payload, userCrn);
    }

    public void sendEventAndNotificationForMissingEnv(BaseNamedFlowEvent payload, ResourceEvent resourceEvent, String userCrn) {
        CDPStructuredNotificationEvent cdpStructuredEvent = createStructureEventForMissingEnvironment(payload, resourceEvent, userCrn);
        cdpDefaultStructuredEventClient.sendStructuredEvent(cdpStructuredEvent);
        notificationService.send(resourceEvent, payload, userCrn);
    }

    private CDPStructuredNotificationEvent getStructuredEvent(AccountAwareResource resource, ResourceEvent resourceEvent, Object payload) {
        String resourceType = resource.getClass().getSimpleName().toLowerCase();
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

        return new CDPStructuredNotificationEvent(operationDetails, notificationDetails);
    }

    private CDPStructuredNotificationEvent createStructureEventForMissingEnvironment(BaseNamedFlowEvent payload, ResourceEvent resourceEvent, String userCrn) {
        String resourceType = payload.getClass().getSimpleName().toLowerCase();
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
        return new CDPStructuredNotificationEvent(operationDetails, notificationDetails);
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
