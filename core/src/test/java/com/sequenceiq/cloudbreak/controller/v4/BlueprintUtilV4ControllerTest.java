package com.sequenceiq.cloudbreak.controller.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.GeneratedCmTemplateToGeneratedCmTemplateV4Response;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.PlatformRecommendationToPlatformRecommendationV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.ScaleRecommendationToScaleRecommendationV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.ServiceDependencyMatrixToServiceDependencyMatrixV4Response;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.SupportedServicesToBlueprintServicesV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.SupportedVersionsToSupportedVersionsV4ResponseConverter;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.api.type.CdpResourceType;

@ExtendWith(MockitoExtension.class)
class BlueprintUtilV4ControllerTest {

    private static final Long WORKSPACE_ID = 12L;

    private static final String BLUEPRINT_NAME = "blueprintName";

    private static final String CREDENTIAL_CRN = "credentialCrn";

    private static final String REGION = "region";

    private static final String PLATFORM_VARIANT = "platformVariant";

    private static final String AVAILABILITY_ZONE = "availabilityZone";

    private static final CdpResourceType RESOURCE_TYPE = CdpResourceType.DATAHUB;

    @Mock
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Mock
    private ScaleRecommendationToScaleRecommendationV4ResponseConverter scaleRecommendationToScaleRecommendationV4ResponseConverter;

    @Mock
    private PlatformRecommendationToPlatformRecommendationV4ResponseConverter platformRecommendationToPlatformRecommendationV4ResponseConverter;

    @Mock
    private ServiceDependencyMatrixToServiceDependencyMatrixV4Response serviceDependencyMatrixToServiceDependencyMatrixV4Response;

    @Mock
    private SupportedVersionsToSupportedVersionsV4ResponseConverter supportedVersionsToSupportedVersionsV4ResponseConverter;

    @Mock
    private SupportedServicesToBlueprintServicesV4ResponseConverter supportedServicesToBlueprintServicesV4ResponseConverter;

    @Mock
    private GeneratedCmTemplateToGeneratedCmTemplateV4Response generatedCmTemplateToGeneratedCmTemplateV4Response;

    @InjectMocks
    private BlueprintUtilV4Controller underTest;

    @Mock
    private PlatformRecommendation recommendation;

    @Test
    void createRecommendationByCredCrnTest() {
        when(threadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(blueprintService.getRecommendationByCredentialCrn(WORKSPACE_ID, BLUEPRINT_NAME, CREDENTIAL_CRN, REGION, PLATFORM_VARIANT, AVAILABILITY_ZONE,
                RESOURCE_TYPE)).thenReturn(recommendation);
        RecommendationV4Response recommendationV4Response = new RecommendationV4Response();
        when(platformRecommendationToPlatformRecommendationV4ResponseConverter.convert(recommendation)).thenReturn(recommendationV4Response);

        RecommendationV4Response result = underTest.createRecommendationByCredCrn(WORKSPACE_ID, BLUEPRINT_NAME, CREDENTIAL_CRN, REGION,
                PLATFORM_VARIANT, AVAILABILITY_ZONE, RESOURCE_TYPE);

        assertThat(result).isSameAs(recommendationV4Response);
    }

}