package com.sequenceiq.cloudbreak.domain.view;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.StatusConverter;

@Entity
@Table(name = "stackstatus")
@Deprecated
public class StackStatusView implements ProvisionEntity {
    @Id
    private Long id;

    @Convert(converter = StatusConverter.class)
    private Status status;

    public Long getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
