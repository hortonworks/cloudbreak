package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.Credential;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@Table(name = "Stack")
// It's only here, because of findbugs does not know the fields will be set by JPA with Reflection
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
public class StackApiView extends CompactView {
    @OneToOne(mappedBy = "stack")
    private ClusterApiView cluster;

    @Column(columnDefinition = "TEXT")
    private String cloudPlatform;

    @Column(columnDefinition = "TEXT")
    private String platformVariant;

    @OneToOne
    private Credential credential;

    @OneToOne
    private StackStatusView stackStatus;

    @OneToMany(mappedBy = "stack", fetch = FetchType.EAGER)
    private Set<InstanceGroupView> instanceGroups = new HashSet<>();

    private Long created;

    private Long terminated;

    private Long datalakeId;

    @Enumerated(EnumType.STRING)
    private StackType type = StackType.WORKLOAD;

    @ManyToOne
    @JoinColumn(name = "createdBy")
    private UserView userView;

    @ManyToOne
    private EnvironmentView environment;

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

    public EnvironmentView getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentView environment) {
        this.environment = environment;
    }

    public UserView getUserView() {
        return userView;
    }

    public void setUserView(UserView userView) {
        this.userView = userView;
    }

    public StackType getType() {
        return type;
    }

    public void setType(StackType type) {
        this.type = type;
    }

    public Long getTerminated() {
        return terminated;
    }

    public void setTerminated(Long terminated) {
        this.terminated = terminated;
    }
}
