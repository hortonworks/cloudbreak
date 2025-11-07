package com.sequenceiq.notification.domain;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Set;

public enum NotificationType {
    AZURE_DEFAULT_OUTBOUND(
            "templates/environment/email/azure_default_outbound_template.ftl",
            Set.of(ChannelType.EMAIL),
            NotificationFormFactor.DISTRIBUTION_LIST,
            NotificationSeverity.WARNING,
            "b1417842-1eef-4d65-ac36-02a0e32d424e",
            "Cloudera Data Platform - Azure Default Outbound Warning: %s"
    );

    private final String template;

    private final Set<ChannelType> channelTypes;

    private final NotificationFormFactor notificationFormFactor;

    private final NotificationSeverity notificationSeverity;

    private final String eventTypeId;

    private final String subjectTemplate;

    NotificationType(
            String template,
            Set<ChannelType> channelTypes,
            NotificationFormFactor notificationFormFactor,
            NotificationSeverity notificationSeverity,
            String eventTypeId,
            String subjectTemplate) {
        this.template = template;
        this.channelTypes = channelTypes;
        this.notificationFormFactor = notificationFormFactor;
        this.notificationSeverity = notificationSeverity;
        this.eventTypeId = eventTypeId;
        this.subjectTemplate = subjectTemplate;
    }

    public String getTemplate() {
        return template;
    }

    public Set<ChannelType> getChannelTypes() {
        return channelTypes;
    }

    public NotificationFormFactor getNotificationFormFactor() {
        return notificationFormFactor;
    }

    public NotificationSeverity getNotificationSeverity() {
        return notificationSeverity;
    }

    public String getEventTypeId() {
        return eventTypeId;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public static Set<String> getEventTypeIds() {
        return Arrays.stream(NotificationType.values())
                .map(NotificationType::getEventTypeId)
                .collect(toSet());
    }

    public static Set<NotificationType> getByChannelType(ChannelType channelType) {
        return Set.of(values()).stream()
                .filter(type -> type.getChannelTypes().contains(channelType))
                .collect(toSet());
    }

    @Override
    public String toString() {
        return "NotificationType{" +
                "template='" + template + '\'' +
                ", channelTypes=" + channelTypes +
                ", notificationFormFactor=" + notificationFormFactor +
                ", notificationSeverity=" + notificationSeverity +
                ", eventTypeId='" + eventTypeId + '\'' +
                ", subjectTemplate='" + subjectTemplate + '\'' +
                '}';
    }
}
