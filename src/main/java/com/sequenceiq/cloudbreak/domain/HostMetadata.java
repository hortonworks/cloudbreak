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
                + "WHERE h.hostGroup.id= :hostGroupId ")
public class HostMetadata {

    @Id
    @GeneratedValue
    private Long id;

    private String hostName;

    @ManyToOne
    private HostGroup hostGroup;

    public HostMetadata() {

    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public HostGroup getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(HostGroup hostGroup) {
        this.hostGroup = hostGroup;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "hostGroup");
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, "hostGroup");
    }

}
