package com.sequenceiq.datalake.entity;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretToString;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"accountid", "envname"}))
public class SdxCluster implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sdx_cluster_generator")
    @SequenceGenerator(name = "sdx_cluster_generator", sequenceName = "sdxcluster_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    private String accountId;

    private String crn;

    @NotNull
    private String clusterName;

    @NotNull
    private String initiatorUserCrn;

    @NotNull
    private String envName;

    @NotNull
    private String envCrn;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SdxClusterShape clusterShape;

    @NotNull
    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json tags;

    private Long stackId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SdxClusterStatus status;

    private String statusReason;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret stackRequest = Secret.EMPTY;

    @Convert(converter = SecretToString.class)
    @SecretValue
    private Secret stackRequestToCloudbreak = Secret.EMPTY;

    private Long deleted;

    @Column(nullable = false)
    private Boolean createDatabase = Boolean.FALSE;

    private String databaseCrn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public SdxClusterStatus getStatus() {
        return status;
    }

    public void setStatus(SdxClusterStatus status) {
        this.status = status;
    }

    public String getInitiatorUserCrn() {
        return initiatorUserCrn;
    }

    public void setInitiatorUserCrn(String initiatorUserCrn) {
        this.initiatorUserCrn = initiatorUserCrn;
    }

    public String getEnvName() {
        return envName;
    }

    public void setEnvName(String envName) {
        this.envName = envName;
    }

    public String getEnvCrn() {
        return envCrn;
    }

    public void setEnvCrn(String envCrn) {
        this.envCrn = envCrn;
    }

    public SdxClusterShape getClusterShape() {
        return clusterShape;
    }

    public void setClusterShape(SdxClusterShape clusterShape) {
        this.clusterShape = clusterShape;
    }

    public Json getTags() {
        return tags;
    }

    public void setTags(Json tags) {
        this.tags = tags;
    }

    public Long getDeleted() {
        return deleted;
    }

    public void setDeleted(Long deleted) {
        this.deleted = deleted;
    }

    public String getStackRequest() {
        return stackRequest.getRaw();
    }

    public void setStackRequest(String stackRequest) {
        this.stackRequest = new Secret(stackRequest);
    }

    public String getStackRequestToCloudbreak() {
        return stackRequestToCloudbreak.getRaw();
    }

    public void setStackRequestToCloudbreak(String stackRequestToCloudbreak) {
        this.stackRequestToCloudbreak = new Secret(stackRequestToCloudbreak);
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Boolean getCreateDatabase() {
        return createDatabase;
    }

    public void setCreateDatabase(Boolean createDatabase) {
        this.createDatabase = createDatabase;
    }

    public String getDatabaseCrn() {
        return databaseCrn;
    }

    public void setDatabaseCrn(String databaseCrn) {
        this.databaseCrn = databaseCrn;
    }
}
