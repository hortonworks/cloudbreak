package com.sequenceiq.datalake.service.sdx;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.ClusterCostV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.cloudbreak.cost.CostCalculationNotEnabledException;

@Service
public class SdxCostService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCostService.class);

    @Inject
    private ClusterCostV4Endpoint clusterCostV4Endpoint;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public RealTimeCostResponse getCosts(List<String> sdxCrns) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (entitlementService.isUsdCostCalculationEnabled(accountId)) {
            String internalCrn = regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString();
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            return ThreadBasedUserCrnProvider.doAsInternalActor(internalCrn, () -> clusterCostV4Endpoint.list(sdxCrns, initiatorUserCrn));
        } else {
            throw new CostCalculationNotEnabledException("Cost calculation features are not enabled!");
        }
    }
}
