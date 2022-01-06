package com.sequenceiq.cloudbreak.domain.stack;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.DetailedStackStatusConverter;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.StatusConverter;

@Entity
@Table(name = "stackstatus")
public class StackStatus<T extends AbstractStack<T>> implements ProvisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stackstatus_generator")
    @SequenceGenerator(name = "stackstatus_generator", sequenceName = "stackstatus_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = Stack.class)
    private T stack;

    private Long created;

    @Convert(converter = StatusConverter.class)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    @Convert(converter = DetailedStackStatusConverter.class)
    private DetailedStackStatus detailedStackStatus;

    public StackStatus() {
    }

    public StackStatus(T stack, DetailedStackStatus detailedStackStatus) {
        this(stack, detailedStackStatus.getStatus(), "", detailedStackStatus);
    }

    public StackStatus(T stack, Status status, String statusReason, DetailedStackStatus detailedStackStatus) {
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

    public void setStack(T stack) {
        this.stack = stack;
    }

    public T getStack() {
        return stack;
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

    public void setDetailedStackStatus(DetailedStackStatus detailedStackStatus) {
        this.detailedStackStatus = detailedStackStatus;
    }

    public DetailedStackStatus getDetailedStackStatus() {
        return detailedStackStatus;
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
                "status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", detailedStackStatus=" + detailedStackStatus +
                '}';
    }
}
