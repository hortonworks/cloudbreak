package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintServicesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.GeneratedCmTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.ServiceDependencyMatrixV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.SupportedVersionsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.ScaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.ScaleRecommendation;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.common.api.type.CdpResourceType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(Blueprint.class)
public class BlueprintUtilV4Controller extends NotificationController implements BlueprintUtilV4Endpoint {

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    /**
     * @deprecated Do not use it, we can't use Credential's Name by themselves with internalCrn's
     */
    @Override
    @Deprecated
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public RecommendationV4Response createRecommendation(Long workspaceId, String blueprintName, @ResourceName String credentialName,
            String region, String platformVariant, String availabilityZone, CdpResourceType cdpResourceType) {
        PlatformRecommendation recommendation = blueprintService.getRecommendation(
                threadLocalService.getRequestedWorkspaceId(),
                blueprintName,
                credentialName,
                region,
                platformVariant,
                availabilityZone,
                cdpResourceType);
        return converterUtil.convert(recommendation, RecommendationV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public RecommendationV4Response createRecommendationByCredCrn(Long workspaceId, String blueprintName,
            @TenantAwareParam @ResourceCrn String credentialCrn, String region, String platformVariant,
            String availabilityZone, CdpResourceType cdpResourceType) {
        PlatformRecommendation recommendation = blueprintService.getRecommendationByCredentialCrn(
                threadLocalService.getRequestedWorkspaceId(),
                blueprintName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                cdpResourceType);
        return converterUtil.convert(recommendation, RecommendationV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public ScaleRecommendationV4Response createRecommendation(Long workspaceId, @ResourceName String blueprintName) {
        ScaleRecommendation recommendation = blueprintService.getScaleRecommendation(
                threadLocalService.getRequestedWorkspaceId(),
                blueprintName);
        return converterUtil.convert(recommendation, ScaleRecommendationV4Response.class);
    }

    @Override
    @DisableCheckPermissions
    public ServiceDependencyMatrixV4Response getServiceAndDependencies(Long workspaceId, Set<String> services,
            String platform) {
        return converterUtil.convert(clusterTemplateGeneratorService.getServicesAndDependencies(services, platform),
                ServiceDependencyMatrixV4Response.class);
    }

    @Override
    @DisableCheckPermissions
    public SupportedVersionsV4Response getServiceList(Long workspaceId) {
        return converterUtil.convert(clusterTemplateGeneratorService.getVersionsAndSupportedServiceList(),
                SupportedVersionsV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public BlueprintServicesV4Response getServicesByBlueprint(Long workspaceId, @ResourceName String blueprintName) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(blueprintName, threadLocalService.getRequestedWorkspaceId());
        return converterUtil.convert(clusterTemplateGeneratorService.getServicesByBlueprint(blueprint.getBlueprintText()),
                BlueprintServicesV4Response.class);
    }

    @Override
    @DisableCheckPermissions
    public GeneratedCmTemplateV4Response getGeneratedTemplate(Long workspaceId, Set<String> services, String platform) {
        return converterUtil.convert(clusterTemplateGeneratorService.generateTemplateByServices(services, platform),
                GeneratedCmTemplateV4Response.class);
    }

}
