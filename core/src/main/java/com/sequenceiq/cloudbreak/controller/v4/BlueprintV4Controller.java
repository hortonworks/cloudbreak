package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.RecommendationV4Response;
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
public class BlueprintV4Controller extends NotificationController implements BlueprintV4Endpoint {

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public BlueprintV4ViewResponses list(Long workspaceId) {
        Set<ClusterDefinitionView> allAvailableViewInWorkspace = clusterDefinitionService.getAllAvailableViewInWorkspace(workspaceId);
        return new BlueprintV4ViewResponses(converterUtil.convertAllAsSet(allAvailableViewInWorkspace, BlueprintV4ViewResponse.class));
    }

    @Override
    public BlueprintV4Response get(Long workspaceId, String name) {
        ClusterDefinition clusterDefinition = clusterDefinitionService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(clusterDefinition, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Response post(Long workspaceId, BlueprintV4Request request) {
        ClusterDefinition clusterDefinition = clusterDefinitionService.createForLoggedInUser(
                converterUtil.convert(request, ClusterDefinition.class), workspaceId);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return converterUtil.convert(clusterDefinition, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Response delete(Long workspaceId, String name) {
        ClusterDefinition deleted = clusterDefinitionService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return converterUtil.convert(deleted, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Request getRequest(Long workspaceId, String name) {
        ClusterDefinition clusterDefinition = clusterDefinitionService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(clusterDefinition, BlueprintV4Request.class);
    }

    @Override
    public ParametersQueryV4Response getParameters(Long workspaceId, String name) {
        ParametersQueryV4Response parametersQueryV4Response = new ParametersQueryV4Response();
        parametersQueryV4Response.setCustom(clusterDefinitionService.queryCustomParametersMap(name, workspaceId));
        return parametersQueryV4Response;
    }

    @Override
    public RecommendationV4Response createRecommendation(Long workspaceId, String blueprintName, String credentialName,
            String region, String platformVariant, String availabilityZone) {
        return converterUtil.convert(platformParameterService.getRecommendation(workspaceId, blueprintName,
                credentialName, region, platformVariant, availabilityZone), RecommendationV4Response.class);
    }

}
