package com.sequenceiq.environment.environment.service.cost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.ClusterCostV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.cost.EnvironmentRealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.cost.CostCalculationNotEnabledException;
import com.sequenceiq.freeipa.api.v1.cost.FreeIpaCostV1Endpoint;

@Service
public class EnvironmentCostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCostService.class);

    @Inject
    private ClusterCostV4Endpoint clusterCostV4Endpoint;

    @Inject
    private FreeIpaCostV1Endpoint freeIpaCostV1Endpoint;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private boolean usdCalculationEnabled;

    public Map<String, EnvironmentRealTimeCost> getCosts(List<String> environmentCrns, List<String> clusterCrns) {
        checkIfCostCalculationIsEnabled();
        errorIfCostCalculationFeatureIsNotEnabled();

        Map<String, EnvironmentRealTimeCost> environmentCost = new HashMap<>();
        Map<String, RealTimeCost> totalCosts = new HashMap<>();
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();

        String internalCrn = regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString();
        if (CollectionUtils.isNotEmpty(environmentCrns)) {
            RealTimeCostResponse freeipaCostResponse = ThreadBasedUserCrnProvider.doAsInternalActor(internalCrn,
                    () -> freeIpaCostV1Endpoint.list(environmentCrns, initiatorUserCrn));
            totalCosts.putAll(freeipaCostResponse.getCost());
        }
        if (CollectionUtils.isNotEmpty(clusterCrns)) {
            RealTimeCostResponse clusterCostResponse = ThreadBasedUserCrnProvider.doAsInternalActor(internalCrn,
                    () -> clusterCostV4Endpoint.list(clusterCrns, initiatorUserCrn));
            totalCosts.putAll(clusterCostResponse.getCost());
        }

        LOGGER.debug("Total Costs: {}", totalCosts);
        for (Map.Entry<String, RealTimeCost> costEntry : totalCosts.entrySet()) {
            String key = costEntry.getKey();
            RealTimeCost realTimeCost = costEntry.getValue();
            String envCrn = costEntry.getValue().getEnvCrn();

            EnvironmentRealTimeCost cost = environmentCost.getOrDefault(envCrn, new EnvironmentRealTimeCost());
            cost.addCostByType(key, realTimeCost);
            environmentCost.put(envCrn, cost);
        }
        LOGGER.debug("Cost summed for environments: {}", environmentCost);
        return environmentCost;
    }

    private void checkIfCostCalculationIsEnabled() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        usdCalculationEnabled = entitlementService.isUsdCostCalculationEnabled(accountId);

        if (!usdCalculationEnabled) {
            LOGGER.info("USD cost calculation feature is disabled!");
        }
    }

    private void errorIfCostCalculationFeatureIsNotEnabled() {
        if (!usdCalculationEnabled) {
            throw new CostCalculationNotEnabledException("Cost calculation features are not enabled!");
        }
    }
}
