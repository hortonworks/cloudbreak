package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.domain.Notification;
import com.sequenceiq.periscope.rest.json.NotificationJson;

@Component
public class NotificationConverter extends AbstractConverter<NotificationJson, Notification> {

    @Override
    public Notification convert(NotificationJson source) {
        Notification notification = new Notification();
        notification.setTarget(source.getTarget());
        notification.setType(source.getNotificationType());
        return notification;
    }

    @Override
    public NotificationJson convert(Notification source) {
        NotificationJson json = new NotificationJson();
        json.setNotificationType(source.getType());
        json.setTarget(source.getTarget());
        return json;
    }
}
