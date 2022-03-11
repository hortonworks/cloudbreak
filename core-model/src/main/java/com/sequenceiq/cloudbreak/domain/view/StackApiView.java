package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.converter.TunnelConverter;
import com.sequenceiq.cloudbreak.domain.converter.StackTypeConverter;
import com.sequenceiq.common.api.type.Tunnel;

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
    private StackStatusView stackStatus;

    @OneToMany(mappedBy = "stack", fetch = FetchType.EAGER)
    private Set<InstanceGroupView> instanceGroups = new HashSet<>();

    private Long created;

    private Long terminated;

    private String datalakeCrn;

    @Convert(converter = StackTypeConverter.class)
    private StackType type = StackType.WORKLOAD;

    @ManyToOne
    @JoinColumn(name = "createdBy")
    private UserView userView;

    private String environmentCrn;

    private String resourceCrn;

    private String stackVersion;

    @Transient
    private Integer nodeCount;

    @Convert(converter = TunnelConverter.class)
    private Tunnel tunnel = Tunnel.DIRECT;

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
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

    public String getDatalakeCrn() {
        return datalakeCrn;
    }

    public void setDatalakeCrn(String datalakeCrn) {
        this.datalakeCrn = datalakeCrn;
    }

    public Status getStatus() {
        return stackStatus != null ? stackStatus.getStatus() : null;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
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

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public Tunnel getTunnel() {
        return tunnel;
    }

    public void setTunnel(Tunnel tunnel) {
        this.tunnel = tunnel;
    }

    public String getStackVersion() {
        return stackVersion;
    }

    public void setStackVersion(String stackVersion) {
        this.stackVersion = stackVersion;
    }

    @Override
    public String toString() {
        return "StackApiView{" +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", platformVariant='" + platformVariant + '\'' +
                ", stackStatus=" + stackStatus +
                ", created=" + created +
                ", terminated=" + terminated +
                ", datalakeCrn='" + datalakeCrn + '\'' +
                ", type=" + type +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", stackVersion='" + stackVersion + '\'' +
                ", nodeCount=" + nodeCount +
                ", tunnel=" + tunnel +
                '}';
    }
}
