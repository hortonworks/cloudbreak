package com.sequenceiq.notification;

import java.util.Collection;
import java.util.Collections;

import jakarta.inject.Inject;

import com.sequenceiq.cloudbreak.event.ResourceEvent;

public abstract class NotificationController {

    @Inject
    private NotificationService notificationService;

    protected final void notify(ResourceEvent resourceEvent) {
        notify(resourceEvent, Collections.emptySet(), null);
    }

    protected final void notify(ResourceEvent resourceEvent, Object payload) {
        notify(resourceEvent, Collections.emptySet(), payload);
    }

    protected final void notify(ResourceEvent resourceEvent, Collection<?> messageArgs, Object payload) {
        notificationService.send(resourceEvent, messageArgs, payload);
    }
}
