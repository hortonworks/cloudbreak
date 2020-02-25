package com.sequenceiq.cloudbreak.domain.view;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_REQUESTED;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
@Table(name = "Cluster")
public class ClusterView extends CompactView {
    @OneToOne(fetch = FetchType.LAZY)
    private StackView stack;

    private String clusterManagerIp;

    @Enumerated(EnumType.STRING)
    private Status status;

    public StackView getStackView() {
        return stack;
    }

    public String getClusterManagerIp() {
        return clusterManagerIp;
    }

    public Status getStatus() {
        return status;
    }

    public boolean isRequested() {
        return REQUESTED.equals(status);
    }

    public boolean isStartRequested() {
        return START_REQUESTED.equals(status);
    }

    public boolean isAvailable() {
        return AVAILABLE.equals(status);
    }
}
