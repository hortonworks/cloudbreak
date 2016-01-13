package com.sequenceiq.periscope.rest.json;

import com.sequenceiq.periscope.domain.NotificationType;

public class NotificationJson implements Json {

    private String[] target;
    private NotificationType notificationType;

    public String[] getTarget() {
        return target;
    }

    public void setTarget(String[] target) {
        this.target = target;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }
}
