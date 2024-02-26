package com.sequenceiq.externalizedcompute.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalizedComputeClusterResponse extends ExternalizedComputeClusterBase {

    private String liftieClusterName;

    private String liftieClusterCrn;

    private ExternalizedComputeClusterApiStatus status;

    private String statusReason;

    public String getLiftieClusterName() {
        return liftieClusterName;
    }

    public void setLiftieClusterName(String liftieClusterName) {
        this.liftieClusterName = liftieClusterName;
    }

    public String getLiftieClusterCrn() {
        return liftieClusterCrn;
    }

    public void setLiftieClusterCrn(String liftieClusterCrn) {
        this.liftieClusterCrn = liftieClusterCrn;
    }

    public ExternalizedComputeClusterApiStatus getStatus() {
        return status;
    }

    public void setStatus(ExternalizedComputeClusterApiStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeClusterResponse{" +
                "liftieClusterName='" + liftieClusterName + '\'' +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                "} " + super.toString();
    }
}
