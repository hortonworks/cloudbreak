package com.sequenceiq.cloudbreak.domain.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "HostGroup")
@Deprecated
public class HostGroupView implements ProvisionEntity {

    @Id
    private Long id;

    private String name;

    @Column(name = "cluster_id")
    private Long clusterId;

    @Column(name = "instancegroup_id")
    private Long instanceGroupId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public Long getInstanceGroupId() {
        return instanceGroupId;
    }

    public void setInstanceGroupId(Long instanceGroupId) {
        this.instanceGroupId = instanceGroupId;
    }
}
