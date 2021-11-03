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
import com.sequenceiq.cloudbreak.domain.converter.StackFixTypeConverter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"stack_id", "type"}))
public class StackFix implements ProvisionEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stackfix_generator")
    @SequenceGenerator(name = "stackfix_generator", sequenceName = "stackfix_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Stack stack;

    @Convert(converter = StackFixTypeConverter.class)
    private StackFixType type;

    private Long created = System.currentTimeMillis();

    public StackFix() {
    }

    public StackFix(Stack stack, StackFixType type) {
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

    public StackFixType getType() {
        return type;
    }

    public void setType(StackFixType type) {
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
        return "StackFix{" +
                "id=" + id +
                ", type=" + type +
                ", created=" + created +
                '}';
    }

    public enum StackFixType {
        UNBOUND_RESTART,
        UNKNOWN
    }
}
