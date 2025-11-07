package com.sequenceiq.notification.generator.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;

public class NotificationGeneratorDto<T extends BaseNotificationRegisterAdditionalDataDtos> {

    private String name;

    private String accountId;

    private String resourceCrn;

    private String resourceName;

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
                ", additionalData=" + additionalData +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, accountId, resourceCrn, resourceName, additionalData, channelMessages);
    }

    public static NotificationGeneratorDto.Builder builder() {
        return new NotificationGeneratorDto.Builder();
    }

    public static class Builder<T extends BaseNotificationRegisterAdditionalDataDtos> {

        private String name;

        private String accountId;

        private String resourceCrn;

        private String resourceName;

        private T additionalData;

        private Map<ChannelType, String> channelMessages = new HashMap<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
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

        public Builder additionalData(T additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public Builder channelMessages(Map<ChannelType, String> channelMessages) {
            this.channelMessages = channelMessages;
            return this;
        }

        public Builder notificationGeneratorDto(NotificationGeneratorDto dto) {
            this.resourceCrn = dto.getResourceCrn();
            this.name = dto.getName();
            this.accountId = dto.getAccountId();
            this.additionalData = (T) dto.getAdditionalData();
            return this;
        }

        public Builder addChannelMessage(ChannelType channelType, String channelMessages) {
            if (this.channelMessages == null) {
                this.channelMessages = new HashMap<>();
            }
            this.channelMessages.put(channelType, channelMessages);
            return this;
        }

        public NotificationGeneratorDto build() {
            NotificationGeneratorDto notification = new NotificationGeneratorDto();
            notification.setResourceCrn(resourceCrn);
            notification.setAccountId(accountId);
            notification.setResourceName(StringUtils.isEmpty(resourceName) ? name : resourceName);
            notification.setName(name);
            notification.setChannelMessages(channelMessages);
            notification.setAdditionalData(additionalData);
            return notification;
        }
    }
}
