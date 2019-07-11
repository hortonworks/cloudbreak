package com.sequenceiq.freeipa.entity;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationType;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;

@Entity
@Table(name = "syncoperation")
public class SyncOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "syncoperation_generator")
    @SequenceGenerator(name = "syncoperation_generator", sequenceName = "syncoperation_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String operationId;

    @Column(nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncOperationType syncOperationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SynchronizationStatus status;

    // TODO can I store these as JSON?
    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json successList;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json failureList;

    @Column(columnDefinition = "TEXT")
    private String error;

    @Column(nullable = false)
    private Long startTime;

    private Long endTime;

    @PrePersist
    protected void onCreate() {
        startTime = System.currentTimeMillis();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public SyncOperationType getSyncOperationType() {
        return syncOperationType;
    }

    public void setSyncOperationType(SyncOperationType syncOperationType) {
        this.syncOperationType = syncOperationType;
    }

    public SynchronizationStatus getStatus() {
        return status;
    }

    public void setStatus(SynchronizationStatus status) {
        this.status = status;
    }

    public Json getSuccessList() {
        return successList;
    }

    public void setSuccessList(Json successList) {
        this.successList = successList;
    }

    public Json getFailureList() {
        return failureList;
    }

    public void setFailureList(Json failureList) {
        this.failureList = failureList;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}
