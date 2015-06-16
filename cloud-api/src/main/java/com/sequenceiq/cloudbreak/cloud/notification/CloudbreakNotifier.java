package com.sequenceiq.cloudbreak.cloud.notification;

public interface CloudbreakNotifier<T> {
    void notify(T event);
}
