package com.sequenceiq.cloudbreak.domain.view;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "HostGroup")
@Deprecated
public class HostGroupView implements ProvisionEntity {

    @Id
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private ClusterApiView cluster;

    @OneToOne
    private InstanceGroupView instanceGroup;

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

    public ClusterApiView getCluster() {
        return cluster;
    }

    public void setCluster(ClusterApiView cluster) {
        this.cluster = cluster;
    }

    public InstanceGroupView getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(InstanceGroupView instanceGroup) {
        this.instanceGroup = instanceGroup;
    }
}
