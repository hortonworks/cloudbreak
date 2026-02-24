package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Objects;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.common.model.JsonEntity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class StackNotificationV4Request implements JsonEntity {

    @Schema(description = ModelDescriptions.StackModelDescription.STACK_STATUS, requiredMode = REQUIRED)
    @NotNull
    private String status;

    @Schema(requiredMode = REQUIRED)
    @NotNull
    private String detailedStackStatus;

    @Schema
    private String statusReason;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetailedStackStatus() {
        return detailedStackStatus;
    }

    public void setDetailedStackStatus(String detailedStackStatus) {
        this.detailedStackStatus = detailedStackStatus;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StackNotificationV4Request that = (StackNotificationV4Request) o;
        return Objects.equals(status, that.status)
                &&  Objects.equals(detailedStackStatus, that.detailedStackStatus)
                && Objects.equals(statusReason, that.statusReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, detailedStackStatus, statusReason);
    }

    @Override
    public String toString() {
        return "StackNotificationRequest{" +
                "status=" + status +
                ", detailedStackStatus=" + detailedStackStatus +
                ", statusReason='" + statusReason + '\'' +
                '}';
    }
}
