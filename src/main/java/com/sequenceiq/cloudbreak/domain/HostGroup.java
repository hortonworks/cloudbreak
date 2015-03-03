package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "HostGroup.findHostGroupsInCluster",
                query = "SELECT h FROM HostGroup h "
                        + "LEFT JOIN FETCH h.hostMetadata "
                        + "WHERE h.cluster.id= :clusterId"),
        @NamedQuery(
                name = "HostGroup.findHostGroupInClusterByName",
                query = "SELECT h FROM HostGroup h "
                        + "LEFT JOIN FETCH h.hostMetadata "
                        + "WHERE h.cluster.id= :clusterId "
                        + "AND h.name= :hostGroupName")
})
public class HostGroup {


    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @ManyToOne
    private Cluster cluster;

    @ManyToOne
    private InstanceGroup instanceGroup;

    @OneToMany(mappedBy = "hostGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HostMetadata> hostMetadata = new HashSet<>();

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

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public InstanceGroup getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(InstanceGroup instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public Set<HostMetadata> getHostMetadata() {
        return hostMetadata;
    }

    public void setHostMetadata(Set<HostMetadata> hostMetadata) {
        this.hostMetadata = hostMetadata;
    }
}
