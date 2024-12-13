package com.sequenceiq.cloudbreak.sdx.paas.service;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class PaasSdxStatusService extends AbstractPaasSdxService implements PlatformAwareSdxStatusService<SdxClusterStatusResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaasSdxStatusService.class);

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Override
    public Set<Pair<String, SdxClusterStatusResponse>> listSdxCrnStatusPair(String environmentCrn) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> sdxEndpoint.getByEnvCrn(environmentCrn, false))
                .stream()
                .map(sdxResponse -> Pair.of(sdxResponse.getCrn(), sdxResponse.getStatus()))
                .collect(Collectors.toSet());
    }

    @Override
    public StatusCheckResult getAvailabilityStatusCheckResult(SdxClusterStatusResponse status) {
        if (status.isAvailable()) {
            return StatusCheckResult.AVAILABLE;
        } else if (status.isRollingUpgradeInProgress()) {
            return StatusCheckResult.ROLLING_UPGRADE_IN_PROGRESS;
        } else {
            return StatusCheckResult.NOT_AVAILABLE;
        }
    }

}
