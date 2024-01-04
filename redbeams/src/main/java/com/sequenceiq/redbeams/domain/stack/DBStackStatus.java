package com.sequenceiq.redbeams.domain.stack;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.repository.converter.DetailedDBStackStatusConverter;
import com.sequenceiq.redbeams.repository.converter.StatusConverter;

@Entity
@Table
public class DBStackStatus {

    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "id")
    @MapsId
    private DBStack dbStack;

    private Long created;

    @Column(nullable = false)
    @Convert(converter = StatusConverter.class)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    @Convert(converter = DetailedDBStackStatusConverter.class)
    private DetailedDBStackStatus detailedDBStackStatus;

    public DBStackStatus() {
    }

    public DBStackStatus(DBStack dbStack, DetailedDBStackStatus detailedDBStackStatus, Long created) {
        this(dbStack, detailedDBStackStatus.getStatus(), "", detailedDBStackStatus, created);
    }

    public DBStackStatus(DBStack dbStack, Status status, String statusReason, DetailedDBStackStatus detailedDBStackStatus, Long created) {
        this.dbStack = dbStack;
        this.status = status;
        this.statusReason = statusReason;
        this.detailedDBStackStatus = detailedDBStackStatus;
        this.created = created;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setDBStack(DBStack dbStack) {
        this.dbStack = dbStack;
    }

    public DBStack getDBStack() {
        return dbStack;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setDetailedDBStackStatus(DetailedDBStackStatus detailedDBStackStatus) {
        this.detailedDBStackStatus = detailedDBStackStatus;
    }

    public DetailedDBStackStatus getDetailedDBStackStatus() {
        return detailedDBStackStatus;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getCreated() {
        return created;
    }
}
