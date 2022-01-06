package com.sequenceiq.cloudbreak.domain.stack.cluster;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.converter.StatusConverter;

@MappedSuperclass
public abstract class AbstractCluster<T extends AbstractCluster<T>> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cluster_generator")
    @SequenceGenerator(name = "cluster_generator", sequenceName = "cluster_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    @Convert(converter = StatusConverter.class)
    private Status status;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String statusReason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @deprecated {@link #getStatus} was replaced by {@link com.sequenceiq.cloudbreak.domain.stack.StackStatus#getStatus}.
     */
    @Deprecated
    public Status getStatus() {
        return status;
    }

    /**
     * @deprecated {@link #setStatus} was replaced by {@link com.sequenceiq.cloudbreak.domain.stack.StackStatus#setStatus}.
     */
    @Deprecated
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * @deprecated {@link #getStatusReason} was replaced by {@link com.sequenceiq.cloudbreak.domain.stack.StackStatus#getStatusReason}.
     */
    @Deprecated
    public String getStatusReason() {
        return statusReason;
    }

    /**
     * @deprecated {@link #setStatusReason} was replaced by {@link com.sequenceiq.cloudbreak.domain.stack.StackStatus#setStatusReason}.
     */
    @Deprecated
    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
}
