package com.sequenceiq.notification;

import java.util.List;

import jakarta.ws.rs.client.Client;

public interface WebSocketNotificationSender {
    <T> void send(WebSocketNotification<T> webSocketNotification, List<String> endpoints, Client restClient);
}
