package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.converter.ClusterTemplateV4TypeConverter;
import com.sequenceiq.cloudbreak.domain.converter.DatalakeRequiredConverter;
import com.sequenceiq.cloudbreak.domain.converter.FeatureStateConverter;
import com.sequenceiq.cloudbreak.domain.converter.ResourceStatusConverter;

@Entity
@Table(name = "ClusterTemplate")
public class ClusterTemplateView extends CompactView {

    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus status;

    private String cloudPlatform;

    private String resourceCrn;

    @Convert(converter = DatalakeRequiredConverter.class)
    private DatalakeRequired datalakeRequired;

    @Convert(converter = ClusterTemplateV4TypeConverter.class)
    private ClusterTemplateV4Type type;

    @Convert(converter = FeatureStateConverter.class)
    private FeatureState featureState;

    @OneToOne
    private ClusterTemplateStackApiView stackTemplate;

    private String templateContent;

    private Long created;

    private String clouderaRuntimeVersion;

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

    public ClusterTemplateStackApiView getStackTemplate() {
        return stackTemplate;
    }

    public void setStackTemplate(ClusterTemplateStackApiView stackTemplate) {
        this.stackTemplate = stackTemplate;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public FeatureState getFeatureState() {
        return featureState;
    }

    public void setFeatureState(FeatureState featureState) {
        this.featureState = featureState;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public String getClouderaRuntimeVersion() {
        return clouderaRuntimeVersion;
    }

    public void setClouderaRuntimeVersion(String clouderaRuntimeVersion) {
        this.clouderaRuntimeVersion = clouderaRuntimeVersion;
    }

    @Override
    public String toString() {
        return "ClusterTemplateView{" +
                "status=" + status +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", datalakeRequired=" + datalakeRequired +
                ", type=" + type +
                ", featureState=" + featureState +
                ", created=" + created +
                ", clouderaRuntimeVersion='" + clouderaRuntimeVersion + '\'' +
                '}';
    }
}
