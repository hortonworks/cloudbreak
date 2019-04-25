package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplateView;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;

@Component
public class ClusterTemplateViewToClusterTemplateViewV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<ClusterTemplateView, ClusterTemplateViewV4Response> {

    @Override
    public ClusterTemplateViewV4Response convert(ClusterTemplateView source) {
        ClusterTemplateViewV4Response clusterTemplateViewV4Response = new ClusterTemplateViewV4Response();
        clusterTemplateViewV4Response.setName(source.getName());
        clusterTemplateViewV4Response.setDescription(source.getDescription());
        clusterTemplateViewV4Response.setCloudPlatform(source.getCloudPlatform());
        clusterTemplateViewV4Response.setStatus(source.getStatus());
        clusterTemplateViewV4Response.setId(source.getId());
        clusterTemplateViewV4Response.setDatalakeRequired(source.getDatalakeRequired());
        clusterTemplateViewV4Response.setStatus(source.getStatus());
        clusterTemplateViewV4Response.setType(source.getType());
        clusterTemplateViewV4Response.setNodeCount(source.getFullNodeCount());
        if (source.getStackTemplate() != null) {
            StackApiView stackTemplate = source.getStackTemplate();
            if (stackTemplate.getCluster() != null) {
                ClusterApiView cluster = stackTemplate.getCluster();
                clusterTemplateViewV4Response.setStackType(cluster.getBlueprint() != null ? cluster.getBlueprint().getStackType() : "");
                clusterTemplateViewV4Response.setStackVersion(cluster.getBlueprint() != null ? cluster.getBlueprint().getStackVersion() : "");
            }
            if (stackTemplate.getEnvironment() != null) {
                clusterTemplateViewV4Response.setEnvironmentName(stackTemplate.getEnvironment().getName());
            }
        }
        return clusterTemplateViewV4Response;
    }
}
