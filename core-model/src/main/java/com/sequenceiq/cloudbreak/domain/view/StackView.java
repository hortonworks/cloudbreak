package com.sequenceiq.cloudbreak.domain.view;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.STOP_REQUESTED;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@Table(name = "Stack")
// It's only here, because of findbugs does not know the fields will be set by JPA with Reflection
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
public class StackView implements ProvisionEntity, OrganizationAwareResource {

    @Id
    private Long id;

    private String name;

    private String owner;

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

    @ManyToOne
    private Organization organization;

    public StackView() {
    }

    public StackView(Long id, String name, String owner, String cloudPlatform, StackStatusView stackStatus) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.cloudPlatform = cloudPlatform;
        this.stackStatus = stackStatus;
    }

    public Long getId() {
        return id;
    }

    @Override
    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    @Override
    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    @Override
    public OrganizationResource getResource() {
        return OrganizationResource.STACK;
    }

    public String getOwner() {
        return owner;
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
}