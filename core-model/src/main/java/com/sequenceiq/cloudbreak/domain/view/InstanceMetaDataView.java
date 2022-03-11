package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.InstanceStatusConverter;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "InstanceMetaData")
public class InstanceMetaDataView implements ProvisionEntity {

    @Id
    private Long id;

    @ManyToOne
    private InstanceGroupView instanceGroup;

    @Column(nullable = false)
    @Convert(converter = InstanceStatusConverter.class)
    private InstanceStatus instanceStatus;

    @Column
    private String instanceName;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

    public boolean isTerminated() {
        return InstanceStatus.TERMINATED.equals(instanceStatus);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(InstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getStatusReason() {
        return statusReason;
    }

    @Override
    public String toString() {
        return "InstanceMetaDataView{" +
                "id=" + id +
                ", instanceStatus=" + instanceStatus +
                ", instanceName='" + instanceName + '\'' +
                ", statusReason='" + statusReason + '\'' +
                '}';
    }
}
