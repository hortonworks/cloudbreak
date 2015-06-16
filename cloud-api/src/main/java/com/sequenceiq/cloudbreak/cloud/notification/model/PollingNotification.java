package com.sequenceiq.cloudbreak.cloud.notification.model;

/**
 * Implementers are subject of event-based polling.
 *
 * @param <T> the type of the payload of the notification
 */
public interface PollingNotification<T> {
    T pollingInfo();

    void operationCompleted(T pollingInfo);
}
