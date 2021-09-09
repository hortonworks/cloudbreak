package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateClusterApiView;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateInstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateStackApiView;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;

@Component
public class ClusterTemplateViewToClusterTemplateViewV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateViewToClusterTemplateViewV4ResponseConverter.class);

    public ClusterTemplateViewV4Response convert(ClusterTemplateView source) {
        ClusterTemplateViewV4Response clusterTemplateViewV4Response = new ClusterTemplateViewV4Response();
        clusterTemplateViewV4Response.setName(source.getName());
        clusterTemplateViewV4Response.setDescription(source.getDescription());
        clusterTemplateViewV4Response.setCrn(source.getResourceCrn());
        clusterTemplateViewV4Response.setCloudPlatform(source.getCloudPlatform());
        clusterTemplateViewV4Response.setStatus(source.getStatus());
        clusterTemplateViewV4Response.setId(source.getId());
        clusterTemplateViewV4Response.setDatalakeRequired(source.getDatalakeRequired());
        clusterTemplateViewV4Response.setStatus(source.getStatus());
        clusterTemplateViewV4Response.setType(source.getType());
        clusterTemplateViewV4Response.setFeatureState(source.getFeatureState());
        if (source.getStackTemplate() != null) {
            ClusterTemplateStackApiView stackTemplate = source.getStackTemplate();
            clusterTemplateViewV4Response.setNodeCount(getFullNodeCount(stackTemplate));
            if (stackTemplate.getCluster() != null) {
                ClusterTemplateClusterApiView cluster = stackTemplate.getCluster();
                clusterTemplateViewV4Response.setStackType(cluster.getBlueprint() != null ? cluster.getBlueprint().getStackType() : "");
                clusterTemplateViewV4Response.setStackVersion(cluster.getBlueprint() != null ? cluster.getBlueprint().getStackVersion() : "");
            }
            if (stackTemplate.getEnvironmentCrn() != null) {
                clusterTemplateViewV4Response.setEnvironmentCrn(stackTemplate.getEnvironmentCrn());
            }
        } else if (source.getStatus().isDefault()) {
            try {
                DistroXV1Request distroXV1Request = new Json(getTemplateString(source.getTemplateContent()))
                        .get(DefaultClusterTemplateV4Request.class)
                        .getDistroXTemplate();
                clusterTemplateViewV4Response.setNodeCount(getFullNodeCount(distroXV1Request));
                clusterTemplateViewV4Response.setStackType("CDH");
                clusterTemplateViewV4Response.setStackVersion(source.getClouderaRuntimeVersion());
            } catch (IOException e) {
                LOGGER.error("CDP was not able to convert back {} template: {}", source.getName(), e.getMessage());
                throw new CloudbreakServiceException("CDP was not able to give back your template: ", e);
            }

        }
        clusterTemplateViewV4Response.setCreated(source.getCreated());
        return clusterTemplateViewV4Response;
    }

    public Integer getFullNodeCount(ClusterTemplateStackApiView stackTemplate) {
        return stackTemplate.getInstanceGroups()
                .stream()
                .mapToInt(ClusterTemplateInstanceGroupView::getInitialNodeCount)
                .sum();

    }

    public Integer getFullNodeCount(DistroXV1Request distroXV1Request) {
        int nodeCount = 0;
        for (InstanceGroupV1Request instanceGroup : distroXV1Request.getInstanceGroups()) {
            nodeCount += instanceGroup.getNodeCount();
        }
        return nodeCount;
    }

    private String getTemplateString(String clusterTemplate) {
        return new String(BaseEncoding.base64().decode(clusterTemplate));
    }

}
