package com.sequenceiq.cloudbreak.domain.stack;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.StackPatchTypeConverter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"stack_id", "type"}))
public class StackPatch implements ProvisionEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stackpatch_generator")
    @SequenceGenerator(name = "stackpatch_generator", sequenceName = "stackpatch_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stack stack;

    @Convert(converter = StackPatchTypeConverter.class)
    private StackPatchType type;

    private Long created = System.currentTimeMillis();

    public StackPatch() {
    }

    public StackPatch(Stack stack, StackPatchType type) {
        this.stack = stack;
        this.type = type;
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

    public StackPatchType getType() {
        return type;
    }

    public void setType(StackPatchType type) {
        this.type = type;
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
                ", type=" + type +
                ", created=" + created +
                '}';
    }

}
