package com.sequenceiq.freeipa.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.util.DetailsStackStatusConverter;
import com.sequenceiq.freeipa.entity.util.StackStatusConverter;

@Entity
@Table(name = "stackstatus")
public class StackStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stackstatus_generator")
    @SequenceGenerator(name = "stackstatus_generator", sequenceName = "stackstatus_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stack stack;

    private Long created;

    @Convert(converter = StackStatusConverter.class)
    private Status status;

    @Column(name = "status", insertable = false, updatable = false)
    private String statusString;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    @Convert(converter = DetailsStackStatusConverter.class)
    private DetailedStackStatus detailedStackStatus;

    @Column(name = "detailedStackStatus", insertable = false, updatable = false)
    private String detailedStackStatusString;

    public StackStatus() {
    }

    public StackStatus(Stack stack, String statusReason, DetailedStackStatus detailedStackStatus) {
        this(stack, detailedStackStatus.getStatus(), statusReason, detailedStackStatus);
    }

    public StackStatus(Stack stack, Status status, String statusReason, DetailedStackStatus detailedStackStatus) {
        this.stack = stack;
        this.status = status;
        this.statusReason = statusReason;
        this.detailedStackStatus = detailedStackStatus;
        created = new Date().getTime();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public String getStatusString() {
        return statusString;
    }

    public void setStatusString(String statusString) {
        this.statusString = statusString;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setDetailedStackStatus(DetailedStackStatus detailedStackStatus) {
        this.detailedStackStatus = detailedStackStatus;
    }

    public DetailedStackStatus getDetailedStackStatus() {
        return detailedStackStatus;
    }

    public String getDetailedStackStatusString() {
        return detailedStackStatusString;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getCreated() {
        return created;
    }

    @Override
    public String toString() {
        return "StackStatus{" +
                "id=" + id +
                ", created=" + created +
                ", status=" + status +
                ", statusString='" + statusString + '\'' +
                ", statusReason='" + statusReason + '\'' +
                ", detailedStackStatus=" + detailedStackStatus +
                ", detailedStackStatusString='" + detailedStackStatusString + '\'' +
                '}';
    }
}
