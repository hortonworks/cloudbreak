package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@Table(name = "Stack")
// It's only here, because of findbugs does not know the fields will be set by JPA with Reflection
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
public class StackApiView extends CompactViewWithOwner {
    @OneToOne(mappedBy = "stack")
    private ClusterApiView cluster;

    @Column(columnDefinition = "TEXT")
    private String cloudPlatform;

    @Column(columnDefinition = "TEXT")
    private String platformVariant;

    @OneToOne
    private Credential credential;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "key")
    @Column(name = "value", columnDefinition = "TEXT", length = 100000, nullable = false)
    @JoinTable(name = "stack_parameters", joinColumns = @JoinColumn(name = "stack_id"))
    private Map<String, String> parameters;

    @OneToOne
    private OrchestratorView orchestrator;

    @OneToOne
    private StackStatusView stackStatus;

    @OneToMany(mappedBy = "stack", fetch = FetchType.EAGER)
    private Set<InstanceGroupView> instanceGroups = new HashSet<>();

    @ManyToOne
    private FlexSubscription flexSubscription;

    private Long created;

    private Long datalakeId;

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.STACK;
    }

    public ClusterApiView getCluster() {
        return cluster;
    }

    public void setCluster(ClusterApiView cluster) {
        this.cluster = cluster;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getPlatformVariant() {
        return platformVariant;
    }

    public void setPlatformVariant(String platformVariant) {
        this.platformVariant = platformVariant;
    }

    public Credential getCredential() {
        return credential;
    }

    public void setCredential(Credential credential) {
        this.credential = credential;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public OrchestratorView getOrchestrator() {
        return orchestrator;
    }

    public void setOrchestrator(OrchestratorView orchestrator) {
        this.orchestrator = orchestrator;
    }

    public StackStatusView getStackStatus() {
        return stackStatus;
    }

    public void setStackStatus(StackStatusView stackStatus) {
        this.stackStatus = stackStatus;
    }

    public Set<InstanceGroupView> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<InstanceGroupView> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public FlexSubscription getFlexSubscription() {
        return flexSubscription;
    }

    public void setFlexSubscription(FlexSubscription flexSubscription) {
        this.flexSubscription = flexSubscription;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getDatalakeId() {
        return datalakeId;
    }

    public void setDatalakeId(Long datalakeId) {
        this.datalakeId = datalakeId;
    }

    public Status getStatus() {
        return stackStatus != null ? stackStatus.getStatus() : null;
    }
}
