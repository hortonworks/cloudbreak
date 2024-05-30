package com.sequenceiq.cloudbreak.sdx.paas;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_FAILED;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.AbstractSdxService;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.polling.PollingResult;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class PaasSdxService extends AbstractSdxService<SdxClusterStatusResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaasSdxService.class);

    @Inject
    private SdxEndpoint sdxEndpoint;

    @Inject
    private Optional<PaasRemoteDataContextSupplier> remoteDataContextSupplier;

    @Inject
    private Optional<LocalPaasSdxService> localPaasSdxService;

    @Override
    public Optional<String> getRemoteDataContext(String crn) {
        return remoteDataContextSupplier.map(rdxSupplier -> rdxSupplier.getPaasSdxRemoteDataContext(crn))
                .orElseThrow(() -> new CloudbreakServiceException("Cannot provide remote data context!"));
    }

    @Override
    public TargetPlatform targetPlatform() {
        return TargetPlatform.PAAS;
    }

    @Override
    public void deleteSdx(String sdxCrn, Boolean force) {
        LOGGER.info("Calling delete for SDX PaaS cluster {}", sdxCrn);
        sdxEndpoint.deleteByCrn(sdxCrn, force);
    }

    @Override
    public Set<String> listSdxCrns(String environmentCrn) {
        return sdxEndpoint.getByEnvCrn(environmentCrn).stream()
                .map(SdxClusterResponse::getCrn)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<SdxBasicView> getSdxByEnvironmentCrn(String environmentCrn) {
        if (localPaasSdxService.isPresent()) {
            return localPaasSdxService.get().getSdxBasicView(environmentCrn);
        } else {
            return sdxEndpoint.getByEnvCrn(environmentCrn).stream()
                    .map(sdx -> new SdxBasicView(
                            sdx.getName(),
                            sdx.getCrn(),
                            sdx.getRuntime(),
                            sdx.getEnvironmentCrn(),
                            sdx.getRangerRazEnabled(),
                            sdx.getCreated(),
                            sdx.getDatabaseServerCrn())
                    )
                    .findFirst();
        }
    }

    @Override
    public Set<Pair<String, SdxClusterStatusResponse>> listSdxCrnStatusPair(String environmentCrn, Set<String> sdxCrns) {
        return sdxEndpoint.getByEnvCrn(environmentCrn).stream()
                .filter(sdxResponse -> sdxCrns.contains(sdxResponse.getCrn()))
                .map(sdxResponse -> Pair.of(sdxResponse.getCrn(), sdxResponse.getStatus()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Pair<String, StatusCheckResult>> listSdxCrnStatusCheckPair(String environmentCrn, Set<String> sdxCrns) {
        return listSdxCrnStatusPair(environmentCrn, sdxCrns).stream()
                .map(statusPair -> Pair.of(statusPair.getKey(), getAvailabilityStatusCheckResult(statusPair.getValue())))
                .collect(Collectors.toSet());
    }

    @Override
    public PollingResult getDeletePollingResultByStatus(SdxClusterStatusResponse status) {
        return DELETE_FAILED.equals(status) ? PollingResult.FAILED : PollingResult.IN_PROGRESS;
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

    @Override
    public boolean isSdxClusterHA(String environmentCrn) {
        return sdxEndpoint.getByCrn(environmentCrn).getClusterShape().isHA();
    }

}
