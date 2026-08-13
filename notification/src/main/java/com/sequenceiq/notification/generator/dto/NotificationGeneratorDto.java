package com.sequenceiq.notification.generator.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;

public class NotificationGeneratorDto<T extends BaseNotificationRegisterAdditionalDataDtos> {

    private String name;

    private String accountId;

    private String resourceCrn;

    private String resourceName;

    private NotificationSeverity severity;

    private T additionalData;

    private Map<ChannelType, String> channelMessages = new HashMap<>();

    public String getName() {
        return name;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Map<ChannelType, String> getChannelMessages() {
        return channelMessages;
    }

    public NotificationSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(NotificationSeverity severity) {
        this.severity = severity;
    }

    public void setChannelMessages(Map<ChannelType, String> channelMessages) {
        this.channelMessages = channelMessages;
    }

    public T getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(T additionalData) {
        this.additionalData = additionalData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotificationGeneratorDto that = (NotificationGeneratorDto) o;
        return Objects.equals(name, that.name)
                && Objects.equals(accountId, that.accountId)
                && Objects.equals(resourceCrn, that.resourceCrn)
                && Objects.equals(resourceName, that.resourceName)
                && Objects.equals(severity, that.severity)
                && Objects.equals(additionalData, that.additionalData)
                && Objects.equals(channelMessages, that.channelMessages);
    }

    @Override
    public String toString() {
        return "NotificationGeneratorDto{" +
                "name='" + name + '\'' +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", resourceName='" + resourceName + '\'' +
                ", severity='" + severity + '\'' +
                ", additionalData=" + additionalData +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, accountId, resourceCrn, resourceName, additionalData, channelMessages, severity);
    }

    public static NotificationGeneratorDto.Builder builder() {
        return new NotificationGeneratorDto.Builder();
    }

    public static class Builder<T extends BaseNotificationRegisterAdditionalDataDtos> {

        private String name;

        private String accountId;

        private String resourceCrn;

        private String resourceName;

        private NotificationSeverity severity;

        private T additionalData;

        private Map<ChannelType, String> channelMessages = new HashMap<>();

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder<T> resourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder<T> resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder<T> additionalData(T additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public Builder<T> channelMessages(Map<ChannelType, String> channelMessages) {
            this.channelMessages = channelMessages;
            return this;
        }

        public Builder<T> severity(NotificationSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder<T> notificationGeneratorDto(NotificationGeneratorDto<T> dto) {
            this.resourceCrn = dto.getResourceCrn();
            this.name = dto.getName();
            this.accountId = dto.getAccountId();
            this.additionalData = (T) dto.getAdditionalData();
            this.severity = dto.getSeverity();
            return this;
        }

        public Builder<T> addChannelMessage(ChannelType channelType, String channelMessages) {
            if (this.channelMessages == null) {
                this.channelMessages = new HashMap<>();
            }
            this.channelMessages.put(channelType, channelMessages);
            return this;
        }

        public NotificationGeneratorDto<T> build() {
            NotificationGeneratorDto<T> notification = new NotificationGeneratorDto<>();
            notification.setResourceCrn(resourceCrn);
            notification.setAccountId(accountId);
            notification.setResourceName(StringUtils.isEmpty(resourceName) ? name : resourceName);
            notification.setName(name);
            notification.setChannelMessages(channelMessages);
            notification.setAdditionalData(additionalData);
            notification.setSeverity(severity);
            return notification;
        }
    }
}
