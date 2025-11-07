package com.sequenceiq.notification.domain.test;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TestOnlyInternalAzureOutboundNotificationDatahubRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestOnlyInternalAzureOutboundNotificationDatahubRequest {

    @NotEmpty
    private String datahubName;

    @NotEmpty
    private String datahubCrn;

    @NotEmpty
    private String creatorName;

    @NotEmpty
    private String status;

    @NotEmpty
    public String getDatahubName() {
        return datahubName;
    }

    public void setDatahubName(String datahubName) {
        this.datahubName = datahubName;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getDatahubCrn() {
        return datahubCrn;
    }

    public void setDatahubCrn(String datahubCrn) {
        this.datahubCrn = datahubCrn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "TestOnlyInternalAzureOutboundNotificationDatahubRequest{" +
                "datahubName='" + datahubName + '\'' +
                ", datahubCrn='" + datahubCrn + '\'' +
                ", creatorName='" + creatorName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
