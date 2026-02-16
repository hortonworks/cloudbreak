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
            NotificationGroupType.ENVIRONMENT,
            "b1417842-1eef-4d65-ac36-02a0e32d424e",
            "Cloudera Data Platform - Azure Default Outbound Warning: %s"
    ),
    STACK_PROVISIONING(
            "templates/stack/email/stack_health_template.ftl",
            Set.of(ChannelType.EMAIL),
            NotificationFormFactor.DISTRIBUTION_LIST,
            NotificationSeverity.WARNING,
            NotificationGroupType.ENVIRONMENT,
            "216b0ab7-f6ed-4e0a-a5d5-6a16a37d2e81",
            "Cloudera Data Platform - Provisioning Alert: %s"
    ),
    STACK_START_STOP(
            "templates/stack/email/stack_health_template.ftl",
            Set.of(ChannelType.EMAIL),
            NotificationFormFactor.DISTRIBUTION_LIST,
            NotificationSeverity.WARNING,
            NotificationGroupType.ENVIRONMENT,
            "216b0ab7-f6ed-4e0a-a5d5-6a16a37d2e83",
            "Cloudera Data Platform - Start/Stop Alert: %s"
    ),
    STACK_RESIZE(
            "templates/stack/email/stack_health_template.ftl",
            Set.of(ChannelType.EMAIL),
            NotificationFormFactor.DISTRIBUTION_LIST,
            NotificationSeverity.WARNING,
            NotificationGroupType.ENVIRONMENT,
            "216b0ab7-f6ed-4e0a-a5d5-6a16a37d2e85",
            "Cloudera Data Platform - Resize Alert: %s"
    ),
    STACK_UPGRADE(
            "templates/stack/email/stack_health_template.ftl",
            Set.of(ChannelType.EMAIL),
            NotificationFormFactor.DISTRIBUTION_LIST,
            NotificationSeverity.WARNING,
            NotificationGroupType.ENVIRONMENT,
            "216b0ab7-f6ed-4e0a-a5d5-6a16a37d2e86",
            "Cloudera Data Platform - Upgrade Alert: %s"
    ),
    STACK_REPAIR(
            "templates/stack/email/stack_health_template.ftl",
            Set.of(ChannelType.EMAIL),
            NotificationFormFactor.DISTRIBUTION_LIST,
            NotificationSeverity.WARNING,
            NotificationGroupType.ENVIRONMENT,
            "216b0ab7-f6ed-4e0a-a5d5-6a16a37d2e87",
            "Cloudera Data Platform - Repair Alert: %s"
    ),
    STACK_HEALTH(
            "templates/stack/email/stack_health_template.ftl",
            Set.of(ChannelType.EMAIL),
            NotificationFormFactor.DISTRIBUTION_LIST,
            NotificationSeverity.WARNING,
            NotificationGroupType.ENVIRONMENT,
            "216b0ab7-f6ed-4e0a-a5d5-6a16a37d2e82",
            "Cloudera Data Platform - Health Alert: %s"
    );

    private final String template;

    private final Set<ChannelType> channelTypes;

    private final NotificationFormFactor notificationFormFactor;

    private final NotificationSeverity notificationSeverity;

    private final String eventTypeId;

    private final String subjectTemplate;

    private NotificationGroupType groupType;

    @SuppressWarnings("ExecutableStatementCount")
    NotificationType(
            String template,
            Set<ChannelType> channelTypes,
            NotificationFormFactor notificationFormFactor,
            NotificationSeverity notificationSeverity,
            NotificationGroupType groupType,
            String eventTypeId,
            String subjectTemplate) {
        this.template = template;
        this.channelTypes = channelTypes;
        this.notificationFormFactor = notificationFormFactor;
        this.notificationSeverity = notificationSeverity;
        this.groupType = groupType;
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

    public NotificationGroupType getGroupType() {
        return groupType;
    }

    public static Set<String> getEventTypeIds(NotificationGroupType notificationGroupType) {
        return Arrays.stream(NotificationType.values())
                .filter(e -> e.groupType.equals(notificationGroupType))
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
                ", groupType=" + groupType +
                ", eventTypeId='" + eventTypeId + '\'' +
                ", subjectTemplate='" + subjectTemplate + '\'' +
                '}';
    }
}
