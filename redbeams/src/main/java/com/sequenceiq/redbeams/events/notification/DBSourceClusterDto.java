package com.sequenceiq.redbeams.events.notification;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;

public record DBSourceClusterDto(
        String sourceClusterName,
        String sourceClusterCrn,
        StackType stackType,
        NotificationState notificationState) {
}
