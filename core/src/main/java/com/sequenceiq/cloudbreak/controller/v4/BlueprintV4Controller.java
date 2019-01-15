package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.filters.RecommendationV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.common.NotificationController;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.util.ConverterUtil;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Blueprint.class)
public class BlueprintV4Controller extends NotificationController implements BlueprintV4Endpoint {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public BlueprintV4ViewResponses list(Long workspaceId) {
        Set<BlueprintView> allAvailableViewInWorkspace = blueprintService.getAllAvailableViewInWorkspace(workspaceId);
        return new BlueprintV4ViewResponses(converterUtil.convertAllAsSet(allAvailableViewInWorkspace, BlueprintV4ViewResponse.class));
    }

    @Override
    public BlueprintV4Response get(Long workspaceId, String name) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(blueprint, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Response post(Long workspaceId, BlueprintV4Request request) {
        Blueprint blueprint = blueprintService.createForLoggedInUser(
                conversionService.convert(request, Blueprint.class), workspaceId);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return conversionService.convert(blueprint, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Response delete(Long workspaceId, String name) {
        Blueprint deleted = blueprintService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return conversionService.convert(deleted, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Request getRequest(Long workspaceId, String name) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(blueprint, BlueprintV4Request.class);
    }

    @Override
    public ParametersQueryV4Response getParameters(Long workspaceId, String name) {
        ParametersQueryV4Response parametersQueryV4Response = new ParametersQueryV4Response();
        parametersQueryV4Response.setCustom(blueprintService.queryCustomParametersMap(name, workspaceId));
        return parametersQueryV4Response;
    }

    @Override
    public RecommendationV4Response createRecommendation(Long workspaceId, RecommendationV4Filter recommendationV4Filter) {
        return conversionService.convert(platformParameterService.getRecommendation(workspaceId, recommendationV4Filter.getBlueprintName(),
                recommendationV4Filter), RecommendationV4Response.class);
    }

}
