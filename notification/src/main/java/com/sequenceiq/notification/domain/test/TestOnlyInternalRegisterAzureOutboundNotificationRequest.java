package com.sequenceiq.notification.domain.test;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.notification.domain.NotificationType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TestOnlyInternalRegisterAzureOutboundNotificationRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestOnlyInternalRegisterAzureOutboundNotificationRequest {

    @NotEmpty
    private String accountId;

    @NotEmpty
    private String environmentCrn;

    @NotEmpty
    private String environmentName;

    @NotNull
    private NotificationType notificationType;

    @NotNull
    private List<TestOnlyInternalAzureOutboundNotificationDatahubRequest> datahubs = new ArrayList<>();

    public List<TestOnlyInternalAzureOutboundNotificationDatahubRequest> getDatahubs() {
        return datahubs;
    }

    public void setDatahubs(List<TestOnlyInternalAzureOutboundNotificationDatahubRequest> datahubs) {
        this.datahubs = datahubs;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return "TestOnlyInternalRegisterAzureOutboundNotificationRequest{" +
                "accountId='" + accountId + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", environmentName='" + environmentName + '\'' +
                ", notificationType=" + notificationType +
                ", datahubs=" + datahubs +
                '}';
    }
}
