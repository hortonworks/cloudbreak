package com.sequenceiq.cloudbreak.domain.view;

import static com.sequenceiq.cloudbreak.api.model.Status.REQUESTED;
import static com.sequenceiq.cloudbreak.api.model.Status.START_REQUESTED;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
@Table(name = "Cluster")
public class ClusterView extends CompactView {
    @OneToOne(fetch = FetchType.LAZY)
    private StackView stack;

    private String ambariIp;

    @Enumerated(EnumType.STRING)
    private Status status;

    public StackView getStackView() {
        return stack;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.STACK;
    }

    public String getAmbariIp() {
        return ambariIp;
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
}
