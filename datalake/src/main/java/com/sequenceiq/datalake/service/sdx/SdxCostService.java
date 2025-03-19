package com.sequenceiq.datalake.service.sdx;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.co2.ClusterCO2V4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.ClusterCostV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.co2.CO2CalculationNotEnabledException;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.cost.CostCalculationNotEnabledException;

@Service
public class SdxCostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCostService.class);

    @Inject
    private ClusterCostV4Endpoint clusterCostV4Endpoint;

    @Inject
    private ClusterCO2V4Endpoint clusterCO2V4Endpoint;

    @Inject
    private EntitlementService entitlementService;

    public Map<String, RealTimeCost> getCosts(List<String> sdxCrns) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (entitlementService.isUsdCostCalculationEnabled(accountId)) {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> clusterCostV4Endpoint.list(sdxCrns, initiatorUserCrn).getCost());
        }
        throw new CostCalculationNotEnabledException("Cost calculation features are not enabled!");
    }

    public Map<String, RealTimeCO2> getCO2(List<String> sdxCrns) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (entitlementService.isCO2CalculationEnabled(accountId)) {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> clusterCO2V4Endpoint.list(sdxCrns, initiatorUserCrn).getCo2());
        }
        throw new CO2CalculationNotEnabledException("CO2 cost calculation feature is not enabled!");
    }
}
