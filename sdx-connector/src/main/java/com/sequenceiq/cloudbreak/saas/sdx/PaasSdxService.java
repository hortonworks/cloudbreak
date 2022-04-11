package com.sequenceiq.cloudbreak.saas.sdx;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_FAILED;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.saas.sdx.polling.PollingResult;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class PaasSdxService extends AbstractSdxService<SdxClusterStatusResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaasSdxService.class);

    @Inject
    private SdxEndpoint sdxEndpoint;

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
    public Set<String> listSdxCrns(String environmentName, String environmentCrn) {
        return sdxEndpoint.list(environmentName, true).stream()
                .filter(sdx -> StringUtils.equals(sdx.getEnvironmentCrn(), environmentCrn))
                .map(SdxClusterResponse::getCrn)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Pair<String, SdxClusterStatusResponse>> listSdxCrnStatusPair(String environmentCrn, String environmentName, Set<String> sdxCrns) {
        return sdxEndpoint.list(environmentName, true).stream()
                .filter(sdxResponse -> sdxCrns.contains(sdxResponse.getCrn()))
                .map(sdxResponse -> Pair.of(sdxResponse.getCrn(), sdxResponse.getStatus()))
                .collect(Collectors.toSet());
    }

    @Override
    public PollingResult getDeletePollingResultByStatus(SdxClusterStatusResponse status) {
        return DELETE_FAILED.equals(status) ? PollingResult.FAILED : PollingResult.IN_PROGRESS;
    }
}
