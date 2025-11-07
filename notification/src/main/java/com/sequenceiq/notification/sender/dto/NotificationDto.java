package com.sequenceiq.notification.sender.dto;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.domain.NotificationFormFactor;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;

public class NotificationDto {

    private NotificationSeverity severity;

    private NotificationType type;

    private ChannelType channelType;

    private String resourceCrn;

    private String resourceName;

    private String name;

    private String accountId;

    private String message;

    private Long createdAt;

    private Long sentAt;

    private boolean sent;

    private NotificationFormFactor formFactor;

    private String metadata;

    private Long id;

    public NotificationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(NotificationSeverity severity) {
        this.severity = severity;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getSentAt() {
        return sentAt;
    }

    public void setSentAt(Long sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public NotificationFormFactor getFormFactor() {
        return formFactor;
    }

    public void setFormFactor(NotificationFormFactor formFactor) {
        this.formFactor = formFactor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public boolean isValidNotification(NotificationFormFactor formFactor) {
        return getFormFactor() != null && getFormFactor() == formFactor && getResourceCrn() != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "NotificationDto{" +
                ", severity=" + severity +
                ", type=" + type +
                ", channelType=" + channelType +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", name='" + name + '\'' +
                ", accountId='" + accountId + '\'' +
                ", createdAt=" + createdAt +
                ", sentAt=" + sentAt +
                ", sent=" + sent +
                ", formFactor=" + formFactor +
                ", id=" + id +
                '}';
    }

    public static NotificationDto.Builder builder() {
        return new NotificationDto.Builder();
    }

    public static class Builder {

        private NotificationSeverity severity;

        private NotificationType type;

        private String resourceCrn;

        private String resourceName;

        private String message;

        private Long createdAt;

        private Long sentAt;

        private boolean sent;

        private ChannelType channelType;

        private NotificationFormFactor formFactor;

        private String metadata;

        private String name;

        private String accountId;

        private Long id;

        public Builder severity(NotificationSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder type(NotificationType type) {
            this.type = type;
            return this;
        }

        public Builder resourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder created(Long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder sent(boolean sent) {
            this.sent = sent;
            return this;
        }

        public Builder channelType(ChannelType channelType) {
            this.channelType = channelType;
            return this;
        }

        public Builder notificationFormFactor(NotificationFormFactor formFactor) {
            this.formFactor = formFactor;
            return this;
        }

        public Builder sentAt(Long sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public Builder metadata(String metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder notification(Notification notification) {
            this.resourceCrn = notification.getResourceCrn();
            this.resourceName = notification.getResourceName();
            this.severity = notification.getSeverity();
            this.type = notification.getType();
            this.channelType = notification.getChannelType();
            this.formFactor = notification.getFormFactor();
            this.sentAt = notification.getSentAt();
            this.message = notification.getMessage();
            this.createdAt = notification.getCreatedAt();
            this.sent = notification.isSent();
            this.accountId = notification.getAccountId();
            this.name = notification.getName();
            this.id = notification.getId();
            return this;
        }

        public NotificationDto build() {
            NotificationDto notification = new NotificationDto();
            notification.setResourceCrn(resourceCrn);
            notification.setResourceName(resourceName);
            notification.setSeverity(severity);
            notification.setType(type);
            notification.setChannelType(channelType);
            notification.setFormFactor(formFactor);
            notification.setSentAt(sentAt);
            notification.setMessage(message);
            notification.setCreatedAt(createdAt == null ? System.currentTimeMillis() : createdAt);
            notification.setMetadata(metadata);
            notification.setSent(sent);
            notification.setAccountId(accountId);
            notification.setName(name);
            notification.setId(id);
            return notification;
        }
    }
}