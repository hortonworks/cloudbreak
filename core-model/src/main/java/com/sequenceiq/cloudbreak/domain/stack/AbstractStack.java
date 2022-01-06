package com.sequenceiq.cloudbreak.domain.stack;

import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.converter.StackTypeConverter;

@MappedSuperclass
public abstract class AbstractStack<T extends AbstractStack<T>> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "stack_generator")
    @SequenceGenerator(name = "stack_generator", sequenceName = "stack_id_seq", allocationSize = 1)
    private Long id;

    @Version
    private Long version;

    @Convert(converter = StackTypeConverter.class)
    private StackType type;

    @OneToOne(cascade = CascadeType.ALL)
    private StackStatus<T> stackStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public StackType getType() {
        return type;
    }

    public void setType(StackType type) {
        this.type = type;
    }

    public StackStatus<T> getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(StackStatus<T> stackStatus) {
        this.stackStatus = stackStatus;
    }

    public Status getStatus() {
        return stackStatus != null ? stackStatus.getStatus() : null;
    }

    public String getStatusReason() {
        return stackStatus != null ? stackStatus.getStatusReason() : null;
    }
}
