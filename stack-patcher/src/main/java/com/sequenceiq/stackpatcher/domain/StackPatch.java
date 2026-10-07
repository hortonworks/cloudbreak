package com.sequenceiq.stackpatcher.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.sequenceiq.stackpatcher.converter.StackPatchStatusConverter;
import com.sequenceiq.stackpatcher.converter.StackPatchTypeConverter;

@Entity
@Table(name = "stackpatch", uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "type"}))
public class StackPatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stackpatch_generator")
    @SequenceGenerator(name = "stackpatch_generator", sequenceName = "stackpatch_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "resource_id")
    private Long resourceId;

    @Convert(converter = StackPatchTypeConverter.class)
    private StackPatchType type;

    @Convert(converter = StackPatchStatusConverter.class)
    private StackPatchStatus status;

    @Column(name = "status_reason")
    private String statusReason;

    private Long created = System.currentTimeMillis();

    public StackPatch() {
    }

    public StackPatch(Long resourceId, StackPatchType type) {
        this.resourceId = resourceId;
        this.type = type;
        this.status = StackPatchStatus.SCHEDULED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public StackPatchType getType() {
        return type;
    }

    public void setType(StackPatchType type) {
        this.type = type;
    }

    public StackPatchStatus getStatus() {
        return status;
    }

    public void setStatus(StackPatchStatus status) {
        this.status = status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "StackPatch{" +
                "id=" + id +
                ", resourceId=" + resourceId +
                ", type=" + type +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", created=" + created +
                '}';
    }
}
