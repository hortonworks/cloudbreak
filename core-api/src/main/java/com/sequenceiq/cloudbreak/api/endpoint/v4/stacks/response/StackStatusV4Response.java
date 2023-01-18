package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.api.type.CertExpirationState;

import io.swagger.v3.oas.annotations.media.Schema;

public class StackStatusV4Response {

    @Schema
    private Long id;

    @Schema
    private Status status;

    @Schema
    private String statusReason;

    @Schema
    private Status clusterStatus;

    @Schema
    private String clusterStatusReason;

    @Schema
    private String crn;

    @Schema(description = ClusterModelDescription.CERT_EXPIRATION)
    private CertExpirationState certExpirationState;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Status getClusterStatus() {
        return clusterStatus;
    }

    public void setClusterStatus(Status clusterStatus) {
        this.clusterStatus = clusterStatus;
    }

    public String getClusterStatusReason() {
        return clusterStatusReason;
    }

    public void setClusterStatusReason(String clusterStatusReason) {
        this.clusterStatusReason = clusterStatusReason;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public CertExpirationState getCertExpirationState() {
        return certExpirationState;
    }

    public void setCertExpirationState(CertExpirationState certExpirationState) {
        this.certExpirationState = certExpirationState;
    }

    @Override
    public String toString() {
        return "StackStatusV4Response{" +
                "id=" + id +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", clusterStatus=" + clusterStatus +
                ", clusterStatusReason='" + clusterStatusReason + '\'' +
                ", crn='" + crn + '\'' +
                ", certExpirationState=" + certExpirationState +
                '}';
    }
}
