package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "HostMetadata.findHostsInCluster",
                query = "SELECT h FROM HostMetadata h "
                        + "WHERE h.hostGroup.cluster.id= :clusterId"),
        @NamedQuery(
                name = "HostMetadata.findHostInClusterByName",
                query = "SELECT h FROM HostMetadata h "
                        + "WHERE h.hostGroup.cluster.id= :clusterId AND h.hostName = :hostName"),
        @NamedQuery(
                name = "HostMetadata.findEmptyContainerHostsInHostGroup",
                query = "SELECT h FROM HostMetadata h "
                        + "WHERE h.hostGroup.id= :hostGroupId AND h.hostMetadataState= 'CONTAINER_RUNNING'")
})
public class HostMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "hostmetadata_generator")
    @SequenceGenerator(name = "hostmetadata_generator", sequenceName = "hostmetadata_id_seq", allocationSize = 1)
    private Long id;

    private String hostName;

    @ManyToOne
    private HostGroup hostGroup;

    @Enumerated(EnumType.STRING)
    private HostMetadataState hostMetadataState = HostMetadataState.HEALTHY;

    public HostMetadata() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public HostMetadataState getHostMetadataState() {
        return hostMetadataState;
    }

    public void setHostMetadataState(HostMetadataState hostMetadataState) {
        this.hostMetadataState = hostMetadataState;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "hostGroup");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        return EqualsBuilder.reflectionEquals(this, o, "hostGroup");
    }

}
