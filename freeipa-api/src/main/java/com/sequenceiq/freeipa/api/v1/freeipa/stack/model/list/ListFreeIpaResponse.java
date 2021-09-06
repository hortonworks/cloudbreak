package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ListFreeIpaV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListFreeIpaResponse {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @ApiModelProperty(value = FreeIpaModelDescriptions.FREEIPA_NAME, required = true)
    private String name;

    @NotNull
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
