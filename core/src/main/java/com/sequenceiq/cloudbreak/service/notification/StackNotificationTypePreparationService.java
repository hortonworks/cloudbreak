package com.sequenceiq.cloudbreak.service.notification;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.notification.domain.NotificationType;

@Service
public class StackNotificationTypePreparationService {

    public NotificationType notificationType(Status newStatus) {
        return newStatus.getNotificationType();
    }

    public boolean isNotificationRequiredByStackStatus(Status status) {
        return status.shouldTriggerNotification();
    }

    public boolean isNotificationRequiredByInstanceStatus(InstanceStatus status) {
        return status.isInstanceNotificationRequired();
    }

    public Set<InstanceStatus> instanceNotificationTargets() {
        return Arrays.stream(InstanceStatus.values())
                .filter(InstanceStatus::isInstanceNotificationRequired)
                .collect(Collectors.toSet());
    }

}
