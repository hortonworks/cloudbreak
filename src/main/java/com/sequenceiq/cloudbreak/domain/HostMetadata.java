package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@NamedQuery(
        name = "HostMetadata.findHostsInHostgroup",
        query = "SELECT h FROM HostMetadata h "
                + "WHERE h.hostGroup= :hostGroup "
                + "AND h.cluster.id= :clusterId")
public class HostMetadata {

    @Id
    @GeneratedValue
    private Long id;

    private String hostName;

    private String hostGroup;

    @ManyToOne
    private Cluster cluster;

    public HostMetadata() {

    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "cluster");
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, "cluster");
    }

}
