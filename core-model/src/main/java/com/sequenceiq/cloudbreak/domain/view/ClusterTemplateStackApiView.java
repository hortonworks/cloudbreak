package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.converter.StackTypeConverter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@Table(name = "Stack")
// It's only here, because of findbugs does not know the fields will be set by JPA with Reflection
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
public class ClusterTemplateStackApiView extends CompactView {

    @OneToOne(mappedBy = "stack")
    private ClusterTemplateClusterApiView cluster;

    @Column(columnDefinition = "TEXT")
    private String cloudPlatform;

    @OneToMany(mappedBy = "stack", fetch = FetchType.EAGER)
    private Set<ClusterTemplateInstanceGroupView> instanceGroups = new HashSet<>();

    @Convert(converter = StackTypeConverter.class)
    private StackType type = StackType.WORKLOAD;

    private String environmentCrn;

    private String resourceCrn;

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public ClusterTemplateClusterApiView getCluster() {
        return cluster;
    }

    public void setCluster(ClusterTemplateClusterApiView cluster) {
        this.cluster = cluster;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Set<ClusterTemplateInstanceGroupView> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Set<ClusterTemplateInstanceGroupView> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public StackType getType() {
        return type;
    }

    public void setType(StackType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ClusterTemplateStackApiView{" +
                "cloudPlatform='" + cloudPlatform + '\'' +
                ", type=" + type +
                ", environmentCrn='" + environmentCrn + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                '}';
    }
}
