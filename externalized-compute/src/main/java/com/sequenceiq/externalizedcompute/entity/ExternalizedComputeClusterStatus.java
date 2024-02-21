package com.sequenceiq.externalizedcompute.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Entity
@Table(name = "externalized_compute_cluster_status")
@EntityType(entityClass = ExternalizedComputeClusterStatus.class)
public class ExternalizedComputeClusterStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "status_generator")
    @SequenceGenerator(name = "status_generator", sequenceName = "status_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "externalizedcomputecluster_id")
    private ExternalizedComputeCluster externalizedComputeCluster;

    private Long created;

    private String statusReason;

    @NotNull
    @Convert(converter = ExternalizedComputeClusterStatusEnumConverter.class)
    private ExternalizedComputeClusterStatusEnum status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExternalizedComputeCluster getExternalizedComputeCluster() {
        return externalizedComputeCluster;
    }

    public void setExternalizedComputeCluster(ExternalizedComputeCluster externalizedComputeCluster) {
        this.externalizedComputeCluster = externalizedComputeCluster;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public ExternalizedComputeClusterStatusEnum getStatus() {
        return status;
    }

    public void setStatus(ExternalizedComputeClusterStatusEnum status) {
        this.status = status;
    }
}
