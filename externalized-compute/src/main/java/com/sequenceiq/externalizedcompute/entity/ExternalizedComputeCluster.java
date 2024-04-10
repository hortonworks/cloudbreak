package com.sequenceiq.externalizedcompute.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@Entity
@Table(name = "externalized_compute_cluster", uniqueConstraints = @UniqueConstraint(columnNames = {"accountid", "name"}))
@EntityType(entityClass = ExternalizedComputeCluster.class)
public class ExternalizedComputeCluster implements AccountAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "externalized_compute_cluster_generator")
    @SequenceGenerator(name = "externalized_compute_cluster_generator", sequenceName = "externalized_compute_cluster_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    private String accountId;

    @NotNull
    private String resourceCrn;

    @NotNull
    private String environmentCrn;

    @NotNull
    private String name;

    private String liftieName;

    @NotNull
    private Long created;

    private Long deleted;

    @NotNull
    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    private boolean defaultCluster;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String envCrn) {
        this.environmentCrn = envCrn;
    }

    public String getLiftieName() {
        return liftieName;
    }

    public void setLiftieName(String liftieName) {
        this.liftieName = liftieName;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getDeleted() {
        return deleted;
    }

    public void setDeleted(Long deleted) {
        this.deleted = deleted;
    }

    public Json getTags() {
        return tags;
    }

    public void setTags(Json tags) {
        this.tags = tags;
    }

    public boolean isDefaultCluster() {
        return defaultCluster;
    }

    public void setDefaultCluster(boolean defaultCluster) {
        this.defaultCluster = defaultCluster;
    }

    @Override
    public String toString() {
        return "ExternalizedComputeCluster{" +
                "id=" + id +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", name='" + name + '\'' +
                ", liftieName='" + liftieName + '\'' +
                ", created=" + created +
                ", deleted=" + deleted +
                ", tags=" + tags +
                ", defaultCluster=" + defaultCluster +
                '}';
    }
}
