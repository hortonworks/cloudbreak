package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.ClusterCostV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;

@ExtendWith(MockitoExtension.class)
public class SdxCostServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String INTERNAL_ACTOR = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    @Mock
    private ClusterCostV4Endpoint clusterCostV4Endpoint;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private SdxCostService underTest;

    @Test
    public void testIfDisabled() {
        when(entitlementService.isUsdCostCalculationEnabled(any())).thenReturn(Boolean.FALSE);

        assertThrows(RuntimeException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getCosts(List.of())));

        verifyNoInteractions(clusterCostV4Endpoint, regionAwareInternalCrnGeneratorFactory);
    }

    @Test
    public void testIfEnabled() {
        when(entitlementService.isUsdCostCalculationEnabled(any())).thenReturn(Boolean.TRUE);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_ACTOR);
        RealTimeCostResponse realTimeCostResponse = new RealTimeCostResponse();
        when(clusterCostV4Endpoint.list(any(), any())).thenReturn(realTimeCostResponse);

        assertEquals(realTimeCostResponse, ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getCosts(List.of())));
    }
}
