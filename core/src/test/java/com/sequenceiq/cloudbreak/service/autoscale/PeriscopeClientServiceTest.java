package com.sequenceiq.cloudbreak.service.autoscale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.periscope.api.endpoint.v1.DistroXAutoScaleYarnRecommendationV1Endpoint;
import com.sequenceiq.periscope.api.model.DistroXAutoScaleYarnRecommendationResponse;

@ExtendWith(MockitoExtension.class)
public class PeriscopeClientServiceTest {

    private static final String RESOURCE_CRN = "resource_crn";

    @InjectMocks
    private PeriscopeClientService periscopeClientService;

    @Mock
    private DistroXAutoScaleYarnRecommendationV1Endpoint distroXAutoScaleYarnRecommendationV1Endpoint;

    @Test
    public void testGetYarnRecommendedInstanceIds() throws Exception {
        DistroXAutoScaleYarnRecommendationResponse distroXAutoScaleYarnRecommendationResponse = new DistroXAutoScaleYarnRecommendationResponse();
        List<String> expected = List.of("i - 1");
        distroXAutoScaleYarnRecommendationResponse.setDecommissionNodeIds(expected);
        when(distroXAutoScaleYarnRecommendationV1Endpoint.getYarnRecommendation(RESOURCE_CRN)).thenReturn(distroXAutoScaleYarnRecommendationResponse);

        List<String> result = periscopeClientService.getYarnRecommendedInstanceIds(RESOURCE_CRN);
        assertEquals(result, expected);
    }
}