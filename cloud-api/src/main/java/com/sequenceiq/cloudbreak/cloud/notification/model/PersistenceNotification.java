package com.sequenceiq.cloudbreak.cloud.notification.model;

/**
 * Marks an object as holder of information that can be saved/retrieved through the event based persistence system.
 *
 * @param <T> the type of the payload of the notification
 */
public interface PersistenceNotification<T> {
    void operationCompleted(T persistenceInfo);
}
