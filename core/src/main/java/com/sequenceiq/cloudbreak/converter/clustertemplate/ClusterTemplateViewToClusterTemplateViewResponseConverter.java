package com.sequenceiq.cloudbreak.converter.clustertemplate;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateViewResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplateView;

@Component
public class ClusterTemplateViewToClusterTemplateViewResponseConverter
        extends AbstractConversionServiceAwareConverter<ClusterTemplateView, ClusterTemplateViewResponse> {

    @Override
    public ClusterTemplateViewResponse convert(ClusterTemplateView source) {
        ClusterTemplateViewResponse clusterTemplateViewResponse = new ClusterTemplateViewResponse();
        clusterTemplateViewResponse.setName(source.getName());
        clusterTemplateViewResponse.setDescription(source.getDescription());
        clusterTemplateViewResponse.setCloudPlatform(source.getCloudPlatform());
        clusterTemplateViewResponse.setStatus(source.getStatus());
        clusterTemplateViewResponse.setId(source.getId());
        clusterTemplateViewResponse.setDatalakeRequired(source.getDatalakeRequired());
        clusterTemplateViewResponse.setStatus(source.getStatus());
        clusterTemplateViewResponse.setType(source.getType());
        clusterTemplateViewResponse.setNodeCount(source.getFullNodeCount());
        clusterTemplateViewResponse.setStackType(source.getStackTemplate().getCluster().getBlueprint().getStackType());
        clusterTemplateViewResponse.setStackVersion(source.getStackTemplate().getCluster().getBlueprint().getStackVersion());
        return clusterTemplateViewResponse;
    }
}
