package com.sequenceiq.periscope.notification;

import java.util.Date;
import java.util.List;

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
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.Subscription;
import com.sequenceiq.periscope.repository.SubscriptionRepository;

@Service
public class HttpNotificationSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotificationSender.class);

    @Inject
    private SubscriptionRepository subscriptionRepository;

    private Client restClient = RestClientUtil.get(new ConfigKey(false, false));

    public void send(History history) {
        send(convertHistory(history));
    }

    private Notification convertHistory(History history) {
        Notification n = new Notification();
        n.setEventType("PERISCOPE_HISTORY");
        n.setHistoryRecord(history);
        n.setEventTimestamp(new Date());
        n.setOwner(history.getUserId());
        return n;
    }

    public void send(Notification notification) {
        List<Subscription> subscriptions = (List<Subscription>) subscriptionRepository.findAll();
        for (Subscription subscription : subscriptions) {
            String endpoint = subscription.getEndpoint();
            try {
                restClient
                        .target(endpoint)
                        .request()
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .post(Entity.json(notification), String.class);
            } catch (Exception ex) {
                LOGGER.info("Could not send notification to the specified endpoint: '{}' Cause: {}", endpoint, ex.getMessage());
            }
        }
    }
}
