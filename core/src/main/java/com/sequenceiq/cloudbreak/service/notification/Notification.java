package com.sequenceiq.cloudbreak.service.notification;

public class Notification<T> {
    private T notification;

    public Notification(T notification) {
        this.notification = notification;
    }

    public T getNotification() {
        return notification;
    }
}
