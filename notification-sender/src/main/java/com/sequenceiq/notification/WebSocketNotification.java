package com.sequenceiq.notification;

public class WebSocketNotification<T> {
    private final T notification;

    public WebSocketNotification(T notification) {
        this.notification = notification;
    }

    public T getNotification() {
        return notification;
    }
}
