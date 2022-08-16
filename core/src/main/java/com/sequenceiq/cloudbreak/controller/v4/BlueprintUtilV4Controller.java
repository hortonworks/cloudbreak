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
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.ScaleRecommendation;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.GeneratedCmTemplateToGeneratedCmTemplateV4Response;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.PlatformRecommendationToPlatformRecommendationV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.ScaleRecommendationToScaleRecommendationV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.ServiceDependencyMatrixToServiceDependencyMatrixV4Response;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.SupportedServicesToBlueprintServicesV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.SupportedVersionsToSupportedVersionsV4ResponseConverter;
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
    private BlueprintService blueprintService;

    @Inject
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Inject
    private ScaleRecommendationToScaleRecommendationV4ResponseConverter scaleRecommendationToScaleRecommendationV4ResponseConverter;

    @Inject
    private PlatformRecommendationToPlatformRecommendationV4ResponseConverter platformRecommendationToPlatformRecommendationV4ResponseConverter;

    @Inject
    private ServiceDependencyMatrixToServiceDependencyMatrixV4Response serviceDependencyMatrixToServiceDependencyMatrixV4Response;

    @Inject
    private SupportedVersionsToSupportedVersionsV4ResponseConverter supportedVersionsToSupportedVersionsV4ResponseConverter;

    @Inject
    private SupportedServicesToBlueprintServicesV4ResponseConverter supportedServicesToBlueprintServicesV4ResponseConverter;

    @Inject
    private GeneratedCmTemplateToGeneratedCmTemplateV4Response generatedCmTemplateToGeneratedCmTemplateV4Response;

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
        return platformRecommendationToPlatformRecommendationV4ResponseConverter.convert(recommendation);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
    public RecommendationV4Response createRecommendationByCredCrn(Long workspaceId, String blueprintName,
            @TenantAwareParam @ResourceCrn String credentialCrn, String region, String platformVariant,
            String availabilityZone, CdpResourceType resourceType) {
        PlatformRecommendation recommendation = blueprintService.getRecommendationByCredentialCrn(
                threadLocalService.getRequestedWorkspaceId(),
                blueprintName,
                credentialCrn,
                region,
                platformVariant,
                availabilityZone,
                resourceType);
        return platformRecommendationToPlatformRecommendationV4ResponseConverter.convert(recommendation);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public ScaleRecommendationV4Response createRecommendation(Long workspaceId, @ResourceName String blueprintName) {
        ScaleRecommendation recommendation = blueprintService.getScaleRecommendation(
                threadLocalService.getRequestedWorkspaceId(),
                blueprintName);
        return scaleRecommendationToScaleRecommendationV4ResponseConverter.convert(recommendation);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public ScaleRecommendationV4Response createRecommendationByDatahubCrn(Long workspaceId, @ResourceCrn String datahubCrn) {
        ScaleRecommendation recommendation = blueprintService.getScaleRecommendationByDatahubCrn(
                threadLocalService.getRequestedWorkspaceId(),
                datahubCrn);
        return scaleRecommendationToScaleRecommendationV4ResponseConverter.convert(recommendation);
    }

    @Override
    @DisableCheckPermissions
    public ServiceDependencyMatrixV4Response getServiceAndDependencies(Long workspaceId, Set<String> services,
            String platform) {
        return serviceDependencyMatrixToServiceDependencyMatrixV4Response
                .convert(clusterTemplateGeneratorService.getServicesAndDependencies(services, platform));
    }

    @Override
    @DisableCheckPermissions
    public SupportedVersionsV4Response getServiceList(Long workspaceId) {
        return supportedVersionsToSupportedVersionsV4ResponseConverter.convert(clusterTemplateGeneratorService.getVersionsAndSupportedServiceList());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
    public BlueprintServicesV4Response getServicesByBlueprint(Long workspaceId, @ResourceName String blueprintName) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(blueprintName, threadLocalService.getRequestedWorkspaceId());
        return supportedServicesToBlueprintServicesV4ResponseConverter
                .convert(clusterTemplateGeneratorService.getServicesByBlueprint(blueprint.getBlueprintText()));
    }

    @Override
    @DisableCheckPermissions
    public GeneratedCmTemplateV4Response getGeneratedTemplate(Long workspaceId, Set<String> services, String platform) {
        return generatedCmTemplateToGeneratedCmTemplateV4Response.convert(clusterTemplateGeneratorService.generateTemplateByServices(services, platform));
    }

}
