package com.sequenceiq.cloudbreak.domain.view;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@Table(name = "Stack")
// It's only here, because of findbugs does not know the fields will be set by JPA with Reflection
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
public class StackView extends CompactView {

    @OneToOne(mappedBy = "stack")
    private ClusterView cluster;

    @Column(columnDefinition = "TEXT")
    private String cloudPlatform;

    @Column(columnDefinition = "TEXT")
    private String platformVariant;

    @OneToOne
    private OrchestratorView orchestrator;

    @OneToOne
    private StackStatusView stackStatus;

    private Integer gatewayPort;

    private Long created;

    private Long terminated;

    private String resourceCrn;

    private String environmentCrn;

    public StackView() {
    }

    public StackView(Long id, String name, String cloudPlatform, StackStatusView stackStatus) {
        super(id, name);
        this.cloudPlatform = cloudPlatform;
        this.stackStatus = stackStatus;
    }

    public ClusterView getClusterView() {
        return cluster;
    }

    public OrchestratorView getOrchestrator() {
        return orchestrator;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public String cloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public boolean isAvailable() {
        return AVAILABLE.equals(getStatus());
    }

    public StackStatusView getStackStatus() {
        return stackStatus;
    }

    public Status getStatus() {
        return stackStatus != null ? stackStatus.getStatus() : null;
    }

    public boolean isDeleteCompleted() {
        return DELETE_COMPLETED.equals(getStatus());
    }

    public boolean isDeleteInProgress() {
        return DELETE_IN_PROGRESS.equals(getStatus());
    }

    public boolean isStopRequested() {
        return STOP_REQUESTED.equals(getStatus());
    }

    public Integer getGatewayPort() {
        return gatewayPort;
    }

    public Long getCreated() {
        return created;
    }

    public boolean isStackInDeletionPhase() {
        return DELETE_COMPLETED.equals(getStatus()) || DELETE_IN_PROGRESS.equals(getStatus());
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Long getTerminated() {
        return terminated;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }
}