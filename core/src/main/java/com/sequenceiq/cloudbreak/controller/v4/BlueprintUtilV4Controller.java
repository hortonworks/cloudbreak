package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.GeneratedCmTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.ServiceDependencyMatrixV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.SupportedVersionsV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Blueprint.class)
public class BlueprintUtilV4Controller extends NotificationController implements BlueprintUtilV4Endpoint {

    @Inject
    private PlatformParameterService platformParameterService;

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Inject
    private ConverterUtil converterUtil;

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
