package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.co2.ClusterCO2V4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.ClusterCostV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;

@ExtendWith(MockitoExtension.class)
public class SdxCostServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private ClusterCostV4Endpoint clusterCostV4Endpoint;

    @Mock
    private ClusterCO2V4Endpoint clusterCO2V4Endpoint;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SdxCostService underTest;

    @Test
    void testIfCostDisabled() {
        when(entitlementService.isUsdCostCalculationEnabled(any())).thenReturn(Boolean.FALSE);

        assertThrows(RuntimeException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getCosts(List.of())));

        verifyNoInteractions(clusterCostV4Endpoint);
    }

    @Test
    void testIfCostEnabled() {
        when(entitlementService.isUsdCostCalculationEnabled(any())).thenReturn(Boolean.TRUE);
        Map<String, RealTimeCost> costMap = Map.of();
        RealTimeCostResponse realTimeCostResponse = new RealTimeCostResponse(costMap);
        when(clusterCostV4Endpoint.list(any(), any())).thenReturn(realTimeCostResponse);

        assertEquals(costMap, ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getCosts(List.of())));
    }

    @Test
    void testIfCO2Disabled() {
        when(entitlementService.isCO2CalculationEnabled(any())).thenReturn(Boolean.FALSE);

        assertThrows(RuntimeException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getCO2(List.of())));

        verifyNoInteractions(clusterCO2V4Endpoint);
    }

    @Test
    void testIfCO2Enabled() {
        when(entitlementService.isCO2CalculationEnabled(any())).thenReturn(Boolean.TRUE);
        Map<String, RealTimeCO2> co2Map = Map.of();
        RealTimeCO2Response realTimeCO2Response = new RealTimeCO2Response(co2Map);
        when(clusterCO2V4Endpoint.list(any(), any())).thenReturn(realTimeCO2Response);

        assertEquals(co2Map, ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getCO2(List.of())));
    }
}
