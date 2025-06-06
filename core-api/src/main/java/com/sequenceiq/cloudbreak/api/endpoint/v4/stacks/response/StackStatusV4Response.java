package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.ProviderSyncState;

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

    @Schema(description = ClusterModelDescription.CERT_EXPIRATION_DETAILS)
    private String certExpirationDetails;

    @Schema(description = ModelDescriptions.StackModelDescription.PROVIDER_SYNC_STATES)
    private Set<ProviderSyncState> providerSyncStates = new HashSet<>();

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

    public String getCertExpirationDetails() {
        return certExpirationDetails;
    }

    public void setCertExpirationDetails(String certExpirationDetails) {
        this.certExpirationDetails = certExpirationDetails;
    }

    public Set<ProviderSyncState> getProviderSyncStates() {
        return providerSyncStates;
    }

    public void setProviderSyncStates(Set<ProviderSyncState> providerSyncStates) {
        this.providerSyncStates = providerSyncStates;
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
                ", certExpirationState=" + certExpirationState + '\'' +
                ", certExpirationDetails='" + certExpirationDetails +
                ", providerSyncStates=" + providerSyncStates +
                '}';
    }
}
