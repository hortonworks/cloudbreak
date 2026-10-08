package com.sequenceiq.redbeams.events.notification;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.event.ResourceEvent;

@Service
public class DbStackNotificationTypePreparationService {

    public static final Set<ResourceEvent> TARGET_EVENTS = Set.of(
            ResourceEvent.REDBEAMS_EXTERNAL_DATABASE_STORAGE_LOW
    );

    public boolean isNotificationRequiredByStackStatus(ResourceEvent resourceEvent) {
        return TARGET_EVENTS.contains(resourceEvent);
    }
}
