package com.sequenceiq.cloudbreak.domain.stack;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "stackstatus")
public class StackStatus implements ProvisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stackstatus_generator")
    @SequenceGenerator(name = "stackstatus_generator", sequenceName = "stackstatus_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stack stack;

    private Long created;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    @Enumerated(EnumType.STRING)
    private DetailedStackStatus detailedStackStatus;

    public StackStatus() {
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
}
