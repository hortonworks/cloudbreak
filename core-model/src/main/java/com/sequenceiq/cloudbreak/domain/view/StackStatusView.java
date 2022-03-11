package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.converter.StatusConverter;

@Entity
@Table(name = "stackstatus")
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

    @Override
    public String toString() {
        return "StackStatusView{" +
                "id=" + id +
                ", status=" + status +
                '}';
    }
}
