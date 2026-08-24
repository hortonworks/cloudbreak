package com.sequenceiq.cloudbreak.controller.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintServicesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.SupportedServiceV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.SupportedServicesToBlueprintServicesV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
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
    private BlueprintService blueprintService;

    @Mock
    private CloudbreakRestRequestThreadLocalService threadLocalService;

    @Mock
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Mock
    private SupportedServicesToBlueprintServicesV4ResponseConverter supportedServicesToBlueprintServicesV4ResponseConverter;

    @InjectMocks
    private BlueprintUtilV4Controller underTest;

    @Test
    void createRecommendationByCredCrnTest() {
        when(threadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        RecommendationV4Response recommendationV4Response = new RecommendationV4Response();
        when(blueprintService.getRecommendationByCredentialCrn(WORKSPACE_ID, BLUEPRINT_NAME, BLUEPRINT_NAME,
                CREDENTIAL_CRN, REGION, PLATFORM_VARIANT, AVAILABILITY_ZONE,
                RESOURCE_TYPE)).thenReturn(recommendationV4Response);

        RecommendationV4Response result = underTest.createRecommendationByCredCrn(WORKSPACE_ID, BLUEPRINT_NAME,
                BLUEPRINT_NAME, CREDENTIAL_CRN, REGION,
                PLATFORM_VARIANT, AVAILABILITY_ZONE, RESOURCE_TYPE);

        assertThat(result).isSameAs(recommendationV4Response);
    }

    @Test
    void getServicesByBlueprintCallsSsoAndNonSsoMethod() {
        when(threadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{\"cdhVersion\":\"7.3.2\"}");
        when(blueprintService.getByNameForWorkspaceId(BLUEPRINT_NAME, WORKSPACE_ID)).thenReturn(blueprint);
        SupportedServices supportedServices = new SupportedServices();
        when(clusterTemplateGeneratorService.getServicesByBlueprintWithSsoAndNonSso(blueprint.getBlueprintJsonText()))
                .thenReturn(supportedServices);
        BlueprintServicesV4Response expectedResponse = new BlueprintServicesV4Response();
        when(supportedServicesToBlueprintServicesV4ResponseConverter.convert(supportedServices))
                .thenReturn(expectedResponse);

        BlueprintServicesV4Response result = underTest.getServicesByBlueprint(WORKSPACE_ID, BLUEPRINT_NAME);

        assertThat(result).isSameAs(expectedResponse);
        verify(clusterTemplateGeneratorService).getServicesByBlueprintWithSsoAndNonSso(blueprint.getBlueprintJsonText());
    }

    @Test
    void getServicesByBlueprintDeduplicatesByName() {
        when(threadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{\"cdhVersion\":\"7.3.2\"}");
        when(blueprintService.getByNameForWorkspaceId(BLUEPRINT_NAME, WORKSPACE_ID)).thenReturn(blueprint);
        SupportedServices supportedServices = new SupportedServices();
        when(clusterTemplateGeneratorService.getServicesByBlueprintWithSsoAndNonSso(blueprint.getBlueprintJsonText()))
                .thenReturn(supportedServices);

        // Build a response with duplicate service names (different displayName)
        SupportedServiceV4Response svc1 = new SupportedServiceV4Response();
        svc1.setName("HIVE");
        svc1.setDisplayName("Hive (SSO)");
        SupportedServiceV4Response svc2 = new SupportedServiceV4Response();
        svc2.setName("HIVE");
        svc2.setDisplayName("Hive (non-SSO)");
        SupportedServiceV4Response svc3 = new SupportedServiceV4Response();
        svc3.setName("RANGER");
        svc3.setDisplayName("Ranger");

        BlueprintServicesV4Response responseWithDuplicates = new BlueprintServicesV4Response();
        Set<SupportedServiceV4Response> services = new java.util.LinkedHashSet<>();
        services.add(svc1);
        services.add(svc2);
        services.add(svc3);
        responseWithDuplicates.setServices(services);
        when(supportedServicesToBlueprintServicesV4ResponseConverter.convert(supportedServices))
                .thenReturn(responseWithDuplicates);

        BlueprintServicesV4Response result = underTest.getServicesByBlueprint(WORKSPACE_ID, BLUEPRINT_NAME);

        assertThat(result.getServices()).hasSize(2);
        assertThat(result.getServices())
                .extracting(SupportedServiceV4Response::getName)
                .containsExactlyInAnyOrder("HIVE", "RANGER");
    }

}