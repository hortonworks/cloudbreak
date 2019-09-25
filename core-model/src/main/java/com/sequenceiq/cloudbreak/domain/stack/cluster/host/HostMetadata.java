package com.sequenceiq.cloudbreak.domain.stack.cluster.host;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
public class HostMetadata implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "hostmetadata_generator")
    @SequenceGenerator(name = "hostmetadata_generator", sequenceName = "hostmetadata_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String hostName;

    @ManyToOne
    private HostGroup hostGroup;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private HostMetadataState hostMetadataState = HostMetadataState.HEALTHY;

    @Column(columnDefinition = "TEXT")
    private String statusReason;

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

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "hostGroup");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        } else if (this == o) {
            return true;
        }

        return EqualsBuilder.reflectionEquals(this, o, "hostGroup");
    }

    @Override
    public String toString() {
        return "HostMetadata{"
                + "id=" + id
                + ", hostName='" + hostName + '\''
                + ", hostGroupName=" + (hostGroup != null ? hostGroup.getName() : null)
                + ", hostMetadataState=" + hostMetadataState
                + '}';
    }
}
