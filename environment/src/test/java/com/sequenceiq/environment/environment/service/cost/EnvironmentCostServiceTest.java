package com.sequenceiq.environment.environment.service.cost;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
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
import com.sequenceiq.cloudbreak.common.cost.EnvironmentRealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.freeipa.api.v1.cost.FreeIpaCostV1Endpoint;

@ExtendWith(MockitoExtension.class)
public class EnvironmentCostServiceTest {

    @Mock
    private ClusterCostV4Endpoint clusterCostV4Endpoint;

    @Mock
    private FreeIpaCostV1Endpoint freeIpaCostV1Endpoint;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private EnvironmentCostService underTest;

    @Test
    void getCosts() {
        when(entitlementService.isUsdCostCalculationEnabled(any())).thenReturn(true);
        when(clusterCostV4Endpoint.listByEnv(any(), any())).thenReturn(getRealTimeCostResponseForClusters());
        when(freeIpaCostV1Endpoint.list(any(), any())).thenReturn(getRealTimeCostResponseForFreeipa());
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:iam:us-west-1:1234:user:1");

        ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> {
            Map<String, EnvironmentRealTimeCost> environmentCosts = underTest.getCosts(List.of("ENVIRONMENT_CRN"), List.of("DATALAKE_CRN", "DATAHUB_CRN"));

            Assertions.assertEquals(1, environmentCosts.entrySet().size());
            EnvironmentRealTimeCost environmentRealTimeCost = environmentCosts.get("ENVIRONMENT_CRN");
            Assertions.assertEquals("FREEIPA_NAME", environmentRealTimeCost.getFreeipa().getResourceName());
            Assertions.assertEquals("DATALAKE_NAME", environmentRealTimeCost.getDatalake().getResourceName());
            Assertions.assertEquals(1, environmentRealTimeCost.getDatahubs().size());
            RealTimeCost datahubRealTimeCost = environmentRealTimeCost.getDatahubs().get("DATAHUB_CRN");
            Assertions.assertNotNull(datahubRealTimeCost);
        });
    }

    private RealTimeCostResponse getRealTimeCostResponseForClusters() {
        RealTimeCost datalakeRealTimeCost = new RealTimeCost();
        datalakeRealTimeCost.setEnvCrn("ENVIRONMENT_CRN");
        datalakeRealTimeCost.setType("DATALAKE");
        datalakeRealTimeCost.setResourceName("DATALAKE_NAME");
        RealTimeCost datahubRealTimeCost = new RealTimeCost();
        datahubRealTimeCost.setEnvCrn("ENVIRONMENT_CRN");
        datahubRealTimeCost.setType("WORKLOAD");
        return new RealTimeCostResponse(Map.of("DATALAKE_CRN", datalakeRealTimeCost, "DATAHUB_CRN", datahubRealTimeCost));
    }

    private RealTimeCostResponse getRealTimeCostResponseForFreeipa() {
        RealTimeCost freeipaRealTimeCost = new RealTimeCost();
        freeipaRealTimeCost.setEnvCrn("ENVIRONMENT_CRN");
        freeipaRealTimeCost.setType("FREEIPA");
        freeipaRealTimeCost.setResourceName("FREEIPA_NAME");
        return new RealTimeCostResponse(Map.of("FREEIPA_CRN", freeipaRealTimeCost));
    }
}
