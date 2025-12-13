package com.sequenceiq.environment.environment.service.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
import com.sequenceiq.cloudbreak.common.co2.EnvironmentRealTimeCO2;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.common.cost.EnvironmentRealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.freeipa.api.v1.co2.FreeIpaCO2V1Endpoint;
import com.sequenceiq.freeipa.api.v1.cost.FreeIpaCostV1Endpoint;

@ExtendWith(MockitoExtension.class)
public class EnvironmentCostServiceTest {

    @Mock
    private ClusterCostV4Endpoint clusterCostV4Endpoint;

    @Mock
    private ClusterCO2V4Endpoint clusterCO2V4Endpoint;

    @Mock
    private FreeIpaCostV1Endpoint freeIpaCostV1Endpoint;

    @Mock
    private FreeIpaCO2V1Endpoint freeIpaCO2V1Endpoint;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private EnvironmentCostService underTest;

    @Test
    void getCosts() {
        when(entitlementService.isUsdCostCalculationEnabled(any())).thenReturn(Boolean.TRUE);
        when(clusterCostV4Endpoint.listByEnv(any(), any())).thenReturn(getRealTimeCostResponseForClusters());
        when(freeIpaCostV1Endpoint.list(any(), any())).thenReturn(getRealTimeCostResponseForFreeipa());

        ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> {
            Map<String, EnvironmentRealTimeCost> environmentCosts = underTest.getCosts(List.of("ENVIRONMENT_CRN"), List.of("DATALAKE_CRN", "DATAHUB_CRN"));

            assertEquals(1, environmentCosts.entrySet().size());
            EnvironmentRealTimeCost environmentRealTimeCost = environmentCosts.get("ENVIRONMENT_CRN");
            assertEquals("FREEIPA_NAME", environmentRealTimeCost.getFreeipa().getResourceName());
            assertEquals("DATALAKE_NAME", environmentRealTimeCost.getDatalake().getResourceName());
            assertEquals(1, environmentRealTimeCost.getDatahubs().size());
            RealTimeCost datahubRealTimeCost = environmentRealTimeCost.getDatahubs().get("DATAHUB_CRN");
            assertNotNull(datahubRealTimeCost);
        });
    }

    @Test
    void getCO2() {
        when(entitlementService.isCO2CalculationEnabled(any())).thenReturn(Boolean.TRUE);
        when(clusterCO2V4Endpoint.listByEnv(any(), any())).thenReturn(getRealTimeCO2ResponseForClusters());
        when(freeIpaCO2V1Endpoint.list(any(), any())).thenReturn(getRealTimeCO2ResponseForFreeipa());

        ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> {
            Map<String, EnvironmentRealTimeCO2> environmentCO2s = underTest.getCO2(List.of("ENVIRONMENT_CRN"), List.of("DATALAKE_CRN", "DATAHUB_CRN"));

            assertEquals(1, environmentCO2s.entrySet().size());
            EnvironmentRealTimeCO2 environmentRealTimeCO2 = environmentCO2s.get("ENVIRONMENT_CRN");
            assertEquals("FREEIPA_NAME", environmentRealTimeCO2.getFreeipa().getResourceName());
            assertEquals("DATALAKE_NAME", environmentRealTimeCO2.getDatalake().getResourceName());
            assertEquals(1, environmentRealTimeCO2.getDatahubs().size());
            RealTimeCO2 datahubRealTimeCO2 = environmentRealTimeCO2.getDatahubs().get("DATAHUB_CRN");
            assertNotNull(datahubRealTimeCO2);
        });

    }

    private RealTimeCostResponse getRealTimeCostResponseForClusters() {
        RealTimeCost datalakeRealTimeCost = new RealTimeCost();
        datalakeRealTimeCost.setEnvCrn("ENVIRONMENT_CRN");
        datalakeRealTimeCost.setResourceCrn("CRN");
        datalakeRealTimeCost.setType("DATALAKE");
        datalakeRealTimeCost.setResourceName("DATALAKE_NAME");
        RealTimeCost datahubRealTimeCost = new RealTimeCost();
        datahubRealTimeCost.setEnvCrn("ENVIRONMENT_CRN");
        datahubRealTimeCost.setResourceCrn("CRN");
        datahubRealTimeCost.setType("WORKLOAD");
        return new RealTimeCostResponse(Map.of("DATALAKE_CRN", datalakeRealTimeCost, "DATAHUB_CRN", datahubRealTimeCost));
    }

    private RealTimeCO2Response getRealTimeCO2ResponseForClusters() {
        RealTimeCO2 datalakeRealTimeCO2 = new RealTimeCO2();
        datalakeRealTimeCO2.setEnvCrn("ENVIRONMENT_CRN");
        datalakeRealTimeCO2.setResourceCrn("CRN");
        datalakeRealTimeCO2.setType("DATALAKE");
        datalakeRealTimeCO2.setResourceName("DATALAKE_NAME");
        RealTimeCO2 datahubRealTimeCO2 = new RealTimeCO2();
        datahubRealTimeCO2.setEnvCrn("ENVIRONMENT_CRN");
        datahubRealTimeCO2.setResourceCrn("CRN");
        datahubRealTimeCO2.setType("WORKLOAD");
        return new RealTimeCO2Response(Map.of("DATALAKE_CRN", datalakeRealTimeCO2, "DATAHUB_CRN", datahubRealTimeCO2));
    }

    private RealTimeCostResponse getRealTimeCostResponseForFreeipa() {
        RealTimeCost freeipaRealTimeCost = new RealTimeCost();
        freeipaRealTimeCost.setEnvCrn("ENVIRONMENT_CRN");
        freeipaRealTimeCost.setType("FREEIPA");
        freeipaRealTimeCost.setResourceName("FREEIPA_NAME");
        return new RealTimeCostResponse(Map.of("FREEIPA_CRN", freeipaRealTimeCost));
    }

    private RealTimeCO2Response getRealTimeCO2ResponseForFreeipa() {
        RealTimeCO2 freeipaRealTimeCO2 = new RealTimeCO2();
        freeipaRealTimeCO2.setEnvCrn("ENVIRONMENT_CRN");
        freeipaRealTimeCO2.setType("FREEIPA");
        freeipaRealTimeCO2.setResourceName("FREEIPA_NAME");
        return new RealTimeCO2Response(Map.of("FREEIPA_CRN", freeipaRealTimeCO2));
    }
}
