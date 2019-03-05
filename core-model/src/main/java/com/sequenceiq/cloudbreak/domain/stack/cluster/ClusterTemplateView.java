package com.sequenceiq.cloudbreak.domain.stack.cluster;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.view.CompactView;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;

@Entity
@Table(name = "ClusterTemplate")
public class ClusterTemplateView extends CompactView {

    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    private String cloudPlatform;

    @Enumerated(EnumType.STRING)
    private DatalakeRequired datalakeRequired;

    @Enumerated(EnumType.STRING)
    private ClusterTemplateV4Type type;

    @OneToOne
    private StackApiView stackTemplate;

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.CLUSTER_TEMPLATE;
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

    public StackApiView getStackTemplate() {
        return stackTemplate;
    }

    public void setStackTemplate(StackApiView stackTemplate) {
        this.stackTemplate = stackTemplate;
    }

    public Integer getFullNodeCount() {
        return stackTemplate.getInstanceGroups()
                .stream()
                .mapToInt(InstanceGroupView::getNodeCount)
                .sum();

    }
}
