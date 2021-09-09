package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.init.clustertemplate.DefaultClusterTemplateCache;

@Component
public class DefaultClusterTemplateV4RequestToClusterTemplateConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClusterTemplateV4RequestToClusterTemplateConverter.class);

    @Inject
    private DefaultClusterTemplateCache defaultClusterTemplateCache;

    public ClusterTemplate convert(DefaultClusterTemplateV4Request source) {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setTemplateContent(defaultClusterTemplateCache.getByName(source.getName()));
        clusterTemplate.setCloudPlatform(getCloudPlatform(source));
        clusterTemplate.setName(source.getName());
        clusterTemplate.setFeatureState(source.getFeatureState() == null ? FeatureState.RELEASED : source.getFeatureState());
        clusterTemplate.setDescription(source.getDescription());
        clusterTemplate.setStatus(ResourceStatus.DEFAULT);
        if (source.getType() == null) {
            clusterTemplate.setType(ClusterTemplateV4Type.OTHER);
        } else {
            clusterTemplate.setType(source.getType());
        }
        clusterTemplate.setDatalakeRequired(source.getDatalakeRequired());
        return clusterTemplate;
    }

    private String getCloudPlatform(DefaultClusterTemplateV4Request source) {
        if (source.getCloudPlatform() != null) {
            return source.getCloudPlatform();
        } else {
            throw new CloudbreakServiceException(String.format("Cluster Defintion with name %s has no cloud provider", source.getName()));
        }
    }
}
