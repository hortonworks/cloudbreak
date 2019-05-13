package com.sequenceiq.notification;

import java.util.List;

import javax.ws.rs.client.Client;

public interface NotificationSender {
    <T> void send(Notification<T> notification, List<String> endpoints, Client restClient);

//    void sendTestNotification(String userId, List<String> endpoints, Client restClient);
}
