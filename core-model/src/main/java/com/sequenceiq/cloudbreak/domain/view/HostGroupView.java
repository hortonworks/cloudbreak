package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "HostGroup")
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

    @Override
    public String toString() {
        return "HostGroupView{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
