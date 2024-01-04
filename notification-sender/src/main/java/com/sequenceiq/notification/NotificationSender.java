package com.sequenceiq.notification;

import java.util.List;

import jakarta.ws.rs.client.Client;

public interface NotificationSender {
    <T> void send(Notification<T> notification, List<String> endpoints, Client restClient);
}
