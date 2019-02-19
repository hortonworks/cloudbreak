package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.ClusterDefinitionV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.requests.ClusterDefinitionV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.view.ClusterDefinitionView;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ClusterDefinition.class)
public class ClusterDefinitionV4Controller extends NotificationController implements ClusterDefinitionV4Endpoint {

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public ClusterDefinitionV4ViewResponses list(Long workspaceId) {
        Set<ClusterDefinitionView> allAvailableViewInWorkspace = clusterDefinitionService.getAllAvailableViewInWorkspace(workspaceId);
        return new ClusterDefinitionV4ViewResponses(converterUtil.convertAllAsSet(allAvailableViewInWorkspace, ClusterDefinitionV4ViewResponse.class));
    }

    @Override
    public ClusterDefinitionV4Response get(Long workspaceId, String name) {
        ClusterDefinition clusterDefinition = clusterDefinitionService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(clusterDefinition, ClusterDefinitionV4Response.class);
    }

    @Override
    public ClusterDefinitionV4Response post(Long workspaceId, ClusterDefinitionV4Request request) {
        ClusterDefinition clusterDefinition = clusterDefinitionService.createForLoggedInUser(
                converterUtil.convert(request, ClusterDefinition.class), workspaceId);
        notify(ResourceEvent.CLUSTER_DEFINITION_CREATED);
        return converterUtil.convert(clusterDefinition, ClusterDefinitionV4Response.class);
    }

    @Override
    public ClusterDefinitionV4Response delete(Long workspaceId, String name) {
        ClusterDefinition deleted = clusterDefinitionService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.CLUSTER_DEFINITION_DELETED);
        return converterUtil.convert(deleted, ClusterDefinitionV4Response.class);
    }

    @Override
    public ClusterDefinitionV4Request getRequest(Long workspaceId, String name) {
        ClusterDefinition clusterDefinition = clusterDefinitionService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(clusterDefinition, ClusterDefinitionV4Request.class);
    }

    @Override
    public ParametersQueryV4Response getParameters(Long workspaceId, String name) {
        ParametersQueryV4Response parametersQueryV4Response = new ParametersQueryV4Response();
        parametersQueryV4Response.setCustom(clusterDefinitionService.queryCustomParametersMap(name, workspaceId));
        return parametersQueryV4Response;
    }

    @Override
    public RecommendationV4Response createRecommendation(Long workspaceId, String clusterDefinitionName, String credentialName,
            String region, String platformVariant, String availabilityZone) {
        return converterUtil.convert(platformParameterService.getRecommendation(workspaceId, clusterDefinitionName,
                credentialName, region, platformVariant, availabilityZone), RecommendationV4Response.class);
    }

}
