package com.sequenceiq.cloudbreak.notification.client.dto;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.SeverityType;

public record PublishEventForResourceRequestDto(
        String resourceCrn,
        String eventType,
        String title,
        String message,
        SeverityType.Value severity
) {

}