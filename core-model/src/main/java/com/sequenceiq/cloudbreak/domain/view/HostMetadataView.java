package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "HostMetadata")
public class HostMetadataView implements ProvisionEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String hostName;

    @ManyToOne(fetch = FetchType.LAZY)
    private HostGroupView hostGroup;

    @Enumerated(EnumType.STRING)
    private HostMetadataState hostMetadataState = HostMetadataState.HEALTHY;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public HostMetadataState getHostMetadataState() {
        return hostMetadataState;
    }

    public void setHostMetadataState(HostMetadataState hostMetadataState) {
        this.hostMetadataState = hostMetadataState;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
