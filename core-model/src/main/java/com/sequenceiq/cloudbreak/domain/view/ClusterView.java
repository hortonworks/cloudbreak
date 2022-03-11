package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.converter.StatusConverter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
@Table(name = "Cluster")
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

    @Override
    public String toString() {
        return "ClusterView{" +
                "clusterManagerIp='" + clusterManagerIp + '\'' +
                ", status=" + status +
                '}';
    }
}
