package com.sequenceiq.redbeams.domain.stack;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.api.model.common.Status;

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
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    @Enumerated(EnumType.STRING)
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
