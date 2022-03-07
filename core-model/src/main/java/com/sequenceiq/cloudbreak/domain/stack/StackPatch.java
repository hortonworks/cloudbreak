package com.sequenceiq.cloudbreak.domain.stack;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.StackPatchStatusConverter;
import com.sequenceiq.cloudbreak.domain.converter.StackPatchTypeConverter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"stack_id", "type"}))
public class StackPatch implements ProvisionEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stackpatch_generator")
    @SequenceGenerator(name = "stackpatch_generator", sequenceName = "stackpatch_id_seq", allocationSize = 1)
    private Long id;

    /**
     * Stack is not mapped from DB, only stored in the object
     */
    @Transient
    private Stack stack;

    @Column(name = "stack_id")
    private Long stackId;

    @Convert(converter = StackPatchTypeConverter.class)
    private StackPatchType type;

    @Convert(converter = StackPatchStatusConverter.class)
    private StackPatchStatus status;

    @Column(name = "status_reason")
    private String statusReason;

    private Long created = System.currentTimeMillis();

    public StackPatch() {
    }

    public StackPatch(Stack stack, StackPatchType type) {
        this.stack = stack;
        this.stackId = stack.getId();
        this.type = type;
        this.status = StackPatchStatus.SCHEDULED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
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
                ", stackId=" + stackId +
                ", type=" + type +
                ", status=" + status +
                ", statusReason='" + statusReason + '\'' +
                ", created=" + created +
                '}';
    }
}
