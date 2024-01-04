package com.sequenceiq.cloudbreak.domain.view;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.converter.StatusConverter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
@Table(name = "Cluster")
@Deprecated
public class ClusterView extends CompactView {
    @OneToOne(fetch = FetchType.LAZY)
    private StackView stack;

    private String clusterManagerIp;

    @Convert(converter = StatusConverter.class)
    private Status status;

    public StackView getStackView() {
        return stack;
    }

    public String getClusterManagerIp() {
        return clusterManagerIp;
    }

    /**
     * @deprecated {@link #getStatus} was replaced by {@link com.sequenceiq.cloudbreak.domain.view.StackView#getStatus}.
     */
    @Deprecated
    public Status getStatus() {
        return status;
    }

}
