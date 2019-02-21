package com.sequenceiq.cloudbreak.notification;

public class Notification<T> {
    private final T notification;

    public Notification(T notification) {
        this.notification = notification;
    }

    public T getNotification() {
        return notification;
    }
}
