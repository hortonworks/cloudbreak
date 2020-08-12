package com.sequenceiq.periscope.notification;

import java.time.Instant;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterResponse;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.converter.DistroXAutoscaleClusterResponseConverter;
import com.sequenceiq.periscope.converter.HistoryConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.Subscription;
import com.sequenceiq.periscope.repository.SubscriptionRepository;

@Service
public class HttpNotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotificationSender.class);

    @Inject
    private SubscriptionRepository subscriptionRepository;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private HistoryConverter historyConverter;

    @Inject
    private DistroXAutoscaleClusterResponseConverter autoscaleClusterResponseConverter;

    private final Client restClient = RestClientUtil.get(new ConfigKey(false, false, false));

    public void sendHistoryUpdateNotification(History historyUpdate, Cluster cluster) {
        Notification historyNotification = new Notification();
        historyNotification.setTenantName(cluster.getClusterPertain().getTenant());
        historyNotification.setEventType(NotificationType.AUTOSCALE_HISTORY_UPDATE.name());
        historyNotification.setEventTimestamp(historyUpdate.getTimestamp());
        historyNotification.setEventMessage(
                messagesService.getMessage(MessageCode.NOTIFICATION_HISTORY_UPDATE));

        AutoscaleClusterHistoryResponse historyResponse = historyConverter.convert(historyUpdate);
        historyNotification.setPayload(historyResponse);
        historyNotification.setPayloadType(historyResponse.getClass().getSimpleName());
        send(historyNotification);
    }

    public void sendConfigUpdateNotification(Cluster configUpdate) {
        Notification configUpdateNotification = new Notification();
        configUpdateNotification.setTenantName(configUpdate.getClusterPertain().getTenant());
        configUpdateNotification.setEventType(NotificationType.AUTOSCALE_CONFIG_UPDATE.name());
        configUpdateNotification.setEventTimestamp(Instant.now().toEpochMilli());
        configUpdateNotification.setEventMessage(
                messagesService.getMessage(MessageCode.NOTIFICATION_AS_CONFIG_UPDATE));

        DistroXAutoscaleClusterResponse asResponse = autoscaleClusterResponseConverter.convert(configUpdate);
        configUpdateNotification.setPayload(asResponse);
        configUpdateNotification.setPayloadType(asResponse.getClass().getSimpleName());
        send(configUpdateNotification);
    }

    private void send(Notification notification) {
        Iterable<Subscription> subscriptions = subscriptionRepository.findAll();
        for (Subscription subscription : subscriptions) {
            String endpoint = subscription.getEndpoint();
            try {
                restClient
                        .target(endpoint)
                        .request()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .post(Entity.json(notification), String.class);
            } catch (Exception ex) {
                LOGGER.warn("Could not send notification to the specified endpoint: '{}' Cause: {}", endpoint, ex.getMessage());
            }
        }
    }
}
