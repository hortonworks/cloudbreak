package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.GeneratedCmTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.ServiceDependencyMatrixV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.SupportedVersionsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ParametersQueryV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Blueprint.class)
public class BlueprintV4Controller extends NotificationController implements BlueprintV4Endpoint, BlueprintUtilV4Endpoint {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public BlueprintV4ViewResponses list(Long workspaceId) {
        Set<Blueprint> allAvailableViewInWorkspace = blueprintService.getAllAvailableViewInWorkspace(workspaceId);
        return new BlueprintV4ViewResponses(converterUtil.convertAllAsSet(allAvailableViewInWorkspace, BlueprintV4ViewResponse.class));
    }

    @Override
    public BlueprintV4Response get(Long workspaceId, String name) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(blueprint, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Response post(Long workspaceId, BlueprintV4Request request) {
        Blueprint blueprint = blueprintService.createForLoggedInUser(
                converterUtil.convert(request, Blueprint.class), workspaceId);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return converterUtil.convert(blueprint, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Response delete(Long workspaceId, String name) {
        Blueprint deleted = blueprintService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return converterUtil.convert(deleted, BlueprintV4Response.class);
    }

    @Override
    public BlueprintV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
        Set<Blueprint> deleted = blueprintService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return new BlueprintV4Responses(converterUtil.convertAllAsSet(deleted, BlueprintV4Response.class));
    }

    @Override
    public BlueprintV4Request getRequest(Long workspaceId, String name) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(blueprint, BlueprintV4Request.class);
    }

    @Override
    public ParametersQueryV4Response getParameters(Long workspaceId, String name) {
        ParametersQueryV4Response parametersQueryV4Response = new ParametersQueryV4Response();
        parametersQueryV4Response.setCustom(blueprintService.queryCustomParametersMap(name, workspaceId));
        return parametersQueryV4Response;
    }

    @Override
    public RecommendationV4Response createRecommendation(Long workspaceId, String blueprintName, String credentialName,
            String region, String platformVariant, String availabilityZone) {
        return converterUtil.convert(platformParameterService.getRecommendation(workspaceId, blueprintName,
                credentialName, region, platformVariant, availabilityZone), RecommendationV4Response.class);
    }

    @Override
    public ServiceDependencyMatrixV4Response getServiceAndDependencies(Long workspaceId, Set<String> services,
        String platform) {
        return converterUtil.convert(clusterTemplateGeneratorService.getServicesAndDependencies(services, platform),
                ServiceDependencyMatrixV4Response.class);
    }

    @Override
    public SupportedVersionsV4Response getServiceList(Long workspaceId) {
        return converterUtil.convert(clusterTemplateGeneratorService.getVersionsAndSupportedServiceList(),
                SupportedVersionsV4Response.class);
    }

    @Override
    public GeneratedCmTemplateV4Response getGeneratedTemplate(Long workspaceId, Set<String> services, String platform) {
        return converterUtil.convert(clusterTemplateGeneratorService.generateTemplateByServices(services, platform),
                GeneratedCmTemplateV4Response.class);
    }

}
