package com.sequenceiq.cloudbreak.domain.stack.cluster;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.converter.ClusterTemplateV4TypeConverter;
import com.sequenceiq.cloudbreak.domain.converter.DatalakeRequiredConverter;
import com.sequenceiq.cloudbreak.domain.converter.FeatureStateConverter;
import com.sequenceiq.cloudbreak.domain.converter.ResourceStatusConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class ClusterTemplate implements WorkspaceAwareResource, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "clustertemplate_generator")
    @SequenceGenerator(name = "clustertemplate_generator", sequenceName = "clustertemplate_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @OneToOne
    private Stack stackTemplate;

    @ManyToOne
    private Workspace workspace;

    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus status;

    private String cloudPlatform;

    @Convert(converter = DatalakeRequiredConverter.class)
    private DatalakeRequired datalakeRequired;

    @Convert(converter = FeatureStateConverter.class)
    private FeatureState featureState;

    @Convert(converter = ClusterTemplateV4TypeConverter.class)
    private ClusterTemplateV4Type type = ClusterTemplateV4Type.OTHER;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String templateContent;

    @Column(nullable = false)
    private String resourceCrn;

    private String clouderaRuntimeVersion;

    private Long created = System.currentTimeMillis();

    public ClusterTemplate() {

    }

    public ClusterTemplate(Long id, String templateContent, String name, ResourceStatus status, Workspace workspace) {
        this.id = id;
        this.templateContent = templateContent;
        this.name = name;
        this.status = status;
        this.workspace = workspace;
    }

    public Stack getStackTemplate() {
        return stackTemplate;
    }

    public void setStackTemplate(Stack stackTemplate) {
        this.stackTemplate = stackTemplate;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public DatalakeRequired getDatalakeRequired() {
        return datalakeRequired;
    }

    public void setDatalakeRequired(DatalakeRequired datalakeRequired) {
        this.datalakeRequired = datalakeRequired;
    }

    public ClusterTemplateV4Type getType() {
        return type;
    }

    public void setType(ClusterTemplateV4Type type) {
        this.type = type;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public FeatureState getFeatureState() {
        return featureState;
    }

    public void setFeatureState(FeatureState featureState) {
        this.featureState = featureState;
    }

    public String getClouderaRuntimeVersion() {
        return clouderaRuntimeVersion;
    }

    public void setClouderaRuntimeVersion(String clouderaRuntimeVersion) {
        this.clouderaRuntimeVersion = clouderaRuntimeVersion;
    }

    @Override
    public String toString() {
        return "ClusterTemplate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", datalakeRequired=" + datalakeRequired +
                ", featureState=" + featureState +
                ", type=" + type +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", clouderaRuntimeVersion='" + clouderaRuntimeVersion + '\'' +
                ", created=" + created +
                '}';
    }
}
