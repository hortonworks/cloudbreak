package com.sequenceiq.cloudbreak.controller.v4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
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

}