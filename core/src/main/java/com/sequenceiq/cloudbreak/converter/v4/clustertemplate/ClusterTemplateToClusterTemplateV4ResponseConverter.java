package com.sequenceiq.cloudbreak.converter.v4.clustertemplate;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.StackToStackV4RequestConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.service.stack.StackTemplateService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;

@Component
public class ClusterTemplateToClusterTemplateV4ResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateToClusterTemplateV4ResponseConverter.class);

    @Inject
    private StackTemplateService stackTemplateService;

    @Inject
    private DistroXV1RequestToStackV4RequestConverter stackV4RequestConverter;

    @Inject
    private StackToStackV4RequestConverter stackToStackV4RequestConverter;

    public ClusterTemplateV4Response convert(ClusterTemplate source) {
        ClusterTemplateV4Response clusterTemplateV4Response = new ClusterTemplateV4Response();
        clusterTemplateV4Response.setName(source.getName());
        clusterTemplateV4Response.setDescription(source.getDescription());
        if (source.getStatus().isNonDefault()) {
            Optional<Stack> stack = stackTemplateService.getByIdWithLists(source.getStackTemplate().getId());
            if (stack.isPresent()) {
                StackV4Request stackV4Request;
                try {
                    stackV4Request = stackToStackV4RequestConverter.convert(stack.get());
                } catch (Exception e) {
                    stackV4Request = null;
                }
                clusterTemplateV4Response.setDistroXTemplate(getIfNotNull(stackV4Request, stackV4RequestConverter::convert));
                clusterTemplateV4Response.setNodeCount(stack.get().getFullNodeCount());
            }
        } else {
            try {
                DefaultClusterTemplateV4Request clusterTemplateV4Request = new Json(getTemplateString(source))
                        .get(DefaultClusterTemplateV4Request.class);
                clusterTemplateV4Response.setDistroXTemplate(clusterTemplateV4Request.getDistroXTemplate());
                clusterTemplateV4Response.setNodeCount(getFullNodeCount(clusterTemplateV4Request.getDistroXTemplate()));
            } catch (IOException e) {
                LOGGER.info("There is no Data Hub template (stack entity missing) for cluster defintion {}", source.getName());
                clusterTemplateV4Response.setDistroXTemplate(null);
            }
        }
        clusterTemplateV4Response.setCloudPlatform(source.getCloudPlatform());
        clusterTemplateV4Response.setStatus(source.getStatus());
        clusterTemplateV4Response.setId(source.getId());
        clusterTemplateV4Response.setDatalakeRequired(source.getDatalakeRequired());
        clusterTemplateV4Response.setCrn(source.getResourceCrn());
        clusterTemplateV4Response.setStatus(source.getStatus());
        clusterTemplateV4Response.setType(source.getType());
        clusterTemplateV4Response.setFeatureState(source.getFeatureState());
        if (source.getStackTemplate() != null) {
            Stack stackTemplate = source.getStackTemplate();
            if (stackTemplate.getEnvironmentCrn() != null) {
                clusterTemplateV4Response.setEnvironmentCrn(stackTemplate.getEnvironmentCrn());
            }
            if (source.getStackTemplate().getCluster() != null && source.getStackTemplate().getCluster().getBlueprint() != null) {
                clusterTemplateV4Response.setStackType(source.getStackTemplate().getCluster().getBlueprint().getStackType());
                clusterTemplateV4Response.setStackVersion(source.getStackTemplate().getCluster().getBlueprint().getStackVersion());
            }
        } else {
            clusterTemplateV4Response.setStackType("CDH");
            clusterTemplateV4Response.setStackVersion(source.getClouderaRuntimeVersion());
        }
        clusterTemplateV4Response.setCreated(source.getCreated());
        return clusterTemplateV4Response;
    }

    private String getTemplateString(ClusterTemplate clusterTemplate) {
        return new String(BaseEncoding.base64().decode(clusterTemplate.getTemplateContent()));
    }

    public Integer getFullNodeCount(DistroXV1Request distroXV1Request) {
        int nodeCount = 0;
        for (InstanceGroupV1Request instanceGroup : distroXV1Request.getInstanceGroups()) {
            nodeCount += instanceGroup.getNodeCount();
        }
        return nodeCount;
    }

}
