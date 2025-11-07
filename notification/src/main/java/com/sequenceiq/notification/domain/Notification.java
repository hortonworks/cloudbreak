package com.sequenceiq.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sequenceiq.notification.domain.converter.ChannelTypeConverter;
import com.sequenceiq.notification.domain.converter.NotificationFormFactorConverter;
import com.sequenceiq.notification.domain.converter.NotificationSeverityConverter;
import com.sequenceiq.notification.domain.converter.NotificationTypeConverter;

@Entity
@Table
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Convert(converter = NotificationSeverityConverter.class)
    private NotificationSeverity severity;

    @Column(nullable = false)
    @Convert(converter = NotificationTypeConverter.class)
    private NotificationType type;

    @Column(nullable = false)
    @Convert(converter = ChannelTypeConverter.class)
    private ChannelType channelType;

    @Column(nullable = false)
    private String resourceCrn;

    @Column(nullable = false)
    private String resourceName;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String accountId;

    private String message;

    @Column(nullable = false)
    private Long createdAt;

    private Long sentAt;

    @Column(nullable = false)
    private boolean sent;

    @Column(nullable = false)
    @Convert(converter = NotificationFormFactorConverter.class)
    private NotificationFormFactor formFactor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Long id;

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

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

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

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Notification build() {
            Notification notification = new Notification();
            notification.setId(id);
            notification.setResourceCrn(resourceCrn);
            notification.setResourceName(resourceName);
            notification.setName(resourceName);
            notification.setSeverity(severity);
            notification.setType(type);
            notification.setChannelType(channelType);
            notification.setFormFactor(formFactor);
            notification.setSentAt(sentAt);
            notification.setMessage(message);
            notification.setCreatedAt(createdAt == null ? System.currentTimeMillis() : createdAt);
            notification.setSent(sent);
            notification.setAccountId(accountId);
            notification.setName(name);
            return notification;
        }
    }
}
