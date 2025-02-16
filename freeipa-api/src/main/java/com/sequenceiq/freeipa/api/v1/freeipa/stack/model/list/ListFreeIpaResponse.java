package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ListFreeIpaV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListFreeIpaResponse {

    @NotNull
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentCrn;

    @NotNull
    @Schema(description = FreeIpaModelDescriptions.FREEIPA_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String crn;

    private String domain;

    private Status status;

    private String statusString;

    private AvailabilityStatus availabilityStatus;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusString() {
        return statusString;
    }

    public void setStatusString(String statusString) {
        this.statusString = statusString;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    @Override
    public String toString() {
        return "ListFreeIpaResponse{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", name='" + name + '\'' +
                ", crn='" + crn + '\'' +
                ", domain='" + domain + '\'' +
                ", status=" + status +
                ", statusString='" + statusString + '\'' +
                ", availabilityStatus=" + availabilityStatus +
                '}';
    }
}
