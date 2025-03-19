package com.sequenceiq.environment.environment.service.cost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.co2.ClusterCO2V4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.co2.requests.ClusterCO2V4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.ClusterCostV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.requests.ClusterCostV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.co2.EnvironmentRealTimeCO2;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.cloudbreak.common.cost.EnvironmentRealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCost;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.cost.CostCalculationNotEnabledException;
import com.sequenceiq.freeipa.api.v1.co2.FreeIpaCO2V1Endpoint;
import com.sequenceiq.freeipa.api.v1.cost.FreeIpaCostV1Endpoint;

@Service
public class EnvironmentCostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCostService.class);

    @Inject
    private ClusterCostV4Endpoint clusterCostV4Endpoint;

    @Inject
    private ClusterCO2V4Endpoint clusterCO2V4Endpoint;

    @Inject
    private FreeIpaCostV1Endpoint freeIpaCostV1Endpoint;

    @Inject
    private FreeIpaCO2V1Endpoint freeIpaCO2V1Endpoint;

    @Inject
    private EntitlementService entitlementService;

    public Map<String, EnvironmentRealTimeCost> getCosts(List<String> environmentCrns, List<String> clusterCrns) {
        errorIfCostCalculationFeatureIsNotEnabled();

        Map<String, EnvironmentRealTimeCost> environmentCost = new HashMap<>();
        Map<String, RealTimeCost> totalCosts = new HashMap<>();
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();

        if (CollectionUtils.isNotEmpty(environmentCrns)) {
            RealTimeCostResponse freeipaCostResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> freeIpaCostV1Endpoint.list(environmentCrns, initiatorUserCrn));
            totalCosts.putAll(freeipaCostResponse.getCost());
        }
        ClusterCostV4Request request = new ClusterCostV4Request();
        request.setEnvironmentCrns(environmentCrns);
        request.setClusterCrns(clusterCrns);
        RealTimeCostResponse clusterCostResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> clusterCostV4Endpoint.listByEnv(request, initiatorUserCrn));
        totalCosts.putAll(clusterCostResponse.getCost());

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

    public Map<String, EnvironmentRealTimeCO2> getCO2(List<String> environmentCrns, List<String> clusterCrns) {
        errorIfCO2CalculationFeatureIsNotEnabled();
        Map<String, EnvironmentRealTimeCO2> environmentRealTimeCO2Map = new HashMap<>();
        Map<String, RealTimeCO2> totalCO2 = new HashMap<>();
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();

        if (CollectionUtils.isNotEmpty(environmentCrns)) {
            RealTimeCO2Response freeipaCO2Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> freeIpaCO2V1Endpoint.list(environmentCrns, initiatorUserCrn));
            totalCO2.putAll(freeipaCO2Response.getCo2());
        }
        ClusterCO2V4Request clusterCO2V4Request = new ClusterCO2V4Request();
        clusterCO2V4Request.setEnvironmentCrns(environmentCrns);
        clusterCO2V4Request.setClusterCrns(clusterCrns);
        RealTimeCO2Response clusterCO2Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> clusterCO2V4Endpoint.listByEnv(clusterCO2V4Request, initiatorUserCrn));
        totalCO2.putAll(clusterCO2Response.getCo2());

        LOGGER.debug("Total CO2: {}", totalCO2);
        for (Map.Entry<String, RealTimeCO2> co2Entry : totalCO2.entrySet()) {
            String key = co2Entry.getKey();
            RealTimeCO2 realTimeCO2 = co2Entry.getValue();
            String envCrn = realTimeCO2.getEnvCrn();

            EnvironmentRealTimeCO2 co2 = environmentRealTimeCO2Map.getOrDefault(envCrn, new EnvironmentRealTimeCO2());
            co2.addCO2ByType(key, realTimeCO2);
            environmentRealTimeCO2Map.put(envCrn, co2);
        }
        LOGGER.debug("CO2 summed for environments: {}", environmentRealTimeCO2Map);
        return environmentRealTimeCO2Map;
    }

    private void errorIfCostCalculationFeatureIsNotEnabled() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (!entitlementService.isUsdCostCalculationEnabled(accountId)) {
            throw new CostCalculationNotEnabledException("Cost calculation features are not enabled!");
        }
    }

    private void errorIfCO2CalculationFeatureIsNotEnabled() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (!entitlementService.isCO2CalculationEnabled(accountId)) {
            throw new CostCalculationNotEnabledException("CO2 calculation feature is not enabled!");
        }
    }
}
