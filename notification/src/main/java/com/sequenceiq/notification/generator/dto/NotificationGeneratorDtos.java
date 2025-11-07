package com.sequenceiq.notification.generator.dto;

import java.util.Collection;

import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;

public class NotificationGeneratorDtos<T extends BaseNotificationRegisterAdditionalDataDtos>  {

    private Collection<NotificationGeneratorDto<T>> notifications;

    private NotificationType notificationType;

    public NotificationGeneratorDtos(Collection<NotificationGeneratorDto<T>> notifications, NotificationType notificationType) {
        this.notifications = notifications;
        this.notificationType = notificationType;
    }

    public Collection<NotificationGeneratorDto<T>> getNotifications() {
        return notifications;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    @Override
    public String toString() {
        return "NotificationGeneratorDtos{" +
                "notifications=" + notifications +
                ", notificationType=" + notificationType +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder<T extends BaseNotificationRegisterAdditionalDataDtos>  {

        private Collection<NotificationGeneratorDto<T>> notifications;

        private NotificationType notificationType;

        public Builder notification(Collection<NotificationGeneratorDto<T>> notifications) {
            this.notifications = notifications;
            return this;
        }

        public Builder notificationType(NotificationType notificationType) {
            this.notificationType = notificationType;
            return this;
        }

        public NotificationGeneratorDtos build() {
            return new NotificationGeneratorDtos(this.notifications, this.notificationType);
        }
    }

}
